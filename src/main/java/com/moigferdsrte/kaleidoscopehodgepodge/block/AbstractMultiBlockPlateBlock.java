package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

abstract class AbstractMultiBlockPlateBlock extends AbstractHodgepodgeFeastBlock {
    protected AbstractMultiBlockPlateBlock(Properties properties) {
        super(properties, CustomFeastData.ContainerKind.DISH);
    }

    protected abstract List<StructurePart> structure(BlockPos pos, BlockState state);

    protected final boolean canReplace(BlockPlaceContext context, BlockPos pos) {
        return context.getLevel().getBlockState(pos).canBeReplaced(context)
                && context.getLevel().getWorldBorder().isWithinBounds(pos);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        List<StructurePart> parts = structure(pos, state);
        for (StructurePart part : parts) {
            if (!part.pos().equals(pos)) {
                boolean waterlogged = level.getFluidState(part.pos()).getType() == Fluids.WATER;
                level.setBlockAndUpdate(part.pos(),
                        part.state().setValue(BlockStateProperties.WATERLOGGED, waterlogged));
            }
        }
        CustomFeastData data = stack.get(KHDataComponents.CUSTOM_FEAST.get());
        if (data == null || data.kind() != CustomFeastData.ContainerKind.DISH) return;
        distributeIngredients(level, parts, data.ingredients());
    }

    @Override
    protected List<HodgepodgeFeastBlockEntity> feastEntities(Level level, BlockPos pos, BlockState state) {
        return validParts(level, pos, state).stream()
                .map(part -> level.getBlockEntity(part.pos()))
                .filter(HodgepodgeFeastBlockEntity.class::isInstance)
                .map(HodgepodgeFeastBlockEntity.class::cast)
                .toList();
    }

    @Override
    protected List<IngredientReference> ingredientReferences(Level level, BlockPos pos, BlockState state) {
        List<StructurePart> parts = validParts(level, pos, state);
        StructurePart target = parts.stream().filter(part -> part.pos().equals(pos)).findFirst().orElse(null);
        if (target == null) return List.of();
        List<IngredientReference> references = new ArrayList<>();
        for (StructurePart source : parts) {
            if (!(level.getBlockEntity(source.pos()) instanceof HodgepodgeFeastBlockEntity feast)) continue;
            List<PlacedIngredient> ingredients = feast.renderIngredients();
            for (int index = 0; index < ingredients.size(); index++) {
                PlacedIngredient canonical = toItemCoordinates(source, ingredients.get(index));
                PlacedIngredient local = fromItemCoordinates(target, canonical);
                references.add(new IngredientReference(feast, index, local));
            }
        }
        return references;
    }

    @Override
    protected VoxelShape ingredientShape(BlockGetter level, BlockPos pos, BlockState state) {
        VoxelShape combined = Shapes.empty();
        for (StructurePart part : validParts(level, pos, state)) {
            if (level.getBlockEntity(part.pos()) instanceof HodgepodgeFeastBlockEntity feast) {
                combined = Shapes.or(combined, feast.ingredientShape().move(
                        part.pos().getX() - pos.getX(), 0, part.pos().getZ() - pos.getZ()));
            }
        }
        return combined.optimize();
    }

