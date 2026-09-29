package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.advancements.Types;
import com.moigferdsrte.kaleidoscopehodgepodge.api.IHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.DishName;
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
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.util.CrashDiagnostics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.Items;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;

public abstract class AbstractHodgepodgeFeastBlock extends Block implements EntityBlock,
        net.minecraft.world.level.block.SimpleWaterloggedBlock, IHodgepodge {
    protected static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
    private static final VoxelShape CONTAINER_SHAPE = Block.box(1, 0, 1, 15, 2, 15);
    private final CustomFeastData.ContainerKind kind;

    protected AbstractHodgepodgeFeastBlock(Properties properties, CustomFeastData.ContainerKind kind) {
        super(properties.forceSolidOn().noOcclusion().dynamicShape()
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos())
                .getType() == net.minecraft.world.level.material.Fluids.WATER);
    }

    @Override
    protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? net.minecraft.world.level.material.Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        // Light workers must never traverse block entities or load neighboring structure chunks.
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATERLOGGED) ? 1 : 0;
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state,
                                                @NotNull Level level, @NotNull BlockPos pos,
                                                @NotNull Player player, @NotNull InteractionHand hand,
                                                @NotNull BlockHitResult hit) {
        if (player.isSpectator() || !player.mayBuild()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(Items.NAME_TAG)) {
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            if (name == null || name.getString().isBlank()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (feast.isRecipeLocked()) return reject(player, "tooltip.kaleidoscope_hodgepodge.recipe_locked");
            if (feast.recipeSnapshot().ingredients().isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide()) {
                feast.setDishName(java.util.Optional.of(name));
                if (!player.isCreative()) stack.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.is(KHItems.LUNCH_BOX.get())) {
            return useLunchBox(stack, state, level, pos, player, hit);
        }
        PackingBagContents contents = PackingBagService.get(stack);
        if (!stack.is(KHItems.WRAPPING_BAG.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isSecondaryUseActive()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) {
            return ItemInteractionResult.FAIL;
        }
        if (PackingBagService.getMode(stack) == PackingBagMode.STORAGE) {
            if (feast.isRecipeLocked()) return reject(player, "tooltip.kaleidoscope_hodgepodge.recipe_locked");
            if (contents.isFull()) return reject(player, "tooltip.kaleidoscope_hodgepodge.storage_full");
            return retrieveIngredient(stack, contents, level, pos, player, hit);
        }
        if (contents.isEmpty()) return reject(player, "tooltip.kaleidoscope_hodgepodge.placement_empty");
        if (feast.isRecipeLocked()) {
            PlacedIngredient expected = feast.nextRecipePlacement().orElse(null);
            if (expected == null) return ItemInteractionResult.FAIL;
            BlockPos targetPos = recipePlacementPos(pos, state, expected);
            if (!targetPos.equals(pos)) {
                return useItemOn(stack, level.getBlockState(targetPos), level, targetPos, player, hand, hit);
            }
        }
        BaggedIngredient baggedIngredient = contents.first().orElseThrow();
        PackingIngredients ingredient = PackingIngredientRegistry.byId(baggedIngredient.id()).orElse(null);
        if (ingredient == null) return reject(player, "tooltip.kaleidoscope_hodgepodge.unknown_ingredient");
        if (!isSuitable(ingredient)) {
            return reject(player, kind == CustomFeastData.ContainerKind.SOUP
                    ? "tooltip.kaleidoscope_hodgepodge.not_applicable_to_soup"
                    : "tooltip.kaleidoscope_hodgepodge.not_applicable_to_dish");
        }
        List<PlacedIngredient> existing = placementIngredients(level, pos, state);
        int placementRotation = baggedIngredient.rotation();
        IngredientPlacementTarget.Pixel target;
        if (feast.isRecipeLocked()) {
            // A locked recipe owns the complete placement target.  Do not derive a
            // position from the player's ray: this keeps the server authoritative
            // and makes the client preview match the actual placement.
            PlacedIngredient expected = feast.nextRecipePlacement()
                    .map(value -> ((IHodgepodge) this).recipePlacementTarget(pos, state, value)).orElse(null);
            if (expected == null || !expected.id().equals(ingredient.getId())) {
                return reject(player, "tooltip.kaleidoscope_hodgepodge.recipe_wrong_ingredient");
            }
            target = new IngredientPlacementTarget.Pixel(expected.x(), expected.z());
            placementRotation = expected.rotation();
        } else {
            target = IngredientPlacementTarget.resolve(existing, pos,
                    player.getEyePosition(), hit, ingredient, placementRotation,
                    feast.placementBounds(), allowsBoundaryPlacementProjection()).orElse(null);
        }
        if (target == null) return ItemInteractionResult.FAIL;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        IngredientFoodData food = IngredientFoodService.resolve(baggedIngredient.id(), baggedIngredient.food());
        PlacementSpace.Result result = feast.addAgainst(existing, ingredient, target.x(), target.z(),
                placementRotation, food);
        if (!result.success()) {
            return reject(player, "tooltip.kaleidoscope_hodgepodge.placement_" + result.failure().name().toLowerCase());
        }
        ModTrigger.EVENT.get().trigger(player, Types.DIY_FEAST);
        PackingBagService.replaceHeldBag(stack, player, contents.withoutFirst());
        CrashDiagnostics.record("placed " + ingredient.getId() + " at " + pos
                + " pixel=" + target.x() + "," + target.z());
        level.playSound(null, pos, SoundEvents.CAKE_ADD_CANDLE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (GeneralConfig.snapshot().debugLogging()) {
            KaleidoscopeHodgepodge.LOGGER.info("Placed ingredient {} at {} pixel {},{}",
                    ingredient.getId(), pos, target.x(), target.z());
        }
        return ItemInteractionResult.SUCCESS;
    }

    private ItemInteractionResult useLunchBox(ItemStack lunchBox, BlockState state, Level level, BlockPos pos,
                                          Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) {
            return ItemInteractionResult.FAIL;
        }
        LunchBoxContents contents = LunchBoxService.get(lunchBox);
        if (LunchBoxService.getMode(lunchBox) == PackingBagMode.STORAGE) {
            if (feast.isRecipeLocked()) return reject(player, "tooltip.kaleidoscope_hodgepodge.recipe_locked");
            return retrieveLunchBoxIngredient(lunchBox, contents, level, pos, player, hit);
        }
        BaggedIngredient baggedIngredient = LunchBoxService.selectedIngredient(lunchBox);
        if (baggedIngredient == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (feast.isRecipeLocked()) {
            PlacedIngredient expected = feast.nextRecipePlacement().orElse(null);
            if (expected == null) return ItemInteractionResult.FAIL;
            BlockPos targetPos = recipePlacementPos(pos, state, expected);
            if (!targetPos.equals(pos)) {
                return useLunchBox(lunchBox, level.getBlockState(targetPos), level, targetPos, player, hit);
            }
        }
        PackingIngredients ingredient = PackingIngredientRegistry.byId(baggedIngredient.id()).orElse(null);
        if (ingredient == null) return reject(player, "tooltip.kaleidoscope_hodgepodge.unknown_ingredient");
        if (!isSuitable(ingredient)) {
            return reject(player, kind == CustomFeastData.ContainerKind.SOUP
                    ? "tooltip.kaleidoscope_hodgepodge.not_applicable_to_soup"
                    : "tooltip.kaleidoscope_hodgepodge.not_applicable_to_dish");
        }
        List<PlacedIngredient> existing = placementIngredients(level, pos, state);
        int placementRotation = baggedIngredient.rotation();
        IngredientPlacementTarget.Pixel target;
        if (feast.isRecipeLocked()) {
            PlacedIngredient expected = feast.nextRecipePlacement()
                    .map(value -> ((IHodgepodge) this).recipePlacementTarget(pos, state, value)).orElse(null);
            if (expected == null || !expected.id().equals(ingredient.getId())) {
                return reject(player, "tooltip.kaleidoscope_hodgepodge.recipe_wrong_ingredient");
            }
            target = new IngredientPlacementTarget.Pixel(expected.x(), expected.z());
            placementRotation = expected.rotation();
        } else {
            target = IngredientPlacementTarget.resolve(existing, pos,
                    player.getEyePosition(), hit, ingredient, placementRotation,
                    feast.placementBounds(), allowsBoundaryPlacementProjection()).orElse(null);
        }
        if (target == null) return ItemInteractionResult.FAIL;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        IngredientFoodData food = IngredientFoodService.resolve(baggedIngredient.id(), baggedIngredient.food());
        PlacementSpace.Result result = feast.addAgainst(existing, ingredient, target.x(), target.z(),
                placementRotation, food);
        if (!result.success()) {
            return reject(player, "tooltip.kaleidoscope_hodgepodge.placement_"
                    + result.failure().name().toLowerCase());
        }
        int selected = LunchBoxService.selectedSlot(lunchBox);
        LunchBoxContents.RemovalResult removed = contents.removeFirst(selected);
        if (removed.removed().isEmpty()) return ItemInteractionResult.FAIL;
        LunchBoxService.set(lunchBox, removed.contents());
        ModTrigger.EVENT.get().trigger(player, Types.DIY_FEAST);
        CrashDiagnostics.record("placed lunch-box ingredient " + ingredient.getId() + " at " + pos
                + " pixel=" + target.x() + "," + target.z());
        level.playSound(null, pos, SoundEvents.CAKE_ADD_CANDLE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return ItemInteractionResult.SUCCESS;
    }

    private ItemInteractionResult retrieveLunchBoxIngredient(ItemStack lunchBox, LunchBoxContents contents,
                                                         Level level, BlockPos pos, Player player,
                                                         BlockHitResult hit) {
        List<IngredientReference> references = ingredientReferences(level, pos, level.getBlockState(pos));
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
        BaggedIngredient packed = new BaggedIngredient(selectedIngredient.id(), selectedIngredient.rotation(),
                IngredientFoodService.resolve(selectedIngredient.id(), selectedIngredient.food()));
        LunchBoxContents.InsertResult result = contents.insert(List.of(packed));
        if (!result.remainder().isEmpty()) return reject(player, "tooltip.kaleidoscope_hodgepodge.storage_full");
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        PlacedIngredient removed = reference.owner().removeIngredient(reference.index()).orElse(null);
        if (removed == null) return ItemInteractionResult.FAIL;
        LunchBoxService.set(lunchBox, result.contents());
        CrashDiagnostics.record("retrieved lunch-box ingredient " + removed.id() + " from " + pos);
        level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0F, 1.0F);
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
        level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0F, 1.0F);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level,
                                                     @NotNull BlockPos pos, @NotNull Player player,
                                                     @NotNull BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || player.isSpectator() || !player.mayBuild()) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast && feast.isRecipeLocked()) {
            return reject(player, "tooltip.kaleidoscope_hodgepodge.recipe_locked").result();
        }
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
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS,
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
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        VoxelShape container = getContainerShape(state, level, pos, context);
        if (level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast) {
            int stateHash = state.hashCode();
            if (feast.hasOutlineShape(stateHash)) return feast.outlineShape();
            VoxelShape outline = Shapes.or(container, ingredientShape(level, pos, state));
            feast.cacheOutlineShape(stateHash, outline);
            return outline;
        }
        return Shapes.or(container, ingredientShape(level, pos, state));
    }

    protected VoxelShape getContainerShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return CONTAINER_SHAPE;
    }

    /**
     * Ingredient collision is configurable and uses four coarse 8px by 8px
     * height-map columns instead of one collision box per ingredient.
     */
    @Override
    protected @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                                    @NotNull CollisionContext context) {
        VoxelShape container = getContainerShape(state, level, pos, context);
        if (!GeneralConfig.snapshot().ingredientModelCollision()
                || !(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) {
            return container;
        }
        int stateHash = state.hashCode();
        if (feast.hasCollisionShape(stateHash)) return feast.collisionShape();
        VoxelShape collision = Shapes.or(container, feast.ingredientCollisionShape());
        feast.cacheCollisionShape(stateHash, collision);
        return collision;
    }

    @Override
    public VoxelShape containerOutlineShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return getContainerShape(state, level, pos, context);
    }

    @Override
    protected boolean triggerEvent(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, int id, int data) {
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
        if (level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast) {
            feast.setDishName(DishName.get(stack));
        }
        CustomFeastData data = stack.get(KHDataComponents.CUSTOM_FEAST.get());
        if (data != null && data.kind() == kind && level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast) {
            feast.setIngredients(data.ingredients());
        }
    }

    protected final ItemStack createDrop(BlockState state, @Nullable HodgepodgeFeastBlockEntity feast) {
        ItemStack stack = new ItemStack(this);
        applyContainerStateToItem(stack, state);
        if (feast != null) DishName.set(stack, feast.dishName());
        if (feast != null && !feast.ingredients().isEmpty()) stack.set(KHDataComponents.CUSTOM_FEAST.get(), feast.snapshot());
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
            return List.of(createDrop(state, feast));
        }
        return List.of(createDrop(state, feast));
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
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
