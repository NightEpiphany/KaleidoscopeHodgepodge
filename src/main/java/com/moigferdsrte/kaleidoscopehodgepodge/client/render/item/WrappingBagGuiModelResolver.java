package com.moigferdsrte.kaleidoscopehodgepodge.client.render.item;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class WrappingBagGuiModelResolver {
    private WrappingBagGuiModelResolver() {
    }

    public static BakedModel resolve(ItemStack stack, BakedModel fallback) {
        if (!stack.is(KHItems.WRAPPING_BAG) || !Screen.hasShiftDown()) return fallback;

        return PackingBagService.get(stack).first()
                .flatMap(ingredient -> PackingIngredientRegistry.byId(ingredient.id()))
                .map(ingredient -> ingredientModel(ingredient.getResourceLoc()))
                .orElse(fallback);
    }

    private static BakedModel ingredientModel(String path) {
        ResourceLocation id = KaleidoscopeHodgepodge.id("item/" + path);
        return Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(id));
    }
}
