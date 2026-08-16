package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.advancements.Types;
import com.moigferdsrte.kaleidoscopehodgepodge.api.IHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientHitTest;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientPlacementTarget;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodService;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.util.CrashDiagnostics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;

abstract class AbstractHodgepodgeFeastBlock extends Block implements EntityBlock, SimpleWaterloggedBlock, IHodgepodge {
    protected static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape CONTAINER_SHAPE = Block.box(1, 0, 1, 15, 2, 15);
    private final CustomFeastData.ContainerKind kind;

    protected AbstractHodgepodgeFeastBlock(Properties properties, CustomFeastData.ContainerKind kind) {
        super(configureProperties(properties));
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    private static Properties configureProperties(Properties properties) {
        return properties.forceSolidOn()
                .mapColor(MapColor.WOOD)
                .pushReaction(PushReaction.DESTROY)
                .noOcclusion();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    protected @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state,
                                                @NotNull Level level, @NotNull BlockPos pos,
                                                @NotNull Player player, @NotNull InteractionHand hand,
                                                @NotNull BlockHitResult hit) {
        PackingBagContents contents = PackingBagService.get(stack);
        if (!stack.is(KHItems.WRAPPING_BAG)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isSecondaryUseActive()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) {
            return ItemInteractionResult.FAIL;
        }
        if (PackingBagService.getMode(stack) == PackingBagMode.STORAGE) {
            if (contents.isFull()) return reject(player, "tooltip.kaleidoscope_hodgepodge.storage_full");
            return retrieveIngredient(stack, contents, level, pos, player, hit);
        }
        if (contents.isEmpty()) return reject(player, "tooltip.kaleidoscope_hodgepodge.placement_empty");
        BaggedIngredient baggedIngredient = contents.first().orElseThrow();
        PackingIngredients ingredient = PackingIngredientRegistry.byId(baggedIngredient.id()).orElse(null);
        if (ingredient == null) return reject(player, "tooltip.kaleidoscope_hodgepodge.unknown_ingredient");
        if (!isSuitable(ingredient)) {
            return reject(player, kind == CustomFeastData.ContainerKind.SOUP
                    ? "tooltip.kaleidoscope_hodgepodge.not_applicable_to_soup"
                    : "tooltip.kaleidoscope_hodgepodge.not_applicable_to_dish");
        }
        List<PlacedIngredient> existing = placementIngredients(level, pos, state);
        IngredientPlacementTarget.Pixel target = IngredientPlacementTarget.resolve(existing, pos,
                player.getEyePosition(), hit, ingredient, baggedIngredient.rotation()).orElse(null);
        if (target == null) return ItemInteractionResult.FAIL;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        IngredientFoodData food = IngredientFoodService.resolve(baggedIngredient.id(), baggedIngredient.food());
        PlacementSpace.Result result = feast.addAgainst(existing, ingredient, target.x(), target.z(),
                baggedIngredient.rotation(), food);
        if (!result.success()) {
            return reject(player, "tooltip.kaleidoscope_hodgepodge.placement_" + result.failure().name().toLowerCase());
        }
        ModTrigger.EVENT.trigger(player, Types.DIY_FEAST);
        PackingBagService.replaceHeldBag(stack, player, contents.withoutFirst());
        CrashDiagnostics.record("placed " + ingredient.getId() + " at " + pos
                + " pixel=" + target.x() + "," + target.z());
        if (GeneralConfig.snapshot().debugLogging()) {
            KaleidoscopeHodgepodge.LOGGER.info("Placed ingredient {} at {} pixel {},{}",
                    ingredient.getId(), pos, target.x(), target.z());
        }
        return ItemInteractionResult.SUCCESS;
    }

    private ItemInteractionResult retrieveIngredient(ItemStack bag, PackingBagContents contents, Level level,
                                                     BlockPos pos, Player player, BlockHitResult hit) {
        List<IngredientReference> references = ingredientReferences(level, pos,
                level.getBlockState(pos));
        List<PlacedIngredient> existing = references.stream().map(IngredientReference::ingredient).toList();
        Vec3 from = player.getEyePosition();
        Vec3 ray = hit.getLocation().subtract(from);
        Vec3 to = ray.lengthSqr() > 1.0E-7
                ? hit.getLocation().add(ray.normalize().scale(1.0 / 16.0))
                : hit.getLocation();
        OptionalInt selected = IngredientHitTest.nearest(existing, pos, from, to);
        if (selected.isEmpty()) return reject(player, "tooltip.kaleidoscope_hodgepodge.storage_no_target");
        IngredientReference reference = references.get(selected.getAsInt());
        PlacedIngredient selectedIngredient = reference.ingredient();
        PackingBagContents updated = contents.with(
                new BaggedIngredient(selectedIngredient.id(), selectedIngredient.rotation(),
                        IngredientFoodService.resolve(selectedIngredient.id(), selectedIngredient.food())))
                .orElse(null);
        if (updated == null) return reject(player, "tooltip.kaleidoscope_hodgepodge.storage_full");
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        PlacedIngredient removed = reference.owner().removeIngredient(reference.index()).orElse(null);
        if (removed == null) return ItemInteractionResult.FAIL;
        PackingBagService.replaceHeldBag(bag, player, updated);
        CrashDiagnostics.record("retrieved " + removed.id() + " from " + pos);
        if (GeneralConfig.snapshot().debugLogging()) {
            KaleidoscopeHodgepodge.LOGGER.info("Retrieved ingredient {} from {}", removed.id(), pos);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level,
                                                     @NotNull BlockPos pos, @NotNull Player player,
                                                     @NotNull BlockHitResult hit) {
        List<HodgepodgeFeastBlockEntity> feasts = feastEntities(level, pos, state);
        int ingredientCount = feasts.stream().mapToInt(feast -> feast.renderIngredients().size()).sum();
        if (ingredientCount == 0) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        int selected = level.getRandom().nextInt(ingredientCount);
        PlacedIngredient eaten = null;
        for (HodgepodgeFeastBlockEntity feast : feasts) {
            if (selected < feast.renderIngredients().size()) {
                eaten = feast.removeIngredient(selected).orElse(null);
                break;
            }
            selected -= feast.renderIngredients().size();
        }
        if (eaten == null) return InteractionResult.FAIL;
        IngredientFoodService.applyAll(level, player,
                List.of(IngredientFoodService.resolveForConsumption(eaten.id(), eaten.food())));
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS,
                0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(player, GameEvent.EAT, pos);

        if (feasts.stream().allMatch(feast -> feast.renderIngredients().isEmpty())) {
            level.levelEvent(null, 2001, pos, Block.getId(state));
            removeContainerAfterEating(level, pos, state, player);
        }
        CrashDiagnostics.record("ate ingredient " + eaten.id() + " from " + pos);
        return InteractionResult.SUCCESS;
    }

    protected List<HodgepodgeFeastBlockEntity> feastEntities(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast
                ? List.of(feast) : List.of();
    }

    @Override
    public List<PlacedIngredient> placementIngredients(Level level, BlockPos pos, BlockState state) {
        return ingredientReferences(level, pos, state).stream()
                .map(IngredientReference::ingredient)
                .toList();
    }

    protected List<IngredientReference> ingredientReferences(Level level, BlockPos pos, BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) return List.of();
        List<PlacedIngredient> ingredients = feast.renderIngredients();
        return java.util.stream.IntStream.range(0, ingredients.size())
                .mapToObj(index -> new IngredientReference(feast, index, ingredients.get(index)))
                .toList();
    }

    protected void removeContainerAfterEating(Level level, BlockPos pos, BlockState state, Player player) {
        level.removeBlock(pos, false);
        ItemStack container = new ItemStack(this);
        applyContainerStateToItem(container, state);
        if (!player.addItem(container)) player.drop(container, false);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        VoxelShape container = getContainerShape(state, level, pos, context);
        return Shapes.or(container, ingredientShape(level, pos, state));
    }

    protected VoxelShape getContainerShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return CONTAINER_SHAPE;
    }

