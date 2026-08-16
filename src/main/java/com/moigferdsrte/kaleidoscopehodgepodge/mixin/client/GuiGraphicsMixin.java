package com.moigferdsrte.kaleidoscopehodgepodge.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.EmptyFeastItemModelResolver;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.WrappingBagGuiModelResolver;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @ModifyExpressionValue(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getModel(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Lnet/minecraft/client/resources/model/BakedModel;"
            )
    )
    private BakedModel resolveGuiItemModelBeforeLighting(BakedModel original, LivingEntity entity, Level level,
                                                          ItemStack stack, int x, int y, int seed, int zOffset) {
        BakedModel model = EmptyFeastItemModelResolver.resolve(stack, original);
        return WrappingBagGuiModelResolver.resolve(stack, model);
    }
}
