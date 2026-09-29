package com.moigferdsrte.kaleidoscopehodgepodge.item;

import com.moigferdsrte.kaleidoscopehodgepodge.core.HodgepodgeRecipeData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.DishName;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.util.CrashDiagnostics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.function.Consumer;
import java.util.Optional;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.HodgepodgeRecipeTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.FeastCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import org.jetbrains.annotations.NotNull;

public final class HodgepodgeRecipeItem extends BlockItem {
    public HodgepodgeRecipeItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static HodgepodgeRecipeData get(ItemStack stack) { return stack.get(KHDataComponents.HODGEPODGE_RECIPE.get()); }

    public static boolean has(ItemStack stack) { return stack.has(KHDataComponents.HODGEPODGE_RECIPE.get()); }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        HodgepodgeRecipeData data = get(stack);
        if (data == null) return Optional.empty();
        try {
            FeastCodec.Decoded decoded = FeastCodec.decode(data.feastCode());
            Item container = BuiltInRegistries.ITEM.getOptional(
                    net.minecraft.resources.ResourceLocation.parse(decoded.containerPath())).orElse(null);
            if (container == null || container == net.minecraft.world.item.Items.AIR) return Optional.empty();
            ItemStack preview = new ItemStack(container);
            preview.set(KHDataComponents.CUSTOM_FEAST.get(), decoded.feast());
            DishName.set(preview, data.dishName());
            return Optional.of(new HodgepodgeRecipeTooltip(preview,
                    decoded.feast().ingredients(), data.owner(), data.ownerProfile(), data.dishName()));
        } catch (FeastCodec.FormatException | RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return has(stack) ? Component.translatable("item.kaleidoscope_hodgepodge.hodgepodge_recipe.recorded")
                : super.getName(stack);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof HodgepodgeFeastBlockEntity feast))
            return super.useOn(context);
        HodgepodgeRecipeData data = get(context.getItemInHand());
        if (data == null) return super.useOn(context);
        if (feast.isRecipeLocked()) {
            if (feast.lockedRecipe().equals(data)) {
                feast.clearLockedRecipe();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        try {
            FeastCodec.Decoded decoded = FeastCodec.decode(data.feastCode());
            String targetContainer = BuiltInRegistries.ITEM.getKey(feast.getBlockState().getBlock().asItem()).toString();
            if (!targetContainer.equals(decoded.containerPath())) {
                fail(context, "tooltip.kaleidoscope_hodgepodge.recipe_container_mismatch");
                return InteractionResult.FAIL;
            }
            if (!feast.recipeSnapshot().ingredients().isEmpty()
                    || decoded.feast().ingredients().isEmpty()
                    || decoded.feast().kind() != feast.kind()) {
                return InteractionResult.FAIL;
            }
        } catch (FeastCodec.FormatException | RuntimeException e) {
            CrashDiagnostics.record(e.getMessage());
            return InteractionResult.FAIL;
        }
        feast.setLockedRecipe(data);
        return InteractionResult.SUCCESS;
    }

    private static void fail(UseOnContext context, String key) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(key)));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @NotNull TooltipContext context,
            @NotNull java.util.List<Component> tooltip,
            @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        HodgepodgeRecipeData data = get(stack);
        if (data != null)
            tooltip.add(Component.translatable("tooltip.kaleidoscope_hodgepodge.recipe_models",
                    Math.max(0, data.feastCode().split("\\|", -1).length - 5)));
    }
}
