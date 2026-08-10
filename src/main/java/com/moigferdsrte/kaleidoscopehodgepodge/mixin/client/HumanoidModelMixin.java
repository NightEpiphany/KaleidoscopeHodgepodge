package com.moigferdsrte.kaleidoscopehodgepodge.mixin.client;

import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends HumanoidRenderState> extends EntityModel<T> {

    @Shadow
    @Final
    public ModelPart rightArm;

    @Shadow
    @Final
    public ModelPart leftArm;

    protected HumanoidModelMixin(ModelPart modelPart) {
        super(modelPart);
    }

    @Inject(method = "poseRightArm", at = @At("HEAD"), cancellable = true)
    public void poseRightArm(T state, CallbackInfo ci) {
        if (state.rightHandItemStack.getItem() instanceof CustomFeastBlockItem item) {
            rightArm.xRot = item.isSpecial ? -Mth.PI : -Mth.PI * 0.5f;
            rightArm.zRot = -Mth.PI * 0.015f;
            ci.cancel();
        }
    }

    @Inject(method = "poseLeftArm", at = @At("HEAD"), cancellable = true)
    public void poseLeftArm(T state, CallbackInfo ci) {
        if (state.leftHandItemStack.getItem() instanceof CustomFeastBlockItem) {
            leftArm.xRot = -Mth.PI * 0.5f;
            leftArm.zRot = Mth.PI * 0.015f;
            ci.cancel();
        }
    }
}
