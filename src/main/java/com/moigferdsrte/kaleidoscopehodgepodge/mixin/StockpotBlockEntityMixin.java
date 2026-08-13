package com.moigferdsrte.kaleidoscopehodgepodge.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.IStockpot;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.serializer.StockpotRecipeSerializer;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSoupBases;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CookwarePackingService;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StockpotBlockEntity.class)
public abstract class StockpotBlockEntityMixin {
    @Shadow private NonNullList<ItemStack> inputs;
    @Shadow private Identifier recipeId;
    @Shadow private Identifier soupBaseId;
    @Shadow private ItemStack result;
    @Shadow private int status;
    @Shadow private int currentTick;
    @Shadow private int takeoutCount;
    @Shadow private Ingredient carrier;
    @Shadow private Identifier cookingTexture;
    @Shadow private Identifier finishedTexture;
    @Shadow private int cookingBubbleColor;
    @Shadow private int finishedBubbleColor;
    @Shadow private boolean flexRecipe;
    @Shadow public @Nullable Entity renderEntity;

    @Inject(method = "takeOutProduct", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeHodgepodge$packFinishedDish(Level level, LivingEntity user, ItemStack stack,
                                                         CallbackInfoReturnable<Boolean> cir) {
        StockpotBlockEntity stockpot = (StockpotBlockEntity) (Object) this;
        if (level.isClientSide() || stockpot.hasLid() || status != IStockpot.FINISHED
                || result.isEmpty() || takeoutCount <= 0) return;
        if (!CookwarePackingService.tryPack(level, user, stack, result)) return;

        takeoutCount--;
        if (takeoutCount <= 0) resetAfterLastServing();
        stockpot.refresh();
        cir.setReturnValue(true);
    }

    private void resetAfterLastServing() {
        status = IStockpot.PUT_SOUP_BASE;
        inputs.clear();
        recipeId = StockpotRecipeSerializer.EMPTY_ID;
        soupBaseId = ModSoupBases.WATER;
        result = ItemStack.EMPTY;
        currentTick = -1;
        renderEntity = null;
        flexRecipe = false;
        carrier = StockpotRecipeSerializer.DEFAULT_CARRIER;
        cookingTexture = StockpotRecipeSerializer.DEFAULT_COOKING_TEXTURE;
        finishedTexture = StockpotRecipeSerializer.DEFAULT_FINISHED_TEXTURE;
        cookingBubbleColor = StockpotRecipeSerializer.DEFAULT_COOKING_BUBBLE_COLOR;
        finishedBubbleColor = StockpotRecipeSerializer.DEFAULT_FINISHED_BUBBLE_COLOR;
    }
}
