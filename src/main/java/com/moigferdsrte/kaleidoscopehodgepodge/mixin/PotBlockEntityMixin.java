package com.moigferdsrte.kaleidoscopehodgepodge.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.IPot;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CookwarePackingService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotBlockEntity.class)
public abstract class PotBlockEntityMixin {
    @Inject(method = "takeOutProduct", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeHodgepodge$packFinishedDish(Level level, LivingEntity user, ItemStack stack,
                                                         CallbackInfoReturnable<Boolean> cir) {
        PotBlockEntity pot = (PotBlockEntity) (Object) this;
        if (pot.getStatus() != IPot.FINISHED) return;
        if (!CookwarePackingService.tryPack(level, user, stack, pot.getResult())) return;
        if (!level.isClientSide()) pot.reset();
        cir.setReturnValue(true);
    }
}
