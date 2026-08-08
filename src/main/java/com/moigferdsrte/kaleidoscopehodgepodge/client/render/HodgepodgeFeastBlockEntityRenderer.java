package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class HodgepodgeFeastBlockEntityRenderer
        implements BlockEntityRenderer<HodgepodgeFeastBlockEntity, HodgepodgeFeastRenderState> {
    private final ItemModelResolver itemModelResolver;

    public HodgepodgeFeastBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public @NonNull HodgepodgeFeastRenderState createRenderState() {
        return new HodgepodgeFeastRenderState();
    }

    @Override
    public void extractRenderState(@NonNull HodgepodgeFeastBlockEntity entity, @NonNull HodgepodgeFeastRenderState state,
                                   float tickProgress, @NonNull Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, tickProgress, cameraPos, overlay);
        if (state.contentRevision == entity.contentRevision()) return;
        List<PlacedIngredient> placements = entity.renderIngredients();
        state.placements = placements;
        if (state.models.length != placements.size()) {
            state.models = new ItemStackRenderState[placements.size()];
        }
        int seed = (int) entity.getBlockPos().asLong();
        for (int i = 0; i < placements.size(); i++) {
            ItemStackRenderState model = state.models[i];
            if (model == null) model = new ItemStackRenderState();
            else model.clear();
            itemModelResolver.updateForTopItem(model, IngredientModelService.createDisplay(placements.get(i).id()),
                    ItemDisplayContext.NONE, entity.getLevel(), null, seed + i);
            state.models[i] = model;
        }
        state.contentRevision = entity.contentRevision();
    }

    @Override
    public void submit(HodgepodgeFeastRenderState state, @NonNull PoseStack poses,
                       @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState camera) {
        for (int i = 0; i < state.models.length; i++) {
            PlacedIngredient placement = state.placements.get(i);
            poses.pushPose();
            poses.translate(placement.x() / 16.0, placement.y() / 16.0 + 0.5, placement.z() / 16.0);
            state.models[i].submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poses.popPose();
        }
    }
}
