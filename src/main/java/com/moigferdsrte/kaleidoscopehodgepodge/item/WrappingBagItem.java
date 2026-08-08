package com.moigferdsrte.kaleidoscopehodgepodge.item;

import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.StackableFoodBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.util.CrashDiagnostics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;

public class WrappingBagItem extends Item {
    public WrappingBagItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NonNull InteractionResult useOn(UseOnContext context) {
        ItemStack bag = context.getItemInHand();
        if (bag.has(KHDataComponents.PACKING_BAG_INGREDIENT)) {
            warn(context, "tooltip.kaleidoscope_hodgepodge.bag_full");
            return InteractionResult.FAIL;
        }
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        List<PackingIngredients> candidates = PackingIngredientRegistry.bySource(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        if (candidates.isEmpty()) return InteractionResult.PASS;

        PackingIngredients ingredient;
        if (state.getBlock() instanceof FoodBiteBlock food) {
            int bites = state.getValue(food.getBites());
            ingredient = candidates.get(Math.min(bites, candidates.size() - 1));
            if (!context.getLevel().isClientSide()) {
                if (bites >= food.getMaxBites()) context.getLevel().removeBlock(context.getClickedPos(), false);
                else context.getLevel().setBlockAndUpdate(context.getClickedPos(), state.setValue(food.getBites(), bites + 1));
            }
        } else if (state.getBlock() instanceof StackableFoodBlock food) {
            int count = state.getValue(food.getCountProperty());
            ingredient = candidates.get(Math.min(candidates.size() - 1, Math.max(0, food.getMaxCount() - count)));
            if (!context.getLevel().isClientSide()) {
                if (count <= 1) context.getLevel().removeBlock(context.getClickedPos(), false);
                else context.getLevel().setBlockAndUpdate(context.getClickedPos(), state.setValue(food.getCountProperty(), count - 1));
            }
        } else {
            return InteractionResult.PASS;
        }

        if (!context.getLevel().isClientSide()) {
            bag.set(KHDataComponents.PACKING_BAG_INGREDIENT, ingredient.getId().toString());
            CrashDiagnostics.record("packed " + ingredient.getId() + " from " + ingredient.getSrcFoodId()
                    + " at " + context.getClickedPos());
            if (GeneralConfig.snapshot().debugLogging()) {
                KaleidoscopeHodgepodge.LOGGER.info("Packed ingredient {} from {} at {}",
                        ingredient.getId(), ingredient.getSrcFoodId(), context.getClickedPos());
            }
        }
        return context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private static void warn(UseOnContext context, String key) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(key)));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(
            @NonNull ItemStack itemStack,
            @NonNull TooltipContext context,
            @NonNull TooltipDisplay display,
            @NonNull Consumer<Component> builder,
            @NonNull TooltipFlag tooltipFlag
    ) {
        String id = itemStack.get(KHDataComponents.PACKING_BAG_INGREDIENT);
        if (id != null) {
            PackingIngredientRegistry.byId(id).ifPresent(ingredient -> builder.accept(Component.translatable(
                    "tooltip.kaleidoscope_hodgepodge.packed_ingredient",
                    ingredient.getId().toString(), ingredient.getSrcFoodId().toString())));
        }
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
    }
}