    @Override
    protected void removeContainerAfterEating(Level level, BlockPos pos, BlockState state, Player player) {
        for (StructurePart part : validParts(level, pos, state)) {
            removeStructurePart(level, part.pos(), false);
        }
        ItemStack container = new ItemStack(this);
        if (!player.addItem(container)) player.drop(container, false);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos,
                                                 @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide()) {
            List<StructurePart> parts = validParts(level, pos, state);
            ItemStack drop = createStructureDrop(level, parts);
            if (!player.isCreative() || drop.has(KHDataComponents.CUSTOM_FEAST.get())) {
                popResource(level, pos, drop);
            }
            for (StructurePart part : parts) {
                if (!part.pos().equals(pos)) {
                    removeStructurePart(level, part.pos(), true);
                    level.levelEvent(player, 2001, part.pos(), Block.getId(part.state()));
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean managesStructureDrops() {
        return true;
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        Entity breaker = builder.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (breaker instanceof Player) return List.of();
        Vec3 origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin == null) return List.of();
        BlockPos pos = BlockPos.containing(origin);
        BlockState actual = builder.getLevel().getBlockState(pos);
        if (!actual.is(this)) return List.of();
        return List.of(createStructureDrop(builder.getLevel(), validParts(builder.getLevel(), pos, actual)));
    }

    @Override
    protected void onExplosionHit(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                  @NotNull Explosion explosion,
                                  @NotNull BiConsumer<ItemStack, BlockPos> dropConsumer) {
        if (explosion.getBlockInteraction() == Explosion.BlockInteraction.TRIGGER_BLOCK) {
            super.onExplosionHit(state, level, pos, explosion, dropConsumer);
            return;
        }
        List<StructurePart> parts = validParts(level, pos, state);
        if (dropFromExplosion(explosion)) {
            dropConsumer.accept(createStructureDrop(level, parts), pos);
        }
        for (StructurePart part : parts) {
            removeStructurePart(level, part.pos(), true);
            part.state().getBlock().wasExploded(level, part.pos(), explosion);
        }
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                   @NotNull Block neighborBlock, @NotNull BlockPos neighborPos,
                                   boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos,
                        @NotNull RandomSource random) {
        List<StructurePart> expected = structure(pos, state);
        List<StructurePart> present = validParts(level, pos, state);
        if (present.size() == expected.size()) return;
        for (StructurePart part : present) {
            removeStructurePart(level, part.pos(), true);
        }
    }

    private static void removeStructurePart(Level level, BlockPos pos, boolean suppressDrops) {
        BlockState actual = level.getBlockState(pos);
        BlockState replacement = actual.hasProperty(BlockStateProperties.WATERLOGGED)
                && actual.getValue(BlockStateProperties.WATERLOGGED)
                ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        int flags = Block.UPDATE_ALL | (suppressDrops ? Block.UPDATE_SUPPRESS_DROPS : 0);
        level.setBlock(pos, replacement, flags);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos,
                                                @NotNull BlockState state) {
        return createStructureDrop(level, validParts(level, pos, state));
    }

    protected final List<StructurePart> validParts(BlockGetter level, BlockPos pos, BlockState state) {
        return structure(pos, state).stream()
                .filter(part -> matchesStructureState(level.getBlockState(part.pos()), part.state()))
                .toList();
    }

    private static boolean matchesStructureState(BlockState actual, BlockState expected) {
        if (!actual.is(expected.getBlock())) return false;
        return actual.setValue(BlockStateProperties.WATERLOGGED, false)
                .equals(expected.setValue(BlockStateProperties.WATERLOGGED, false));
    }

    private ItemStack createStructureDrop(BlockGetter level, List<StructurePart> parts) {
        ItemStack stack = new ItemStack(this);
        List<PlacedIngredient> ingredients = new ArrayList<>();
        for (StructurePart part : parts) {
            BlockEntity entity = level.getBlockEntity(part.pos());
            if (!(entity instanceof HodgepodgeFeastBlockEntity feast)) continue;
            feast.ingredients().stream()
                    .map(ingredient -> toItemCoordinates(part, ingredient))
                    .forEach(ingredients::add);
        }
        if (!ingredients.isEmpty()) {
            stack.set(KHDataComponents.CUSTOM_FEAST.get(),
                    new CustomFeastData(CustomFeastData.ContainerKind.DISH, Direction.NORTH, ingredients));
        }
        return stack;
    }

    private void distributeIngredients(Level level, List<StructurePart> parts,
                                       List<PlacedIngredient> ingredients) {
        Map<BlockPos, List<PlacedIngredient>> byPart = new HashMap<>();
        parts.forEach(part -> byPart.put(part.pos(), new ArrayList<>()));
        for (PlacedIngredient ingredient : ingredients) {
            for (StructurePart part : parts) {
                PlacedIngredient local = toLocalCoordinates(part, ingredient);
                if (local != null) {
                    byPart.get(part.pos()).add(local);
                    break;
                }
            }
        }
        for (StructurePart part : parts) {
            if (level.getBlockEntity(part.pos()) instanceof HodgepodgeFeastBlockEntity feast) {
                feast.setIngredients(byPart.get(part.pos()));
            }
        }
    }

    protected PlacedIngredient toItemCoordinates(StructurePart part, PlacedIngredient ingredient) {
        return ingredient.translated(part.pixelOffsetX(), part.pixelOffsetZ());
    }

    protected PlacedIngredient fromItemCoordinates(StructurePart part, PlacedIngredient ingredient) {
        return ingredient.translated(-part.pixelOffsetX(), -part.pixelOffsetZ());
    }

    protected @Nullable PlacedIngredient toLocalCoordinates(StructurePart part, PlacedIngredient ingredient) {
        int localX = ingredient.x() - part.pixelOffsetX();
        int localZ = ingredient.z() - part.pixelOffsetZ();
        return localX >= 0 && localX < 16 && localZ >= 0 && localZ < 16
                ? fromItemCoordinates(part, ingredient) : null;
    }

    protected record StructurePart(BlockPos pos, BlockState state, int pixelOffsetX, int pixelOffsetZ) {}
}
