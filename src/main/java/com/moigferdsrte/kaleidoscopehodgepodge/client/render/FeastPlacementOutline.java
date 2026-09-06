package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.api.IHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.*;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Optional;

/** 渲染宴席方块的默认、容器和材料放置轮廓。 */
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
        LevelRenderEvents.COLLECT_SUBMITS.register(FeastPlacementOutline::renderPreview);
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(FeastPlacementOutline::renderOutline);
    }

    private static boolean renderOutline(LevelRenderContext context, BlockOutlineRenderState outline) {
        Minecraft minecraft = Minecraft.getInstance();
        Optional<FeastTarget> feastTarget = resolveFeastTarget(minecraft, outline);
        if (feastTarget.isEmpty()) return true;

        VertexConsumer consumer = minecraft.renderBuffers().bufferSource().getBuffer(OUTLINE);
        Vec3 camera = context.levelState().cameraRenderState.pos;
        Optional<PlacementPreview> preview = resolvePlacementPreview(minecraft, feastTarget.orElseThrow());
        if (preview.isEmpty()) {
            renderShape(context, consumer, outline.shape(), outline, camera,
                    DEFAULT_COLOR, DEFAULT_LINE_WIDTH);
            return false;
        }

        PlacementPreview resolved = preview.orElseThrow();
        HodgepodgeFeastBlockEntity.ContainerLimits limits = resolved.limits();
        PlacementSpace.Bounds bounds = resolved.bounds();
        var containerShape = Shapes.box(bounds.minX() / 16.0, limits.baseHeight() / 16.0,
                bounds.minZ() / 16.0, bounds.maxX() / 16.0, bounds.maxHeight() / 16.0,
                bounds.maxZ() / 16.0);
        renderShape(context, consumer, containerShape, outline, camera,
                CONTAINER_COLOR, CONTAINER_LINE_WIDTH);

        resolved.placement().ifPresent(placement -> renderShape(context, consumer,
                IngredientHitTest.localShape(placement), outline, camera,
                PLACEMENT_COLOR, PLACEMENT_LINE_WIDTH));
        return false;
    }

    private static void renderShape(LevelRenderContext context, VertexConsumer consumer, VoxelShape shape,
                                    BlockOutlineRenderState outline, Vec3 camera, int color, float lineWidth) {
        ShapeRenderer.renderShape(context.poseStack(), consumer, shape,
                outline.pos().getX() - camera.x,
                outline.pos().getY() - camera.y,
                outline.pos().getZ() - camera.z,
                color, lineWidth);
    }

    private static void renderPreview(LevelRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        float alpha = (float) GeneralConfig.snapshot().placementPreviewAlpha();
        if (alpha <= 0.0F) return;
        resolveFeastTarget(minecraft, context.levelState().blockOutlineRenderState)
                .flatMap(target -> resolvePlacementPreview(minecraft, target))
                .filter(preview -> preview.placement().isPresent())
                .ifPresent(preview -> submitPreview(context, minecraft, preview,
                        preview.placement().orElseThrow(), alpha));
    }

    private static void submitPreview(LevelRenderContext context, Minecraft minecraft,
                                      PlacementPreview preview, PlacedIngredient placement, float alpha) {
        if (minecraft.level == null) return;
        PREVIEW_MODEL.clear();
        minecraft.getItemModelResolver().updateForTopItem(PREVIEW_MODEL,
                IngredientModelService.createDisplay(preview.baggedIngredient().id()), ItemDisplayContext.NONE,
                minecraft.level, minecraft.player, (int) preview.origin().asLong());
        Vec3 camera = context.levelState().cameraRenderState.pos;
        context.poseStack().pushPose();
        try {
            context.poseStack().translate(
                    preview.origin().getX() - camera.x + placement.x() / 16.0,
                    preview.origin().getY() - camera.y + placement.y() / 16.0 + 0.5,
                    preview.origin().getZ() - camera.z + placement.z() / 16.0);
            context.poseStack().mulPose(Axis.YP.rotationDegrees(-90.0F * placement.rotation()));
            TranslucentItemPreviewRenderer.submit(PREVIEW_MODEL, context.poseStack(), context.submitNodeCollector(),
                    getLightCoords(minecraft.level, preview.origin()), OverlayTexture.NO_OVERLAY, alpha);
        } finally {
            context.poseStack().popPose();
        }
    }

    private static Optional<FeastTarget> resolveFeastTarget(Minecraft minecraft,
                                                            BlockOutlineRenderState outline) {
        if (outline == null || minecraft.level == null || minecraft.player == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !hit.getBlockPos().equals(outline.pos())
                || !(minecraft.level.getBlockEntity(outline.pos()) instanceof HodgepodgeFeastBlockEntity feast)
                || !(minecraft.level.getBlockState(outline.pos()).getBlock() instanceof IHodgepodge surface)) {
            return Optional.empty();
        }
        return Optional.of(new FeastTarget(outline.pos(), hit, feast, surface));
    }

    private static Optional<PlacementPreview> resolvePlacementPreview(Minecraft minecraft, FeastTarget target) {
        if (minecraft.level == null || minecraft.player == null) return Optional.empty();
        ItemStack bag = heldFilledBag(minecraft);
        BaggedIngredient baggedIngredient = bag == null ? null : PackingBagService.get(bag).first().orElse(null);
        PackingIngredients ingredient = baggedIngredient == null ? null
                : PackingIngredientRegistry.byId(baggedIngredient.id()).orElse(null);
        if (ingredient == null || !isSuitable(ingredient, target.feast().kind())) return Optional.empty();

        HodgepodgeFeastBlockEntity.ContainerLimits limits = target.feast().limits();
        PlacementSpace.Bounds bounds = target.feast().placementBounds();
        List<PlacedIngredient> existing = target.surface().placementIngredients(minecraft.level, target.origin(),
                minecraft.level.getBlockState(target.origin()));
        Optional<PlacedIngredient> placement = IngredientPlacementTarget.resolve(existing, target.origin(),
                        minecraft.player.getEyePosition(), target.hit(), ingredient, baggedIngredient.rotation())
                .flatMap(resolved -> PlacementSpace.place(existing, ingredient, resolved.x(), resolved.z(),
                        limits.capacity(), limits.baseHeight(), bounds, baggedIngredient.rotation()).placement());
        return Optional.of(new PlacementPreview(target.origin(), baggedIngredient, limits, bounds, placement));
    }

    public static int getLightCoords(final BlockAndLightGetter level, final BlockPos pos) {
        return getLightCoords(BrightnessGetter.DEFAULT, level, level.getBlockState(pos), pos);
    }

    public static int getLightCoords(final BrightnessGetter brightnessGetter, final BlockAndLightGetter level, final BlockState state, final BlockPos pos) {
        if (state.emissiveRendering(level, pos)) {
            return 15728880;
        } else {
            int packedBrightness = brightnessGetter.packedBrightness(level, pos);
            int block = LightCoordsUtil.block(packedBrightness);
            int blockSelfEmission = state.getLightEmission();
            return block < blockSelfEmission ? LightCoordsUtil.withBlock(packedBrightness, blockSelfEmission) : packedBrightness;
        }
    }

    private static ItemStack heldFilledBag(Minecraft minecraft) {
        for (InteractionHand hand : InteractionHand.values()) {
            assert minecraft.player != null;
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (stack.is(KHItems.WRAPPING_BAG)
                    && PackingBagService.getMode(stack) == PackingBagMode.PLACEMENT
                    && !PackingBagService.get(stack).isEmpty()) return stack;
        }
        return null;
    }

    private static boolean isSuitable(PackingIngredients ingredient, CustomFeastData.ContainerKind kind) {
        return ingredient.suitableFor() == PackingIngredients.SuitableFor.BOTH
                || kind == CustomFeastData.ContainerKind.DISH && ingredient.suitableFor() == PackingIngredients.SuitableFor.DISH
                || kind == CustomFeastData.ContainerKind.SOUP && ingredient.suitableFor() == PackingIngredients.SuitableFor.SOUP;
    }

    private record FeastTarget(BlockPos origin, BlockHitResult hit, HodgepodgeFeastBlockEntity feast,
                               IHodgepodge surface) {}

    private record PlacementPreview(BlockPos origin, BaggedIngredient baggedIngredient,
                                    HodgepodgeFeastBlockEntity.ContainerLimits limits,
                                    PlacementSpace.Bounds bounds, Optional<PlacedIngredient> placement) {}

    private FeastPlacementOutline() {}


    @FunctionalInterface
    public interface BrightnessGetter {
        BrightnessGetter DEFAULT = (level, pos) -> {
            int sky = level.getBrightness(LightLayer.SKY, pos);
            int block = level.getBrightness(LightLayer.BLOCK, pos);
            return LightCoordsUtil.pack(block, sky);
        };

        int packedBrightness(BlockAndLightGetter level, BlockPos pos);
    }
}
