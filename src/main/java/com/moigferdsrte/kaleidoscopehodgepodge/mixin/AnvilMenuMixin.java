package com.moigferdsrte.kaleidoscopehodgepodge.mixin;

import com.moigferdsrte.kaleidoscopehodgepodge.core.DishName;
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @Inject(method = "createResult", at = @At("RETURN"))
    private void hodgepodge$syncDishName(CallbackInfo callback) {
        ItemStack result = ((AnvilMenu) (Object) this).getSlot(AnvilMenu.RESULT_SLOT).getItem();
        if (!result.isEmpty() && result.getItem() instanceof CustomFeastBlockItem) {
            DishName.set(result, Optional.ofNullable(result.get(DataComponents.CUSTOM_NAME)));
        }
    }
}
