package com.moigferdsrte.kaleidoscopehodgepodge.item;

import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.DishName;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodService;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.FeastIngredientsTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.NonNull;

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
    public @NonNull Component getName(@NonNull ItemStack stack) {
        Optional<Component> name = DishName.get(stack);
        if (name.isPresent()) return name.get();
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
        if (feast == null || feast.ingredients().isEmpty()) return super.getName(stack);
        return Component.translatable(feast.kind() == CustomFeastData.ContainerKind.SOUP
                ? "item.kaleidoscope_hodgepodge.custom_soup"
                : "item.kaleidoscope_hodgepodge.custom_dish");
    }

    @Override
    public @NonNull Optional<TooltipComponent> getTooltipImage(@NonNull ItemStack stack) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
        if (feast == null || feast.ingredients().isEmpty()) return Optional.empty();
        return Optional.of(new FeastIngredientsTooltip(
                feast.ingredients().stream().map(PlacedIngredient::id).toList()));
    }

    @Override
    public @NonNull InteractionResult use(@NonNull Level level, @NonNull Player player,
                                          @NonNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
        if (feast == null || feast.ingredients().isEmpty()) return super.use(level, player, hand);
        if (!handheldEatingAllowed()) return super.use(level, player, hand);
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(@NonNull ItemStack stack, @NonNull LivingEntity entity) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
        return !handheldEatingAllowed()
                || feast == null || feast.ingredients().isEmpty() ? 0 : EAT_DURATION_TICKS;
    }

    @Override
    public @NonNull ItemUseAnimation getUseAnimation(@NonNull ItemStack stack) {
        return handheldEatingAllowed()
                && stack.has(KHDataComponents.CUSTOM_FEAST) ? ItemUseAnimation.EAT : ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(@NonNull Level level, @NonNull LivingEntity entity,
                          @NonNull ItemStack stack, int remainingTicks) {
        if (!handheldEatingAllowed()) return;
        Consumable consumable = this.isSoup ? Consumables.defaultDrink().build() : Consumables.defaultFood().build();
        if (consumable.shouldEmitParticlesAndSounds(remainingTicks)) {
            consumable.emitParticlesAndSounds(entity.getRandom(), entity, stack, 5);
        }
    }

    @Override
    public @NonNull ItemStack finishUsingItem(@NonNull ItemStack stack, @NonNull Level level,
                                              @NonNull LivingEntity entity) {
        if (!handheldEatingAllowed()) return stack;
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
        if (feast == null || feast.ingredients().isEmpty() || !(entity instanceof Player player)) return stack;
        if (level.isClientSide()) return stack;

        IngredientFoodService.applyAll(level, player, feast.ingredients().stream()
                .map(ingredient -> IngredientFoodService.resolveForConsumption(ingredient.id(), ingredient.food()))
                .toList());
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS,
                0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(player, GameEvent.EAT, player.blockPosition());
        if (player.isCreative()) return stack;

        ItemStack container = new ItemStack(this);
        if (isSoup) container.remove(KHDataComponents.SOUP_BASE);
        if (stack.getCount() == 1) return container;
        stack.shrink(1);
        if (!player.addItem(container)) player.drop(container, false);
        return stack;
    }

    private boolean handheldEatingAllowed() {
        GeneralConfig.Snapshot config = GeneralConfig.snapshot();
        return isSoup ? config.allowHandheldSoupEating() : config.allowHandheldDishEating();
    }
}
