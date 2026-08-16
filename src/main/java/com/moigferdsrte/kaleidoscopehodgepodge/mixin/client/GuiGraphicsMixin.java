package com.moigferdsrte.kaleidoscopehodgepodge.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.WrappingBagGuiModelResolver;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@OnlyIn(Dist.CLIENT)
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @ModifyExpressionValue(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getModel(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Lnet/minecraft/client/resources/model/BakedModel;"
            )
    )
    private BakedModel useEmptyFeastModelBeforeLighting(BakedModel original, LivingEntity entity, Level level,
                                                        ItemStack stack, int x, int y, int seed, int zOffset) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
        if (kH_1_21_1_neoforge$emptyModelId(stack) != null && (feast == null || feast.ingredients().isEmpty())) {
            Lighting.setupForFlatItems();
        }
        return WrappingBagGuiModelResolver.resolve(stack, original);
    }

    @Unique
    private static ResourceLocation kH_1_21_1_neoforge$emptyModelId(ItemStack stack) {
        if (stack.is(KHItems.WOODEN_PLATE)) {
            return KaleidoscopeHodgepodge.id("wooden_plate_empty");
        }
        if (stack.is(KHItems.PORCELAIN_PLATE)) {
            return KaleidoscopeHodgepodge.id("porcelain_plate_empty");
        }
        if (stack.is(KHItems.MEDIAN_PORCELAIN_PLATE)) {
            return KaleidoscopeHodgepodge.id("median_porcelain_plate_empty");
        }
        if (stack.is(KHItems.LARGE_PORCELAIN_PLATE)) {
            return KaleidoscopeHodgepodge.id("large_porcelain_plate_empty");
        }
        if (stack.is(KHItems.PORCELAIN_SOUP_BOWL)) {
            return KaleidoscopeHodgepodge.id(stack.has(KHDataComponents.SOUP_BASE)
                    ? "porcelain_soup_bowl_empty_with_soup"
                    : "porcelain_soup_bowl_empty_without_soup");
        }
        return null;
    }
}
