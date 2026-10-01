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
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class HodgepodgeRecipeBlockEntityRenderer implements BlockEntityRenderer<HodgepodgeRecipeBlockEntity, HodgepodgeRecipeRenderState> {
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
            if (!(item instanceof CustomFeastBlockItem))
                return;
            ItemStack target = new ItemStack(item);
            CustomFeastData feast = decoded.feast();
            target.set(KHDataComponents.CUSTOM_FEAST, feast);
            // GUI context resolves the dish as its flat inventory icon. The recipe paper
            // deliberately displays that icon instead of the container's 3D block model.
            resolver.updateForTopItem(state.targetItem, target, ItemDisplayContext.GUI,
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
        poseStack.mulPose(Axis.YP.rotationDegrees(horizontalAngle(state.facing) + (state.attachFace == AttachFace.CEILING ? 180.0F : 0.0F)));
        poseStack.mulPose(Axis.XP.rotationDegrees(switch (state.attachFace) {
            case FLOOR -> 0.0F;
            case WALL -> -90.0F;
            case CEILING -> 180.0F;
        }));
        // The raised recipe holder occupies z=2..7; place the flat icon on its free half.
        poseStack.translate(0.0F, -0.49F, 3.0F / 16.0F);
        // GUI item models use the opposite vertical axis from the recipe paper.
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.55F, 0.35F, 0.001F);
        state.targetItem.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static float horizontalAngle(Direction facing) {
        return switch (facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}
