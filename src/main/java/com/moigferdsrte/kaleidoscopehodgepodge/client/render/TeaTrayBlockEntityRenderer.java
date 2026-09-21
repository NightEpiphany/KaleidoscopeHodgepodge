package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.block.TeaTrayBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.TeaTrayBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.client.render.renderstate.TeaTrayRenderState;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TeaTrayLayout;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TrayTeacup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@Environment(EnvType.CLIENT)
public final class TeaTrayBlockEntityRenderer implements BlockEntityRenderer<TeaTrayBlockEntity, TeaTrayRenderState> {
    private static final Set<String> CUP_MODELS = Set.of("barley_tea", "biluochun", "butter_tea",
            "flower_tea", "mystery_tea", "oolong", "sakura_fubuki", "tieguanyin", "empty_cup");
    private final ItemModelResolver resolver;

    public TeaTrayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        resolver = context.itemModelResolver();
    }

    @Override
    public @NonNull TeaTrayRenderState createRenderState() {
        return new TeaTrayRenderState();
    }

    @Override
    public void extractRenderState(@NonNull TeaTrayBlockEntity entity, @NonNull TeaTrayRenderState state,
            float tickProgress, @NonNull Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, tickProgress, cameraPos, overlay);
        state.facing = entity.getBlockState().getValue(TeaTrayBlock.FACING);
        for (ItemStackRenderState cup : state.cups) cup.clear();
        for (TrayTeacup cup : entity.cups()) {
            ItemStack display = cup.tea();
            Identifier id = BuiltInRegistries.ITEM.getKey(display.getItem());
            if (id.getNamespace().equals("kaleidoscope_cookery") && CUP_MODELS.contains(id.getPath())) {
                display.set(DataComponents.ITEM_MODEL, KaleidoscopeHodgepodge.id("tea_cups/" + id.getPath()));
            }
            ItemStackRenderState model = state.cups[cup.slot()];
            resolver.updateForTopItem(model, display, ItemDisplayContext.NONE, entity.getLevel(), null,
                    (int) entity.getBlockPos().asLong() + cup.slot());
            state.transforms[cup.slot()] = TeaCupTransform.fit(model.getModelBoundingBox());
        }
    }

    @Override
    public void submit(@NonNull TeaTrayRenderState state, @NonNull PoseStack poses,
            @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState camera) {
        poses.pushPose();
        poses.translate(0.5, 0, 0.5);
        poses.rotateDegrees(Axis.YP, switch (state.facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        });
        poses.translate(-0.5, 0, -0.5);
        for (int slot = 0; slot < TeaTrayLayout.CAPACITY; slot++) {
            ItemStackRenderState model = state.cups[slot];
            if (model.isEmpty()) continue;
            TeaCupTransform transform = state.transforms[slot];
            poses.pushPose();
            poses.translate(TeaTrayLayout.centerX(slot), TeaTrayLayout.SURFACE_Y, TeaTrayLayout.centerZ(slot));
            poses.scale(transform.scaleX(), 1.0F, transform.scaleZ());
            poses.translate(-transform.centerX(), -transform.bottomY(), -transform.centerZ());
            model.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poses.popPose();
        }
        poses.popPose();
    }
}
