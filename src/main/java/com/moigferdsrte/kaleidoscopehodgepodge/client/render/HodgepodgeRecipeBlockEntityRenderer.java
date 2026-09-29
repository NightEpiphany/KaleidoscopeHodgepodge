package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeRecipeBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.FeastCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.Map;
import java.util.WeakHashMap;

public final class HodgepodgeRecipeBlockEntityRenderer implements BlockEntityRenderer<HodgepodgeRecipeBlockEntity> {
    private final Map<HodgepodgeRecipeBlockEntity, Preview> previews = new WeakHashMap<>(16);

    public HodgepodgeRecipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(HodgepodgeRecipeBlockEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        if (entity.recipe() == null) return;
        String code = entity.recipe().feastCode();
        Preview preview = previews.get(entity);
        if (preview == null || !preview.code().equals(code)) {
            preview = decode(code);
            previews.put(entity, preview);
        }
        if (preview.stack().isEmpty()) return;
        var state = entity.getBlockState();
        var face = state.getValue(BlockStateProperties.ATTACH_FACE);
        float angle = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> -90;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
        poses.pushPose();
        poses.translate(0.5, 0.5, 0.5);
        poses.mulPose(Axis.YP.rotationDegrees(angle + (face == AttachFace.CEILING ? 180 : 0)));
        poses.mulPose(Axis.XP.rotationDegrees(switch (face) {
            case FLOOR -> 0;
            case WALL -> -90;
            case CEILING -> 180;
        }));
        // Match the blockstate transform, then print on the free half of the paper.
        // The raised recipe holder occupies z=2..7; the image belongs at z=11.
        poses.translate(0, -0.49, 3.0 / 16.0);
        // GUI item models use the opposite vertical axis from the recipe paper.
        poses.mulPose(Axis.XP.rotationDegrees(-90));
        poses.mulPose(Axis.ZP.rotationDegrees(180));
        poses.scale(0.55F, 0.35F, 0.001F);
        Minecraft.getInstance().getItemRenderer().renderStatic(preview.stack(), ItemDisplayContext.GUI,
                light, overlay, poses, buffers, entity.getLevel(), 0);
        poses.popPose();
    }

    private static Preview decode(String code) {
        try {
            var decoded = FeastCodec.decode(code);
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(decoded.containerPath()));
            if (item instanceof CustomFeastBlockItem) {
                ItemStack stack = new ItemStack(item);
                stack.set(KHDataComponents.CUSTOM_FEAST.get(), decoded.feast());
                return new Preview(code, stack);
            }
        } catch (FeastCodec.FormatException | RuntimeException ignored) {
        }
        return new Preview(code, ItemStack.EMPTY);
    }

    private record Preview(String code, ItemStack stack) {}
}