    @Override
    public VoxelShape containerOutlineShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return getContainerShape(state, level, pos, context);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int data) {
        super.triggerEvent(state, level, pos, id, data);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, data);
    }

    protected VoxelShape ingredientShape(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast
                ? feast.ingredientShape() : Shapes.empty();
    }

    private boolean isSuitable(PackingIngredients ingredient) {
        return ingredient.suitableFor() == PackingIngredients.SuitableFor.BOTH
                || kind == CustomFeastData.ContainerKind.DISH && ingredient.suitableFor() == PackingIngredients.SuitableFor.DISH
                || kind == CustomFeastData.ContainerKind.SOUP && ingredient.suitableFor() == PackingIngredients.SuitableFor.SOUP;
    }

    private static ItemInteractionResult reject(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(key)));
        }
        return ItemInteractionResult.FAIL;
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        CustomFeastData data = stack.get(KHDataComponents.CUSTOM_FEAST);
        if (data != null && data.kind() == kind && level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast) {
            feast.setIngredients(data.ingredients());
        }
    }

    protected final ItemStack createDrop(BlockState state, @Nullable HodgepodgeFeastBlockEntity feast) {
        ItemStack stack = new ItemStack(this);
        applyContainerStateToItem(stack, state);
        if (feast != null && !feast.ingredients().isEmpty()) stack.set(KHDataComponents.CUSTOM_FEAST, feast.snapshot());
        return stack;
    }

    protected void applyContainerStateToItem(ItemStack stack, BlockState state) {
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        HodgepodgeFeastBlockEntity feast = entity instanceof HodgepodgeFeastBlockEntity value ? value : null;
        Entity breaker = builder.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (feast == null || feast.ingredients().isEmpty()) {
            // Creative destruction remains empty; survival and environmental drops return the container.
            if (breaker instanceof Player player && player.isCreative()) return List.of();
            return List.of(createDrop(state, null));
        }
        return List.of(createDrop(state, feast));
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                                                 @NotNull Player player) {
        if (!managesStructureDrops() && !level.isClientSide() && player.isCreative()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof HodgepodgeFeastBlockEntity feast && !feast.ingredients().isEmpty()) {
                popResource(level, pos, createDrop(state, feast));
                CrashDiagnostics.record("creative drop at " + pos);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    protected boolean managesStructureDrops() {
        return false;
    }

    protected record IngredientReference(HodgepodgeFeastBlockEntity owner, int index,
                                         PlacedIngredient ingredient) {}

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos,
                                                @NotNull BlockState state) {
        BlockEntity entity = level.getBlockEntity(pos);
        return createDrop(state, entity instanceof HodgepodgeFeastBlockEntity feast ? feast : null);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new HodgepodgeFeastBlockEntity(pos, state);
    }
}
