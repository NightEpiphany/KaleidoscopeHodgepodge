package com.moigferdsrte.kaleidoscopehodgepodge.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.github.ysbbbbbb.kaleidoscopecookery.item.RecipeItem;
import com.moigferdsrte.kaleidoscopehodgepodge.advancements.Types;
import com.moigferdsrte.kaleidoscopehodgepodge.core.FeastCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.HodgepodgeRecipeData;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeItem.class)
public class NormalRecipeRecordTransferMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void hodgepodge$record(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide() || context.getPlayer() == null)
            return;
        ItemStack stack = context.getItemInHand();
        if (!stack.is(ModItems.RECIPE_ITEM) || RecipeItem.hasRecipe(stack))
            return;
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof HodgepodgeFeastBlockEntity feast))
            return;
        CustomFeastData snapshot = feast.recipeSnapshot();
        if (snapshot.ingredients().isEmpty()) return;
        Item containerItem = feast.getBlockState().getBlock().asItem();
        if (containerItem == net.minecraft.world.item.Items.AIR)
            return;
        String container = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(containerItem).toString();
        String code = FeastCodec.encode(container, snapshot);
        ItemStack result = new ItemStack(KHItems.HODGEPODGE_RECIPE);
        result.set(KHDataComponents.HODGEPODGE_RECIPE,
                new HodgepodgeRecipeData(code, context.getPlayer().getGameProfile(), feast.dishName()));
        stack.shrink(1);
        context.getPlayer().setItemInHand(context.getHand(), result);
        ModTrigger.EVENT.trigger(context.getPlayer(), Types.INNOVATION_BOOMING);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
