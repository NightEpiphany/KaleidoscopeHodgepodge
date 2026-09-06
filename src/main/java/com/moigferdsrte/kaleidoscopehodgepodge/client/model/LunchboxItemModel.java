package com.moigferdsrte.kaleidoscopehodgepodge.client.model;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.mojang.serialization.MapCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.item.LunchBoxItem;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static java.awt.SystemColor.menu;

@Environment(EnvType.CLIENT)
public class LunchboxItemModel implements ItemModel {
    private static final Identifier CLOSED_MODEL = KaleidoscopeHodgepodge.id("item/lunch_box");
    private static final Identifier OPENED_MODEL = KaleidoscopeHodgepodge.id("item/lunch_box_open");

    private final ItemModel closedModel;
    private final ItemModel openedModel;

    public LunchboxItemModel(ItemModel closedModel, ItemModel openedModel) {
        this.closedModel = closedModel;
        this.openedModel = openedModel;
    }

    @Override
    public void update(
            @NonNull ItemStackRenderState output,
            @NonNull ItemStack item,
            @NonNull ItemModelResolver resolver,
            @NonNull ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner, int seed
    ) {
        boolean opened = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.containerMenu instanceof LunchBoxMenu;
        (opened ? openedModel : closedModel).update(output, item, resolver, displayContext, level, owner, seed);
    }

    public record Unbaked() implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new LunchboxItemModel.Unbaked());

        @Override
        public @NonNull MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public @NonNull ItemModel bake(@NonNull BakingContext context, @NonNull Matrix4fc transformation) {
            return new LunchboxItemModel(model(CLOSED_MODEL).bake(context, transformation),
                    model(OPENED_MODEL).bake(context, transformation));
        }

        @Override
        public void resolveDependencies(@NonNull Resolver resolver) {
            model(CLOSED_MODEL).resolveDependencies(resolver);
            model(OPENED_MODEL).resolveDependencies(resolver);
        }

        private static CuboidItemModelWrapper.Unbaked model(Identifier id) {
            return new CuboidItemModelWrapper.Unbaked(id, Optional.empty(),
                    List.of(new Dye(LunchBoxItem.DEFAULT_COLOR)));
        }
    }
}
