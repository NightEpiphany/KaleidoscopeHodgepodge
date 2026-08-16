package com.moigferdsrte.kaleidoscopehodgepodge.client.model;

import com.mojang.serialization.MapCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class WrappingBagItemModel implements ItemModel {
    private static final Identifier EMPTY_MODEL = KaleidoscopeHodgepodge.id("item/wrapping_bag");
    private static final Identifier FULL_MODEL = KaleidoscopeHodgepodge.id("item/wrapping_bag_full");

    private final ItemModel emptyModel;
    private final ItemModel fullModel;

    private WrappingBagItemModel(ItemModel emptyModel, ItemModel fullModel) {
        this.emptyModel = emptyModel;
        this.fullModel = fullModel;
    }

    @Override
    public void update(@NonNull ItemStackRenderState output, @NonNull ItemStack bag,
                       @NonNull ItemModelResolver resolver, @NonNull ItemDisplayContext displayContext,
                       @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        var contents = PackingBagService.get(bag);
        if (contents.isEmpty()) {
            emptyModel.update(output, bag, resolver, displayContext, level, owner, seed);
            return;
        }
        if (displayContext == ItemDisplayContext.GUI && Minecraft.getInstance().hasShiftDown()) {
            ItemStack display = IngredientModelService.createDisplay(contents.first().orElseThrow().id());
            if (!display.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_MODEL, "").isEmpty()) {
                resolver.updateForTopItem(output, display, displayContext, level, owner, seed);
                return;
            }
        }
        fullModel.update(output, bag, resolver, displayContext, level, owner, seed);
    }

    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public @NonNull MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public @NonNull ItemModel bake(ItemModel.@NonNull BakingContext context, @NonNull Matrix4fc transformation) {
            return new WrappingBagItemModel(model(EMPTY_MODEL).bake(context, transformation),
                    model(FULL_MODEL).bake(context, transformation));
        }

        @Override
        public void resolveDependencies(ResolvableModel.@NonNull Resolver resolver) {
            model(EMPTY_MODEL).resolveDependencies(resolver);
            model(FULL_MODEL).resolveDependencies(resolver);
        }

        private static CuboidItemModelWrapper.Unbaked model(Identifier id) {
            return new CuboidItemModelWrapper.Unbaked(id, Optional.empty(), List.of());
        }
    }
}
