package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
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
            poses.translate((placement.x() - 8) / 16.0, placement.y() / 16.0,
                    (placement.z() - 8) / 16.0);
            model.submit(poses, collector, light, OverlayTexture.NO_OVERLAY, outlineColor);
            poses.popPose();
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x : new int[]{-1, 1}) for (int y : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
            output.accept(new Vector3f(x * 0.5F, y * 0.5F, z * 0.5F));
        }
    }

    @Override
    public @Nullable RenderData extractArgument(ItemStack stack) {
        CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
        if (feast == null) return null;
        return new RenderData(feast.ingredients());
    }

    public record RenderData(java.util.List<PlacedIngredient> ingredients) {}

    public record Unbaked() implements SpecialModelRenderer.Unbaked<RenderData> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());
        @Override public CustomFeastSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new CustomFeastSpecialRenderer();
        }
        @Override public MapCodec<? extends SpecialModelRenderer.Unbaked<RenderData>> type() { return MAP_CODEC; }
    }
}
