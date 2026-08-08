package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.api.IHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientHitTest;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.util.CrashDiagnostics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
        String ingredientId = stack.get(KHDataComponents.PACKING_BAG_INGREDIENT);
        boolean retrieving = ingredientId == null && stack.is(KHItems.WRAPPING_BAG);
        if (ingredientId == null && !retrieving) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)) {
            return InteractionResult.FAIL;
        }
        if (retrieving) return retrieveIngredient(stack, level, pos, player, hit, feast);
        if (hit.getDirection() != Direction.UP) return InteractionResult.FAIL;
        PackingIngredients ingredient = PackingIngredientRegistry.byId(ingredientId).orElse(null);
        if (ingredient == null) return reject(player, "tooltip.kaleidoscope_hodgepodge.unknown_ingredient");
        if (!isSuitable(ingredient)) {
            return reject(player, kind == CustomFeastData.ContainerKind.SOUP
                    ? "tooltip.kaleidoscope_hodgepodge.not_applicable_to_soup"
                    : "tooltip.kaleidoscope_hodgepodge.not_applicable_to_dish");
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int hitX = Math.max(0, Math.min(15, (int) Math.floor((hit.getLocation().x - pos.getX()) * 16.0)));
        int hitZ = Math.max(0, Math.min(15, (int) Math.floor((hit.getLocation().z - pos.getZ()) * 16.0)));
        PlacementSpace.Result result = feast.add(ingredient, hitX, hitZ);
        if (!result.success()) {
            return reject(player, "tooltip.kaleidoscope_hodgepodge.placement_" + result.failure().name().toLowerCase());
        }
        stack.remove(KHDataComponents.PACKING_BAG_INGREDIENT);
        CrashDiagnostics.record("placed " + ingredient.getId() + " at " + pos + " pixel=" + hitX + "," + hitZ);
        if (GeneralConfig.snapshot().debugLogging()) {
            KaleidoscopeHodgepodge.LOGGER.info("Placed ingredient {} at {} pixel {},{}", ingredient.getId(), pos, hitX, hitZ);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult retrieveIngredient(ItemStack bag, Level level, BlockPos pos, Player player,
                                                        BlockHitResult hit, HodgepodgeFeastBlockEntity feast) {
        Vec3 from = player.getEyePosition();
        Vec3 ray = hit.getLocation().subtract(from);
        Vec3 to = ray.lengthSqr() > 1.0E-7
                ? hit.getLocation().add(ray.normalize().scale(1.0 / 16.0))
                : hit.getLocation();
        OptionalInt selected = IngredientHitTest.nearest(feast.renderIngredients(), pos, from, to);
        if (selected.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        PlacedIngredient removed = feast.removeIngredient(selected.getAsInt()).orElse(null);
        if (removed == null) return InteractionResult.FAIL;
        bag.set(KHDataComponents.PACKING_BAG_INGREDIENT, removed.id().toString());
        CrashDiagnostics.record("retrieved " + removed.id() + " from " + pos);
        if (GeneralConfig.snapshot().debugLogging()) {
            KaleidoscopeHodgepodge.LOGGER.info("Retrieved ingredient {} from {}", removed.id(), pos);
        }
        return InteractionResult.SUCCESS;
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

    private ItemStack createDrop(@Nullable HodgepodgeFeastBlockEntity feast) {
        ItemStack stack = new ItemStack(this);
        if (feast != null && !feast.ingredients().isEmpty()) stack.set(KHDataComponents.CUSTOM_FEAST, feast.snapshot());
        return stack;
    }

    @Override
    public @NonNull List<ItemStack> getDrops(@NonNull BlockState state, LootParams.@NonNull Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(entity instanceof HodgepodgeFeastBlockEntity feast) || feast.ingredients().isEmpty()) {
            return List.of();
        }
        return List.of(createDrop(feast));
    }

    @Override
    public @NonNull BlockState playerWillDestroy(Level level, @NonNull BlockPos pos, @NonNull BlockState state,
                                                 @NonNull Player player) {
        if (!level.isClientSide() && player.isCreative()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof HodgepodgeFeastBlockEntity feast && !feast.ingredients().isEmpty()) {
                popResource(level, pos, createDrop(feast));
                CrashDiagnostics.record("creative drop at " + pos);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected @NonNull ItemStack getCloneItemStack(@NonNull LevelReader level, @NonNull BlockPos pos,
                                                   @NonNull BlockState state, boolean includeData) {
        BlockEntity entity = level.getBlockEntity(pos);
        return createDrop(entity instanceof HodgepodgeFeastBlockEntity feast ? feast : null);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new HodgepodgeFeastBlockEntity(pos, state);
    }
}
