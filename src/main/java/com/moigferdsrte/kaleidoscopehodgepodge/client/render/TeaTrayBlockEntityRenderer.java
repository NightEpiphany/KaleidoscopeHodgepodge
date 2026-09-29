package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.block.TeaTrayBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.TeaTrayBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TeaTrayLayout;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TrayTeacup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class TeaTrayBlockEntityRenderer implements BlockEntityRenderer<TeaTrayBlockEntity> {
    public static final Set<String> COOKERY_CUPS = Set.of("barley_tea", "biluochun", "butter_tea",
            "flower_tea", "mystery_tea", "oolong", "sakura_fubuki", "tieguanyin", "empty_cup");
    public static final Set<String> CHINESE_CUPS = Set.of("hk_milk_tea", "dianhong_tea");
    private final Map<BakedModel, TeaCupTransform> transforms = new WeakHashMap<>(16);

    public TeaTrayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    public static ResourceLocation cupModel(ResourceLocation item) {
        return ("kaleidoscope_cookery".equals(item.getNamespace()) && COOKERY_CUPS.contains(item.getPath())
                || "kaleidoscope_chinesefood".equals(item.getNamespace()) && CHINESE_CUPS.contains(item.getPath()))
                ? KaleidoscopeHodgepodge.id("block/tea_cups/" + item.getPath()) : null;
    }

    @Override
    public void render(TeaTrayBlockEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        var minecraft = Minecraft.getInstance();
        poses.pushPose();
        poses.translate(0.5, 0, 0.5);
        poses.mulPose(Axis.YP.rotationDegrees(switch (entity.getBlockState().getValue(TeaTrayBlock.FACING)) {
            case EAST -> -90;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        }));
        poses.translate(-0.5, 0, -0.5);
        for (TrayTeacup cup : entity.cups()) {
            ItemStack stack = cup.tea();
            ResourceLocation id = cupModel(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            BakedModel model = id == null ? minecraft.getItemRenderer().getModel(stack, entity.getLevel(), null, 0)
                    : minecraft.getModelManager().getModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(id));
            TeaCupTransform transform = transforms.computeIfAbsent(model, TeaTrayBlockEntityRenderer::measure);
            poses.pushPose();
            poses.translate(TeaTrayLayout.centerX(cup.slot()), TeaTrayLayout.SURFACE_Y, TeaTrayLayout.centerZ(cup.slot()));
            poses.scale(transform.scaleX(), 1, transform.scaleZ());
            // Vanilla ItemRenderer centers model-space vertices at (-0.5, -0.5, -0.5).
            poses.translate(0.5 - transform.centerX(), 0.5 - transform.bottomY(), 0.5 - transform.centerZ());
            minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, poses, buffers, light, overlay, model);
            poses.popPose();
        }
        poses.popPose();
    }

    private static TeaCupTransform measure(BakedModel model) {
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        RandomSource random = RandomSource.create(42);
        for (int face = 0; face <= 6; face++) {
            random.setSeed(42);
            for (var quad : model.getQuads(null, face == 6 ? null : Direction.from3DDataValue(face), random)) {
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    float x = Float.intBitsToFloat(vertices[vertex * stride]);
                    float y = Float.intBitsToFloat(vertices[vertex * stride + 1]);
                    float z = Float.intBitsToFloat(vertices[vertex * stride + 2]);
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                }
            }
        }
        return TeaCupTransform.fit(Double.isFinite(minX)
                ? new AABB(minX, minY, minZ, maxX, maxY, maxZ) : new AABB(0, 0, 0, 1, 1, 1));
    }
}
