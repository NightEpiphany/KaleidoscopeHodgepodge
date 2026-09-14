package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.client.render.renderstate.HodgepodgeRecipeRenderState;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.util.CrashDiagnostics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeRecipeBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.FeastCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class HodgepodgeRecipeBlockEntityRenderer implements BlockEntityRenderer<HodgepodgeRecipeBlockEntity, HodgepodgeRecipeRenderState> {
    private static final float BASE_MODEL_X = 90.0F;
    private static final float BASE_MODEL_Y = 270.0F;
    private static final float BASE_ITEM_Y = -90.0F;

    private final ItemModelResolver resolver;

    public HodgepodgeRecipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        resolver = context.itemModelResolver();
    }

    @Override
    public @NonNull HodgepodgeRecipeRenderState createRenderState() {
        return new HodgepodgeRecipeRenderState();
    }

    @Override
    public void extractRenderState(@NonNull HodgepodgeRecipeBlockEntity entity,
            @NonNull HodgepodgeRecipeRenderState state, float tickProgress, @NonNull Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, tickProgress, cameraPos, overlay);
        state.facing = entity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        state.attachFace = entity.getBlockState().getValue(BlockStateProperties.ATTACH_FACE);
        String code = entity.recipe() == null ? "" : entity.recipe().feastCode();
        if (code.equals(state.code))
            return;
        state.code = code;
        state.valid = false;
        if (code.isEmpty())
            return;
        try {
            FeastCodec.Decoded decoded = FeastCodec.decode(code);
            Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(decoded.containerPath())).orElse(null);
            if (item == null || item == Items.AIR)
                return;
            ItemStack target = new ItemStack(item);
            CustomFeastData feast = decoded.feast();
            target.set(KHDataComponents.CUSTOM_FEAST, feast);
            resolver.updateForTopItem(state.targetItem, target, ItemDisplayContext.FIXED,
                    entity.getLevel(), null, (int) entity.getBlockPos().asLong());
            state.valid = true;
            state.isSoup = feast.kind() == CustomFeastData.ContainerKind.SOUP;
            state.dishSize = state.isSoup ? 1 : target.is(KHItems.LARGE_PORCELAIN_PLATE) ? 9 : target.is(KHItems.MEDIAN_PORCELAIN_PLATE) ? 2 : 1;
        } catch (FeastCodec.FormatException | RuntimeException e) {
            CrashDiagnostics.record(e.toString());
        }
    }

    @Override
    public void submit(@NonNull HodgepodgeRecipeRenderState state, @NonNull PoseStack poseStack,
            @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState camera) {
        if (!state.valid) return;
        poseStack.pushPose();

        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(relativeModelRotation(state.attachFace, state.facing));
        if (state.facing.getAxis() == Direction.Axis.Z) {
            if (state.attachFace == AttachFace.FLOOR || state.attachFace == AttachFace.CEILING) {
                poseStack.mulPose(Axis.XN.rotationDegrees(180.0F));
                poseStack.translate(-0.3126F, -0.05F, 0.0F);
            }
            else if (state.attachFace == AttachFace.WALL)
                poseStack.translate(-0.6423F, -0.05F, 0.0F);
        } else if (state.facing.getAxis() == Direction.Axis.X) {
            if (state.attachFace == AttachFace.FLOOR || state.attachFace == AttachFace.CEILING) {
                poseStack.mulPose(Axis.XN.rotationDegrees(0.0F));
                poseStack.translate(-0.3126F, -0.05F, 0.0F);
            }
            else if (state.attachFace == AttachFace.WALL)
                poseStack.translate(-0.3423F, -0.05F, 0.0F);
        }
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        poseStack.translate(0.5f, 0.805f, 0.75f);
        poseStack.mulPose(Axis.YP.rotationDegrees(BASE_ITEM_Y));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.scale(0.25f, 0.25f, 0.25f);
        poseStack.translate(1, 1.25, 0);
        if (state.dishSize == 1) {
            poseStack.scale(1.525f, 1.525f, 1.525f);
            poseStack.translate(0.0F, 0.0765F, 0.0F);
        }
        else if (state.dishSize == 2) {
            poseStack.scale(1.425f, 1.425f, 1.425f);
            poseStack.translate(0.0F, 0.085F, 0.0F);
        }
        state.targetItem.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static Quaternionf relativeModelRotation(AttachFace face, Direction facing) {
        float modelX;
        float modelY = switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
        switch (face) {
            case FLOOR -> modelX = 0.0F;
            case WALL -> modelX = 90.0F;
            case CEILING -> {
                modelX = 180.0F;
                modelY += 180.0F;
            }
            default -> throw new IllegalStateException("Unsupported recipe block face: " + face);
        }

        Quaternionf base = new Quaternionf()
                .rotateY((float) Math.toRadians(BASE_MODEL_Y))
                .rotateX((float) Math.toRadians(BASE_MODEL_X));
        Quaternionf target = new Quaternionf()
                .rotateY((float) Math.toRadians(modelY))
                .rotateX((float) Math.toRadians(modelX));
        return target.mul(base.invert());
    }
}
