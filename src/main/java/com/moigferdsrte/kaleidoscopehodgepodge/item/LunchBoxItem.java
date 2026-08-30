package com.moigferdsrte.kaleidoscopehodgepodge.item;

import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.LunchBoxTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.api.IHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.function.Consumer;

/** Fifteen-slot ingredient carrier with persistent selection and storage/placement modes. */
public final class LunchBoxItem extends Item {
    public LunchBoxItem(Properties properties) {
        super(properties.stacksTo(1)
                .component(KHDataComponents.LUNCH_BOX_MODE, PackingBagMode.STORAGE)
                .component(KHDataComponents.LUNCH_BOX_SELECTED_SLOT, -1));
    }

    @Override
    public @NonNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player != null && player.isSecondaryUseActive()) {
            return switchMode(context.getLevel(), player, stack);
        }
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof IHodgepodge)) {
            return openMenu(context.getLevel(), player, context.getHand(), stack);
        }
        if (LunchBoxService.getMode(stack) == PackingBagMode.PLACEMENT
                && LunchBoxService.selectedIngredient(stack) == null) {
            return openMenu(context.getLevel(), player, context.getHand(), stack);
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NonNull InteractionResult use(@NonNull Level level, Player player, @NonNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) return switchMode(level, player, stack);
        return openMenu(level, player, hand, stack);
    }

    private static InteractionResult openMenu(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (player == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            LunchBoxService.migrateIfNeeded(stack, player);
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, _) -> new LunchBoxMenu(containerId, inventory, stack, hand),
                    stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult switchMode(Level level, Player player, ItemStack stack) {
        PackingBagMode next = LunchBoxService.getMode(stack).next();
        LunchBoxService.setMode(stack, next);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                    "tooltip.kaleidoscope_hodgepodge.bag_mode_changed",
                    Component.translatable(next.translationKey()))));
        }
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(@NonNull ItemStack stack, @NonNull TooltipContext context,
                                @NonNull TooltipDisplay display, @NonNull Consumer<Component> builder,
                                @NonNull TooltipFlag tooltipFlag) {
        if (!tooltipFlag.isCreative()) {
            builder.accept(Component.translatable("tooltip.kaleidoscope_hodgepodge.bag_mode",
                    Component.translatable(LunchBoxService.getMode(stack).translationKey())));
        }
        super.appendHoverText(stack, context, display, builder, tooltipFlag);
    }

    @Override
    public @NonNull Optional<TooltipComponent> getTooltipImage(@NonNull ItemStack stack) {
        var contents = LunchBoxService.get(stack);
        BaggedIngredient selected = LunchBoxService.selectedIngredient(stack);
        Optional<Identifier> selectedId = selected == null
                ? Optional.empty() : Optional.of(selected.id());
        return Optional.of(new LunchBoxTooltip(
                selectedId, contents.unitCount(), contents.occupiedSlotCount()));
    }
}
