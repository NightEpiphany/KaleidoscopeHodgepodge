package com.moigferdsrte.kaleidoscopehodgepodge.item;

import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodService;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.FeastIngredientsTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CustomFeastBlockItem extends BlockItem {
    private static final int EAT_DURATION_TICKS = 32;
    public final boolean isSoup;

    public final boolean isSpecial;

    public CustomFeastBlockItem(Block block, Properties properties) {
        this(block, false, false, properties);
    }

    public CustomFeastBlockItem(Block block, boolean isSoup, boolean isSpecial, Properties properties) {
        super(block, properties.stacksTo(1));
        this.isSpecial = isSpecial;
        this.isSoup = isSoup;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST.get());
        if (feast == null || feast.ingredients().isEmpty()) return super.getName(stack);
        return Component.translatable(feast.kind() == CustomFeastData.ContainerKind.SOUP
                ? "item.kaleidoscope_hodgepodge.custom_soup"
                : "item.kaleidoscope_hodgepodge.custom_dish");
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST.get());
        if (feast == null || feast.ingredients().isEmpty()) return Optional.empty();
        return Optional.of(new FeastIngredientsTooltip(
                feast.ingredients().stream().map(PlacedIngredient::id).toList()));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST.get());
        if (feast == null || feast.ingredients().isEmpty()) return super.use(level, player, hand);
        if (!GeneralConfig.snapshot().allowsHandheldEating(feast.kind())) return super.use(level, player, hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST.get());
        return feast == null || !GeneralConfig.snapshot().allowsHandheldEating(feast.kind())
                || feast.ingredients().isEmpty() ? 0 : EAT_DURATION_TICKS;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST.get());
        if (feast == null || feast.ingredients().isEmpty()
                || !GeneralConfig.snapshot().allowsHandheldEating(feast.kind())) return UseAnim.NONE;
        return feast.kind() == CustomFeastData.ContainerKind.SOUP ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level,
                                              @NotNull LivingEntity entity) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST.get());
        if (feast == null || feast.ingredients().isEmpty()
                || !GeneralConfig.snapshot().allowsHandheldEating(feast.kind())
                || !(entity instanceof Player player)) return stack;
        if (level.isClientSide()) return stack;

        IngredientFoodService.applyAll(level, player, feast.ingredients().stream()
                .map(ingredient -> IngredientFoodService.resolveForConsumption(ingredient.id(), ingredient.food()))
                .toList());
        level.playSound(null, player.blockPosition(),
                feast.kind() == CustomFeastData.ContainerKind.SOUP
                        ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT, SoundSource.PLAYERS,
                0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(player, GameEvent.EAT, player.blockPosition());
        if (player.isCreative()) return stack;

        ItemStack container = new ItemStack(this);
        if (isSoup) container.remove(KHDataComponents.SOUP_BASE.get());
        if (stack.getCount() == 1) return container;
        stack.shrink(1);
        if (!player.addItem(container)) player.drop(container, false);
        return stack;
    }
}
