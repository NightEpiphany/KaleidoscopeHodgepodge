package com.moigferdsrte.kaleidoscopehodgepodge.core;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/*材料模型在碰撞体积上的高度分布图*/
public final class IngredientCollisionHeightMap {
    private static final int REGION_SIZE = 8;
    private static final int REGION_COUNT = 2;

    public static VoxelShape fromIngredients(List<PlacedIngredient> ingredients) {
        int[] heights = new int[REGION_COUNT * REGION_COUNT];
        for (PlacedIngredient ingredient : ingredients) {
            int top = ingredient.y() + ingredient.sizeY();
            if (top <= 0) continue;
            for (int z = 0; z < REGION_COUNT; z++) {
                int minZ = z * REGION_SIZE * 2;
                int maxZ = minZ + REGION_SIZE * 2;
                if (ingredient.zMax() <= minZ || ingredient.zMin() >= maxZ) continue;
                for (int x = 0; x < REGION_COUNT; x++) {
                    int minX = x * REGION_SIZE * 2;
                    int maxX = minX + REGION_SIZE * 2;
                    if (ingredient.xMax() <= minX || ingredient.xMin() >= maxX) continue;
                    int index = z * REGION_COUNT + x;
                    heights[index] = Math.max(heights[index], top);
                }
            }
        }

        VoxelShape shape = Shapes.empty();
        for (int z = 0; z < REGION_COUNT; z++) {
            for (int x = 0; x < REGION_COUNT; x++) {
                int height = heights[z * REGION_COUNT + x];
                if (height <= 0) continue;
                shape = Shapes.or(shape, Shapes.box(x * 0.5D, 0.0D, z * 0.5D,
                        (x + 1) * 0.5D, height / 16.0D, (z + 1) * 0.5D));
            }
        }
        return shape.optimize();
    }

    private IngredientCollisionHeightMap() {
    }
}
