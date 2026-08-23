package com.moigferdsrte.kaleidoscopehodgepodge.mixin.client;

import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.EmptyFeastItemModelResolver;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @ModifyVariable(
            method = "render",
            at = @At("HEAD"),
            argsOnly = true
    )
    public BakedModel usePlateModel(BakedModel model, ItemStack stack, ItemDisplayContext renderMode, boolean leftHanded, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        boolean bl = renderMode == ItemDisplayContext.GUI;
        return bl ? EmptyFeastItemModelResolver.resolve(stack, model) : model;
    }
}
