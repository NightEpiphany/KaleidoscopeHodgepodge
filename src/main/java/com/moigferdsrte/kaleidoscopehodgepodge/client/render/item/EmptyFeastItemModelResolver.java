package com.moigferdsrte.kaleidoscopehodgepodge.client.render.item;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class EmptyFeastItemModelResolver {
    private EmptyFeastItemModelResolver() {
    }

    public static BakedModel resolve(ItemStack stack, BakedModel fallback) {
        if (!(stack.getItem() instanceof CustomFeastBlockItem)) return fallback;
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
        if (feast != null && !feast.ingredients().isEmpty()) return fallback;

        ResourceLocation modelId = emptyModelId(stack);
        if (modelId == null) return fallback;
        return Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.inventory(modelId));
    }

    private static ResourceLocation emptyModelId(ItemStack stack) {
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
