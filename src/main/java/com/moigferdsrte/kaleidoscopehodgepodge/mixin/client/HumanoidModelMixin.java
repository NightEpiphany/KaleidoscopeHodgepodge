package com.moigferdsrte.kaleidoscopehodgepodge.mixin.client;

import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {

    @Shadow
    @Final
    public ModelPart rightArm;

    @Shadow
    @Final
    public ModelPart leftArm;

    @Inject(method = "poseRightArm", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeHodgepodge$poseRightArm(T entity, CallbackInfo ci) {
        ItemStack held = entity.getMainArm() == HumanoidArm.RIGHT
                ? entity.getMainHandItem() : entity.getOffhandItem();
        if (held.getItem() instanceof CustomFeastBlockItem item) {
            rightArm.xRot = item.isSpecial ? -Mth.PI : -Mth.PI * 0.5f;
            rightArm.zRot = -Mth.PI * 0.015f;
            ci.cancel();
        }
    }

    @Inject(method = "poseLeftArm", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeHodgepodge$poseLeftArm(T entity, CallbackInfo ci) {
        ItemStack held = entity.getMainArm() == HumanoidArm.LEFT
                ? entity.getMainHandItem() : entity.getOffhandItem();
        if (held.getItem() instanceof CustomFeastBlockItem) {
            leftArm.xRot = -Mth.PI * 0.5f;
            leftArm.zRot = Mth.PI * 0.015f;
            ci.cancel();
        }
    }
}
