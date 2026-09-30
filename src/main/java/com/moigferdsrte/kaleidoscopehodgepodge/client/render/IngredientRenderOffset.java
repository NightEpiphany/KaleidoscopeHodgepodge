package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;

/** Provides a stable sub-pixel offset so coplanar ingredient faces do not z-fight. */
@Environment(EnvType.CLIENT)
public final class IngredientRenderOffset {
    private static final float BASE_BLOCK_UNITS = 0.00001F / 16.0F;

    public static Vector3f forPlacement(BlockPos blockPos, PlacedIngredient ingredient, int index) {
        Vector3f result = new Vector3f();
        writeForPlacement(result, blockPos, ingredient, index);
        return result;
    }

    public static void writeForPlacement(Vector3f result, BlockPos blockPos,
                                         PlacedIngredient ingredient, int index) {
        long seed = mix(blockPos.asLong() ^ ingredient.id().hashCode());
        seed = mix(seed ^ ((long) ingredient.x() << 32) ^ ((long) ingredient.z() << 16)
                ^ ingredient.y() ^ index * 0x9E3779B9L);
        writeAxialOffset(result, seed);
    }

    public static Vector3f forItem(PlacedIngredient ingredient, int index) {
        long seed = mix(ingredient.id().hashCode() * 0x9E3779B97F4A7C15L
                ^ ((long) ingredient.x() << 32) ^ ((long) ingredient.y() << 16)
                ^ ingredient.z() ^ index * 0xC2B2AE3D27D4EB4FL);
        return axialOffset(seed);
    }

    private static Vector3f axialOffset(long seed) {
        Vector3f result = new Vector3f();
        writeAxialOffset(result, seed);
        return result;
    }

    private static void writeAxialOffset(Vector3f result, long seed) {
        float magnitude = BASE_BLOCK_UNITS * (0.75F + ((seed >>> 8) & 0xFFL) / 510.0F);
        result.set(0.0F, 0.0F, 0.0F);
        switch ((int) Long.remainderUnsigned(seed, 6)) {
            case 0 -> result.set(magnitude, 0.0F, 0.0F);
            case 1 -> result.set(-magnitude, 0.0F, 0.0F);
            case 2 -> result.set(0.0F, 0.0F, magnitude);
            case 3 -> result.set(0.0F, 0.0F, -magnitude);
            case 4 -> result.set(0.0F, magnitude, 0.0F);
            default -> result.set(0.0F, -magnitude, 0.0F);
        }
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private IngredientRenderOffset() {
    }
}
