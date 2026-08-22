package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBlock;
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
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientConsumptionSelector;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;

abstract class AbstractHodgepodgeFeastBlock extends FoodBlock implements EntityBlock, IHodgepodge {
    private final CustomFeastData.ContainerKind kind;

    protected AbstractHodgepodgeFeastBlock(Properties properties, CustomFeastData.ContainerKind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public @NonNull InteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state,
                                                @NonNull Level level, @NonNull BlockPos pos,
                                                @NonNull Player player, @NonNull InteractionHand hand,
                                                @NonNull BlockHitResult hit) {
        PackingBagContents contents = PackingBagService.get(stack);
        if (!stack.is(KHItems.WRAPPING_BAG)) {
            return stack.isEmpty() && hand == InteractionHand.MAIN_HAND
                    ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) {
            return InteractionResult.FAIL;
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
                player.getEyePosition(), hit, ingredient, baggedIngredient.rotation(),
                allowsBoundaryPlacementProjection()).orElse(null);
        if (target == null) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
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
        level.playSound(null, pos, SoundEvents.CAKE_ADD_CANDLE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (GeneralConfig.snapshot().debugLogging()) {
            KaleidoscopeHodgepodge.LOGGER.info("Placed ingredient {} at {} pixel {},{}",
                    ingredient.getId(), pos, target.x(), target.z());
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult retrieveIngredient(ItemStack bag, PackingBagContents contents, Level level,
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
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        PlacedIngredient removed = reference.owner().removeIngredient(reference.index()).orElse(null);
        if (removed == null) return InteractionResult.FAIL;
        PackingBagService.replaceHeldBag(bag, player, updated);
        CrashDiagnostics.record("retrieved " + removed.id() + " from " + pos);
        if (GeneralConfig.snapshot().debugLogging()) {
            KaleidoscopeHodgepodge.LOGGER.info("Retrieved ingredient {} from {}", removed.id(), pos);
        }
        level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level level,
                                                     @NonNull BlockPos pos, @NonNull Player player,
                                                     @NonNull BlockHitResult hit) {
        List<IngredientReference> references = ingredientReferences(level, pos, state);
        if (references.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        int selected = IngredientConsumptionSelector.highest(
                references.stream().map(IngredientReference::ingredient).toList(), level.getRandom())
                .orElse(-1);
        if (selected < 0) return InteractionResult.FAIL;
        IngredientReference reference = references.get(selected);
        PlacedIngredient eaten = reference.owner().removeIngredient(reference.index()).orElse(null);
        if (eaten == null) return InteractionResult.FAIL;
        IngredientFoodService.applyAll(level, player,
                List.of(IngredientFoodService.resolveForConsumption(eaten.id(), eaten.food())));
        level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS,
                0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(player, GameEvent.EAT, pos);

        if (feastEntities(level, pos, state).stream()
                .allMatch(feast -> feast.renderIngredients().isEmpty())) {
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
    public @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level,
                                        @NonNull BlockPos pos, @NonNull CollisionContext context) {
        VoxelShape container = getContainerShape(state, level, pos, context);
        return Shapes.or(container, ingredientShape(level, pos, state));
    }

    protected VoxelShape getContainerShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape containerOutlineShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return getContainerShape(state, level, pos, context);
    }

    @Override
    protected boolean triggerEvent(@NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, int id, int data) {
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

    private static InteractionResult reject(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(key)));
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void setPlacedBy(@NonNull Level level, @NonNull BlockPos pos, @NonNull BlockState state,
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
    public @NonNull List<ItemStack> getDrops(@NonNull BlockState state, LootParams.@NonNull Builder builder) {
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
    public @NonNull BlockState playerWillDestroy(@NonNull Level level, @NonNull BlockPos pos, @NonNull BlockState state,
                                                 @NonNull Player player) {
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
    protected @NonNull ItemStack getCloneItemStack(@NonNull LevelReader level, @NonNull BlockPos pos,
                                                   @NonNull BlockState state, boolean includeData) {
        BlockEntity entity = level.getBlockEntity(pos);
        return createDrop(state, entity instanceof HodgepodgeFeastBlockEntity feast ? feast : null);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new HodgepodgeFeastBlockEntity(pos, state);
    }
}
