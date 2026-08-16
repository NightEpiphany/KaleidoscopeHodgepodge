package com.moigferdsrte.kaleidoscopehodgepodge.client.render.item;

import com.moigferdsrte.kaleidoscopehodgepodge.client.render.IngredientRenderOffset;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/** 1.21.1 dynamic renderer for ingredient previews and filled feast containers. */
public abstract class CustomFeastItemRenderer extends BlockEntityWithoutLevelRenderer {

    protected CustomFeastItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    protected abstract float offsetX(int rot);
    protected abstract float offsetY(int rot);
    protected abstract float offsetZ(int rot);

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext mode, PoseStack poses,
                             MultiBufferSource consumers, int light, int overlay) {
        int renderLight = mode == ItemDisplayContext.GUI ? LightTexture.FULL_BRIGHT : light;
        if (stack.is(KHItems.INGREDIENT_DISPLAY.get())) {
            String model = stack.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_MODEL.get(), "");
            renderModel(stack, ingredientModel(model), mode, poses, consumers, renderLight, overlay);
            return;
        }

        ResourceLocation containerModel = containerModel(stack, mode);
        BakedModel baseModel = getModel(containerModel);
        boolean leftHand = mode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || mode == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        applyDisplayTransformAroundCenter(baseModel, mode, leftHand, poses);
        renderModel(stack, baseModel, ItemDisplayContext.NONE, poses, consumers, renderLight, overlay);
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST.get());
        if (feast == null) return;
        int index = 0;
        for (PlacedIngredient ingredient : feast.ingredients()) {
            poses.pushPose();
            var offset = GeneralConfig.snapshot().modelMicroOffset()
                    ? IngredientRenderOffset.forItem(ingredient, index) : new Vector3f();
            poses.translate(ingredient.x() / 16.0 + offset.x() + offsetX(ingredient.rotation()),
                    (ingredient.y() + 8.0) / 16.0 + offset.y() + offsetY(ingredient.rotation()),
                    ingredient.z() / 16.0 + offset.z() + offsetZ(ingredient.rotation()));
            poses.mulPose(Axis.YP.rotationDegrees(-90.0F * ingredient.rotation()));
            ItemStack display = com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService
                    .createDisplay(ingredient.id());
            renderModel(display, getModel(ingredientModel(ingredient.id())),
                    ItemDisplayContext.NONE, poses, consumers, renderLight, overlay);
            poses.popPose();
            index++;
        }
    }

    protected ResourceLocation containerModel(ItemStack stack, ItemDisplayContext mode) {
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        boolean gui = mode == ItemDisplayContext.GUI;
        if (stack.is(KHItems.PORCELAIN_SOUP_BOWL.get())) {
            boolean soupBase = stack.has(KHDataComponents.SOUP_BASE.get());
            if (stack.has(KHDataComponents.CUSTOM_FEAST.get()) || !gui) {
                return KaleidoscopeHodgepodge.id("block/porcelain_soup_bowl_"
                        + (soupBase ? "with_soup" : "without_soup"));
            }
            return KaleidoscopeHodgepodge.id("item/porcelain_soup_bowl_empty_"
                    + (soupBase ? "with_soup" : "without_soup"));
        }
        if (gui && !stack.has(KHDataComponents.CUSTOM_FEAST.get())) {
            return KaleidoscopeHodgepodge.id("item/" + name + "_empty");
        }
        if (stack.is(KHItems.MEDIAN_PORCELAIN_PLATE.get()) || stack.is(KHItems.LARGE_PORCELAIN_PLATE.get())) {
            return KaleidoscopeHodgepodge.id("item/" + name + "_base");
        }
        return KaleidoscopeHodgepodge.id("block/" + name);
    }

    protected static ResourceLocation ingredientModel(String path) {
        if (path == null || path.isBlank()) return KaleidoscopeHodgepodge.id("item/wrapping_bag");
        return KaleidoscopeHodgepodge.id("item/" + path);
    }

    protected static ResourceLocation ingredientModel(ResourceLocation id) {
        return com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry.byId(id)
                .map(value -> ingredientModel(value.getResourceLoc()))
                .orElseGet(() -> ingredientModel(""));
    }

    protected void renderModel(ItemStack stack, ResourceLocation id, ItemDisplayContext mode, PoseStack poses,
                             MultiBufferSource consumers, int light, int overlay) {
        renderModel(stack, getModel(id), mode, poses, consumers, light, overlay);
    }

    protected void renderModel(ItemStack stack, BakedModel model, ItemDisplayContext mode, PoseStack poses,
                             MultiBufferSource consumers, int light, int overlay) {
        poses.pushPose();
        // ItemRenderer.render always centers its model with a -0.5 translation.
        // Cancel that nested translation so the outer item transform is applied once.
        poses.translate(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer()
                .render(stack, mode, false, poses, consumers, light, overlay, model);
        poses.popPose();
    }

    /** ItemRenderer applies display transforms before its fixed -0.5 model centering. */
    protected static void applyDisplayTransformAroundCenter(BakedModel model, ItemDisplayContext mode,
                                                            boolean leftHand, PoseStack poses) {
        poses.translate(0.5F, 0.5F, 0.5F);
        model.getTransforms().getTransform(mode).apply(leftHand, poses);
        poses.translate(-0.5F, -0.5F, -0.5F);
    }

    protected static BakedModel getModel(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(id));
    }
}
