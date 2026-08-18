package com.moigferdsrte.kaleidoscopehodgepodge.item;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.StackableFoodBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.*;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.IngredientTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor.StackableFoodBlockAccessor;
import com.moigferdsrte.kaleidoscopehodgepodge.util.CrashDiagnostics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WrappingBagItem extends Item {
    public WrappingBagItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        ItemStack bag = context.getItemInHand();
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            return switchMode(context.getLevel(), player, bag);
        }
        if (PackingBagService.getMode(bag) != PackingBagMode.STORAGE) return InteractionResult.PASS;
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof FoodBiteBlock)
                && !(state.getBlock() instanceof StackableFoodBlock)
                && !(state.getBlock() instanceof CakeBlock)) {
            return InteractionResult.PASS;
        }
        PackingBagContents current = PackingBagService.get(bag);
        if (current.isFull()) return warn(player, "tooltip.kaleidoscope_hodgepodge.storage_full");
        ResourceLocation sourceId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        List<PackingIngredients> candidates = PackingIngredientRegistry.bySource(sourceId);
        if (candidates.isEmpty()) return InteractionResult.PASS;
        if (state.getBlock() instanceof FoodBiteBlock food) {
            if (state.getValue(food.getBites()) != 0) {
                return warn(player, "tooltip.kaleidoscope_hodgepodge.dish_must_be_whole");
            }
            IngredientFoodData foodData = IngredientFoodService.capture(state.getBlock());
            PackingBagContents packedDish = PackingBagService.fromWholeDish(candidates, sourceId, foodData);
            if (packedDish.isEmpty()) return InteractionResult.PASS;
            PackingBagContents updated = current.withAll(packedDish.ingredients()).orElse(null);
            if (updated == null) return warn(player, "tooltip.kaleidoscope_hodgepodge.storage_full");
            if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
            FoodBiteStructureService.removePackedDish(context.getLevel(), context.getClickedPos(), state);
            context.getLevel().levelEvent(null, 2001, context.getClickedPos(), Block.getId(state));
            context.getLevel().removeBlock(context.getClickedPos(), false);
            PackingBagService.replaceHeldBag(bag, player, updated);
            recordPacked(context, packedDish.ingredients().size() + " ingredients", sourceId);
            if (player != null) ItemUtils.giveItemToPlayer(player, Items.BOWL.getDefaultInstance());
        } else if (state.getBlock() instanceof StackableFoodBlock food) {
            if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
            PackingIngredients ingredient = candidates.get(context.getLevel().getRandom().nextInt(candidates.size()));
            IngredientFoodData foodData = IngredientFoodService.capture(state.getBlock());
            PackingBagContents updated = current.with(new BaggedIngredient(ingredient.getId(), 0, foodData))
                    .orElse(null);
            if (updated == null) return warn(player, "tooltip.kaleidoscope_hodgepodge.storage_full");
            IntegerProperty countProperty = ((StackableFoodBlockAccessor) food)
                    .kaleidoscopeHodgepodge$getCountProperty();
            int count = state.getValue(countProperty);
            if (count <= 1) context.getLevel().removeBlock(context.getClickedPos(), false);
            else context.getLevel().setBlockAndUpdate(context.getClickedPos(), state.setValue(countProperty, count - 1));
            PackingBagService.replaceHeldBag(bag, player, updated);
            recordPacked(context, ingredient.getId().toString(), sourceId);
        } else if (state.getBlock() instanceof CakeBlock) {
            if (state.getValue(CakeBlock.BITES) != 0) {
                return warn(player, "tooltip.kaleidoscope_hodgepodge.dish_must_be_whole");
            }
            PackingIngredients ingredient = candidates.getFirst();
            IngredientFoodData foodData = IngredientFoodService.capture(state.getBlock());
            PackingBagContents updated = current.with(new BaggedIngredient(ingredient.getId(), 0, foodData))
                    .orElse(null);
            if (updated == null) return warn(player, "tooltip.kaleidoscope_hodgepodge.storage_full");
            if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
            context.getLevel().levelEvent(null, 2001, context.getClickedPos(), Block.getId(state));
            context.getLevel().removeBlock(context.getClickedPos(), false);
            PackingBagService.replaceHeldBag(bag, player, updated);
            recordPacked(context, ingredient.getId().toString(), sourceId);
        } else {
            return InteractionResult.PASS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack bag = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) return InteractionResultHolder.pass(bag);
        return new InteractionResultHolder<>(switchMode(level, player, bag), bag);
    }

    private static InteractionResult switchMode(Level level, Player player, ItemStack bag) {
        PackingBagMode next = PackingBagService.getMode(bag).next();
        // 堆叠纸袋只由服务端拆分，单个纸袋可在客户端立即更新手感。
        if (!level.isClientSide() || bag.getCount() == 1) {
            PackingBagService.replaceHeldBagMode(bag, player, next);
        }
        if (!level.isClientSide()) {
            sendActionBar(player, Component.translatable("tooltip.kaleidoscope_hodgepodge.bag_mode_changed",
                    Component.translatable(next.translationKey())));
        }
        return InteractionResult.SUCCESS;
    }

    private static void recordPacked(UseOnContext context, String packed, ResourceLocation source) {
        CrashDiagnostics.record("packed " + packed + " from " + source + " at " + context.getClickedPos());
        if (GeneralConfig.snapshot().debugLogging()) {
            KaleidoscopeHodgepodge.LOGGER.info("Packed {} from {} at {}", packed, source, context.getClickedPos());
        }
    }

    private static InteractionResult warn(Player player, String key) {
        sendActionBar(player, Component.translatable(key));
        return InteractionResult.FAIL;
    }

    private static void sendActionBar(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack itemStack,
            @NotNull TooltipContext context,
            @NotNull List<Component> tooltip,
            @NotNull TooltipFlag tooltipFlag
    ) {
        PackingBagContents contents = PackingBagService.get(itemStack);
        if (!tooltipFlag.isCreative())
            tooltip.add(Component.translatable("tooltip.kaleidoscope_hodgepodge.bag_mode",
                Component.translatable(PackingBagService.getMode(itemStack).translationKey()))
                .withStyle(ChatFormatting.GRAY));
        if (!contents.isEmpty() && Screen.hasShiftDown()) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            contents.ingredients().forEach(ingredient -> counts.merge(ingredient.id().toString(), 1, Integer::sum));
            counts.forEach((id, count) -> {
                String value = count > 1 ? id + " x" + count : id;
                Component ingredientId = Component.literal(value)
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC);
                tooltip.add(Component.translatable("tooltip.kaleidoscope_hodgepodge.contained_ingredient", ingredientId)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            });
        }
        if (PackingBagService.has(itemStack) && !Screen.hasShiftDown())
            tooltip.add(Component.translatable("tooltip.kaleidoscope_hodgepodge.shift_for_more")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(itemStack, context, tooltip, tooltipFlag);
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        PackingBagContents contents = PackingBagService.get(stack);
        if (contents.isEmpty() || Screen.hasShiftDown()) return Optional.empty();
        List<ResourceLocation> ingredientIds = contents.ingredients().stream().map(BaggedIngredient::id).toList();
        List<ResourceLocation> sourceIds = PackingIngredientRegistry.sourceIdsFor(ingredientIds);
        return sourceIds.isEmpty() ? Optional.empty() : Optional.of(new IngredientTooltip(ingredientIds, sourceIds));
    }
}
