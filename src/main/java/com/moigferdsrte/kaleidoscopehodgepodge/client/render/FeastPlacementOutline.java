package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.api.IHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientHitTest;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientPlacementTarget;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingIngredientRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FeastPlacementOutline {
    private static final float[] PLACEMENT_COLOR = {0.0F, 0.0F, 0.0F, 1.0F};
    private static final float[] CONTAINER_COLOR = {0.07F, 0.23F, 0.45F, 1.0F};
    private static final float[] DEFAULT_COLOR = {0.0F, 0.0F, 0.0F, 0.4F};

    public static void register() {
        WorldRenderEvents.BLOCK_OUTLINE.register(FeastPlacementOutline::render);
    }

    private static boolean render(WorldRenderContext context, WorldRenderContext.BlockOutlineContext outline) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = outline.blockPos();
        if (minecraft.level == null || minecraft.player == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !hit.getBlockPos().equals(pos)
                || !(minecraft.level.getBlockEntity(pos) instanceof HodgepodgeFeastBlockEntity feast)
                || !(outline.blockState().getBlock() instanceof IHodgepodge surface)) {
            return true;
        }

        ItemStack bag = heldFilledBag(minecraft);
        BaggedIngredient bagged = bag == null ? null : PackingBagService.get(bag).first().orElse(null);
        PackingIngredients ingredient = bagged == null ? null
                : PackingIngredientRegistry.byId(bagged.id()).orElse(null);
        if (ingredient == null || !isSuitable(ingredient, feast.kind())) {
            drawShape(context, outline, surface.containerOutlineShape(outline.blockState(), minecraft.level, pos,
                    CollisionContext.of(minecraft.player)), DEFAULT_COLOR);
            return false;
        }

        HodgepodgeFeastBlockEntity.ContainerLimits limits = feast.limits();
        PlacementSpace.Bounds bounds = feast.placementBounds();
        var existing = surface.placementIngredients(minecraft.level, pos, outline.blockState());
        drawShape(context, outline, Shapes.box(bounds.minX() / 16.0, limits.baseHeight() / 16.0,
                bounds.minZ() / 16.0, bounds.maxX() / 16.0, bounds.maxHeight() / 16.0,
                bounds.maxZ() / 16.0), CONTAINER_COLOR);
        IngredientPlacementTarget.resolve(existing, pos, minecraft.player.getEyePosition(), hit,
                        ingredient, bagged.rotation())
                .flatMap(target -> PlacementSpace.place(existing, ingredient, target.x(), target.z(),
                        limits.capacity(), limits.baseHeight(), bounds, bagged.rotation()).placement())
                .ifPresent(placement -> drawShape(context, outline,
                        IngredientHitTest.localShape(placement), PLACEMENT_COLOR));
        return false;
    }

    private static void drawShape(WorldRenderContext context, WorldRenderContext.BlockOutlineContext outline,
                                  VoxelShape shape, float[] color) {
        double x = outline.blockPos().getX() - outline.cameraX();
        double y = outline.blockPos().getY() - outline.cameraY();
        double z = outline.blockPos().getZ() - outline.cameraZ();
        shape.toAabbs().forEach(box -> LevelRenderer.renderLineBox(context.matrixStack(),
                outline.vertexConsumer(), box.move(x, y, z), color[0], color[1], color[2], color[3]));
    }

    private static ItemStack heldFilledBag(Minecraft minecraft) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (stack.is(KHItems.WRAPPING_BAG)
                    && PackingBagService.getMode(stack) == PackingBagMode.PLACEMENT
                    && !PackingBagService.get(stack).isEmpty()) return stack;
        }
        return null;
    }

    private static boolean isSuitable(PackingIngredients ingredient, CustomFeastData.ContainerKind kind) {
        return ingredient.suitableFor() == PackingIngredients.SuitableFor.BOTH
                || kind == CustomFeastData.ContainerKind.DISH
                && ingredient.suitableFor() == PackingIngredients.SuitableFor.DISH
                || kind == CustomFeastData.ContainerKind.SOUP
                && ingredient.suitableFor() == PackingIngredients.SuitableFor.SOUP;
    }

    private FeastPlacementOutline() {}
}
