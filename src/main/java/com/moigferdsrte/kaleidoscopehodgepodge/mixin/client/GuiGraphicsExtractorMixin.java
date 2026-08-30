package com.moigferdsrte.kaleidoscopehodgepodge.mixin.client;

import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxService;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    @Unique
    private static final float PREVIEW_SCALE = 0.75F;
    @Unique
    private static final float PREVIEW_OFFSET_X = 6.0F;
    @Unique
    private static final float PREVIEW_OFFSET_Y = 4.0F;

    @Inject(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("TAIL")
    )
    private void kaleidoscopeHodgepodge$renderContainerIngredient(
            @Nullable LivingEntity owner, @Nullable Level level, ItemStack itemStack,
            int x, int y, int seed, CallbackInfo callbackInfo) {
        if (Minecraft.getInstance().hasShiftDown()) {
            return;
        }

        GeneralConfig.Snapshot config = GeneralConfig.snapshot();
        BaggedIngredient selected;
        if (itemStack.is(KHItems.WRAPPING_BAG)) {
            if (!config.wrappingBagIngredientPreview()) return;
            selected = PackingBagService.get(itemStack).first().orElse(null);
        } else if (itemStack.is(KHItems.LUNCH_BOX)) {
            if (!config.lunchBoxIngredientPreview()) return;
            selected = LunchBoxService.selectedIngredient(itemStack);
        } else {
            return;
        }
        if (selected == null) return;

        ItemStack display = IngredientModelService.createDisplay(selected);
        if (display.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_MODEL, "").isEmpty()) return;

        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + PREVIEW_OFFSET_X, y + PREVIEW_OFFSET_Y);
        pose.scale(PREVIEW_SCALE, PREVIEW_SCALE);
        graphics.item(display, 0, 0, seed);
        pose.popMatrix();
    }
}
