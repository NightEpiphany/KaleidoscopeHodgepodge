package com.moigferdsrte.kaleidoscopehodgepodge.mixin.client;

import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    private static final float PREVIEW_SCALE = 0.5F;
    private static final float PREVIEW_OFFSET = 8.0F;

    @Inject(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("TAIL")
    )
    private void kaleidoscopeHodgepodge$renderWrappingBagIngredient(
            @Nullable LivingEntity owner, @Nullable Level level, ItemStack stack,
            int x, int y, int seed, CallbackInfo callbackInfo) {
        if (!stack.is(KHItems.WRAPPING_BAG)
                || Minecraft.getInstance().hasShiftDown()
                || !GeneralConfig.snapshot().wrappingBagIngredientPreview()) {
            return;
        }

        PackingBagService.get(stack).first().ifPresent(first -> {
            ItemStack display = IngredientModelService.createDisplay(first.id());
            if (display.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_MODEL, "").isEmpty()) return;

            GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(x + PREVIEW_OFFSET, y + PREVIEW_OFFSET);
            pose.scale(PREVIEW_SCALE, PREVIEW_SCALE);
            graphics.item(display, 0, 0, seed);
            pose.popMatrix();
        });
    }
}
