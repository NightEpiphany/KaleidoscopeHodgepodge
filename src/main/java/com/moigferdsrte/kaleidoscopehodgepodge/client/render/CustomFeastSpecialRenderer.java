package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class CustomFeastSpecialRenderer implements SpecialModelRenderer<CustomFeastSpecialRenderer.RenderData> {
    // Pixel corrections from centered nested-item space to the special model's block-model space.
    private static final double ITEM_TRANSLATION_X_PIXELS = 8.0;
    private static final double ITEM_TRANSLATION_Y_PIXELS = 8.0;
    private static final double ITEM_TRANSLATION_Z_PIXELS = 8.0;

    private final boolean asymmetry;

    public CustomFeastSpecialRenderer(boolean asymmetry) {
        this.asymmetry = asymmetry;
    }

    @Override
    public void submit(@Nullable RenderData data, @NonNull PoseStack poses, @NonNull SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int outlineColor) {
        if (data == null) return;
        ItemModelResolver resolver = Minecraft.getInstance().getItemModelResolver();
        int seed = 1;
        for (PlacedIngredient placement : data.ingredients()) {
            ItemStackRenderState model = new ItemStackRenderState();
            resolver.updateForTopItem(model, IngredientModelService.createDisplay(placement.id()),
                    ItemDisplayContext.NONE, null, null, seed++);
            poses.pushPose();
            var offset = GeneralConfig.snapshot().modelMicroOffset()
                    ? IngredientRenderOffset.forItem(placement, seed)
                    : new Vector3f();
            poses.translate((placement.x() - 8 + ITEM_TRANSLATION_X_PIXELS) / 16.0 + offset.x() + normalizeX(placement.rotation()),
                    (placement.y() + ITEM_TRANSLATION_Y_PIXELS) / 16.0 + offset.y() + normalizeY(placement.rotation()),
                    (placement.z() - 8 + ITEM_TRANSLATION_Z_PIXELS) / 16.0 + offset.z() + normalizeZ(placement.rotation()));
            poses.mulPose(Axis.YP.rotationDegrees(-90.0F * placement.rotation()));
            model.submit(poses, collector, light, OverlayTexture.NO_OVERLAY, outlineColor);
            poses.popPose();
        }
    }

    private float normalizeX(int rot) {
        if (!this.asymmetry || rot > 3) return 0;
        return switch (rot) {
            case 0 -> -0.44f;
            case 1, 2, 3 -> -0.42f;
            default -> 0;
        };
    }

    @SuppressWarnings("unused")
    private float normalizeY(int rot) {
        return 0;
    }

    @SuppressWarnings("unused")
    private float normalizeZ(int rot) {
        return 0;
    }

    @Override
    public void getExtents(@NonNull Consumer<Vector3fc> output) {
        for (float x : new float[]{-2.5F, 2.5F}) {
            for (float y : new float[]{-0.5F, 3.0F}) {
                for (float z : new float[]{-2.5F, 2.5F}) {
                    output.accept(new Vector3f(x, y, z));
                }
            }
        }
    }

    @Override
    public @Nullable RenderData extractArgument(ItemStack stack) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
        if (feast == null) return null;
        return new RenderData(feast.ingredients());
    }

    public record RenderData(java.util.List<PlacedIngredient> ingredients) {}

    public record Unbaked(boolean asymmetry) implements SpecialModelRenderer.Unbaked<RenderData> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked(false));
        public static final MapCodec<Unbaked> MAP_CODEC_ASYMMETRY = MapCodec.unit(new Unbaked(true));

        @Override public CustomFeastSpecialRenderer bake(SpecialModelRenderer.@NonNull BakingContext context) {
            return new CustomFeastSpecialRenderer(this.asymmetry);
        }
        @Override public @NonNull MapCodec<? extends SpecialModelRenderer.Unbaked<RenderData>> type() { return this.asymmetry ? MAP_CODEC_ASYMMETRY : MAP_CODEC; }
    }
}
