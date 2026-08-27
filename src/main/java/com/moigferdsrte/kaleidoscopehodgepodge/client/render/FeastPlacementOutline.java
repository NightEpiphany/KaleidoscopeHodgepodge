package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.api.IHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientHitTest;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientPlacementTarget;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

/** 使用深蓝容器空间与黑色材料尺寸描边辅助精确放置。 */
@Environment(EnvType.CLIENT)
public final class FeastPlacementOutline {
    private static final ItemStackRenderState PREVIEW_MODEL = new ItemStackRenderState();
    private static final RenderType OUTLINE = RenderTypes.lines();
    private static final int PLACEMENT_COLOR = 0xFF000000;
    private static final float PLACEMENT_LINE_WIDTH = 2.5F;
    private static final int CONTAINER_COLOR = 0xFF123A73;
    private static final float CONTAINER_LINE_WIDTH = 2.0F;
    private static final int DEFAULT_COLOR = 0x66000000;
    private static final float DEFAULT_LINE_WIDTH = 1.0F;

    public static void register() {
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(FeastPlacementOutline::render);
    }

    private static boolean render(LevelRenderContext context, BlockOutlineRenderState outline) {
        Minecraft minecraft = Minecraft.getInstance();
        if (outline == null || minecraft.level == null || minecraft.player == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !(minecraft.level.getBlockEntity(outline.pos()) instanceof HodgepodgeFeastBlockEntity feast)) {
            return true;
        }
        if (!hit.getBlockPos().equals(outline.pos())) return true;
        if (!(minecraft.level.getBlockState(outline.pos()).getBlock() instanceof IHodgepodge surface)) return true;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        context.poseStack().pushPose();
        context.poseStack().translate(outline.pos().getX() - camera.x, outline.pos().getY() - camera.y,
                outline.pos().getZ() - camera.z);
        BaggedIngredient baggedIngredient = heldPlacementIngredient(minecraft);
        PackingIngredients ingredient = baggedIngredient == null ? null
                : PackingIngredientRegistry.byId(baggedIngredient.id()).orElse(null);
        if (ingredient == null || !isSuitable(ingredient, feast.kind())) {
            var state = minecraft.level.getBlockState(outline.pos());
            var containerShape = surface.containerOutlineShape(state, minecraft.level, outline.pos(),
                    CollisionContext.of(minecraft.player));
            context.submitNodeCollector().submitShapeOutline(context.poseStack(), containerShape, OUTLINE,
                    DEFAULT_COLOR, DEFAULT_LINE_WIDTH, outline.isTranslucent());
            context.poseStack().popPose();
            return false;
        }

        HodgepodgeFeastBlockEntity.ContainerLimits limits = feast.limits();
        PlacementSpace.Bounds bounds = feast.placementBounds();
        var existing = surface.placementIngredients(minecraft.level, outline.pos(),
                minecraft.level.getBlockState(outline.pos()));
        var containerShape = Shapes.box(bounds.minX() / 16.0, limits.baseHeight() / 16.0,
                bounds.minZ() / 16.0, bounds.maxX() / 16.0, bounds.maxHeight() / 16.0,
                bounds.maxZ() / 16.0);
        context.submitNodeCollector().submitShapeOutline(context.poseStack(), containerShape, OUTLINE,
                CONTAINER_COLOR, CONTAINER_LINE_WIDTH, outline.isTranslucent());

        IngredientPlacementTarget.resolve(existing, outline.pos(), minecraft.player.getEyePosition(),
                        hit, ingredient, baggedIngredient.rotation(),
                        surface.allowsBoundaryPlacementProjection())
                .ifPresent(target -> PlacementSpace.place(existing, ingredient,
                            target.x(), target.z(),
                            limits.capacity(), limits.baseHeight(), bounds, baggedIngredient.rotation())
                    .placement()
                    .ifPresent(placement -> {
                        var placementShape = IngredientHitTest.localShape(placement);
                        context.submitNodeCollector().submitShapeOutline(context.poseStack(), placementShape,
                                OUTLINE, PLACEMENT_COLOR, PLACEMENT_LINE_WIDTH, outline.isTranslucent());
                        renderPreview(context, minecraft, outline.pos(), baggedIngredient, placement);
                    }));
        context.poseStack().popPose();
        return false;
    }

    private static void renderPreview(LevelRenderContext context, Minecraft minecraft,
                                      BlockPos origin, BaggedIngredient ingredient,
                                      PlacedIngredient placement) {
        float alpha = (float) GeneralConfig.snapshot().placementPreviewAlpha();
        if (alpha <= 0.0F || minecraft.level == null) return;
        PREVIEW_MODEL.clear();
        minecraft.getItemModelResolver().updateForTopItem(PREVIEW_MODEL,
                IngredientModelService.createDisplay(ingredient.id()), ItemDisplayContext.NONE,
                minecraft.level, minecraft.player, 0);
        context.poseStack().pushPose();
        context.poseStack().translate(placement.x() / 16.0, placement.y() / 16.0 + 0.5,
                placement.z() / 16.0);
        context.poseStack().mulPose(Axis.YP.rotationDegrees(-90.0F * placement.rotation()));
        TranslucentItemPreviewRenderer.submit(PREVIEW_MODEL, context.poseStack(), context.submitNodeCollector(),
                LightCoordsUtil.getLightCoords(minecraft.level, origin),
                OverlayTexture.NO_OVERLAY, alpha,
                context.levelState().cameraRenderState.viewRotationMatrix);
        context.poseStack().popPose();
    }

    private static BaggedIngredient heldPlacementIngredient(Minecraft minecraft) {
        for (InteractionHand hand : InteractionHand.values()) {
            assert minecraft.player != null;
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (stack.is(KHItems.WRAPPING_BAG)
                    && PackingBagService.getMode(stack) == PackingBagMode.PLACEMENT
                    && !PackingBagService.get(stack).isEmpty()) {
                return PackingBagService.get(stack).first().orElse(null);
            }
            if (stack.is(KHItems.LUNCH_BOX)
                    && LunchBoxService.getMode(stack) == PackingBagMode.PLACEMENT) {
                BaggedIngredient selected = LunchBoxService.selectedIngredient(stack);
                if (selected != null) return selected;
            }
        }
        return null;
    }

    private static boolean isSuitable(PackingIngredients ingredient, CustomFeastData.ContainerKind kind) {
        return ingredient.suitableFor() == PackingIngredients.SuitableFor.BOTH
                || kind == CustomFeastData.ContainerKind.DISH && ingredient.suitableFor() == PackingIngredients.SuitableFor.DISH
                || kind == CustomFeastData.ContainerKind.SOUP && ingredient.suitableFor() == PackingIngredients.SuitableFor.SOUP;
    }

    private FeastPlacementOutline() {}
}
