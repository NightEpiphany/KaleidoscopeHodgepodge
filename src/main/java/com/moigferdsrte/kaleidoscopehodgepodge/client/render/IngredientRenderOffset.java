package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;

/** 渲染偏移量修正，以防止模型重叠产生错乱感 */
@Environment(EnvType.CLIENT)
public final class IngredientRenderOffset {
    private static final float BASE_BLOCK_UNITS = 0.00001F / 16.0F;

    public static Vector3f forPlacement(BlockPos blockPos, PlacedIngredient ingredient, int index) {
        long seed = mix(blockPos.asLong() ^ ingredient.id().hashCode());
        seed = mix(seed ^ ((long) ingredient.x() << 32) ^ ((long) ingredient.z() << 16)
                ^ ingredient.y() ^ index * 0x9E3779B9L);
        return axialOffset(seed);
    }

    public static Vector3f forItem(PlacedIngredient ingredient, int index) {
        long seed = mix(ingredient.id().hashCode() * 0x9E3779B97F4A7C15L
                ^ ((long) ingredient.x() << 32) ^ ((long) ingredient.y() << 16)
                ^ ingredient.z() ^ index * 0xC2B2AE3D27D4EB4FL);
        return axialOffset(seed);
    }

    private static Vector3f axialOffset(long seed) {
        float magnitude = BASE_BLOCK_UNITS * (0.75F + ((seed >>> 8) & 0xFFL) / 510.0F);
        return switch ((int) Long.remainderUnsigned(seed, 6)) {
            case 0 -> new Vector3f(magnitude, 0.0F, 0.0F);
            case 1 -> new Vector3f(-magnitude, 0.0F, 0.0F);
            case 2 -> new Vector3f(0.0F, 0.0F, magnitude);
            case 3 -> new Vector3f(0.0F, 0.0F, -magnitude);
            case 4 -> new Vector3f(0.0F, magnitude, 0.0F);
            default -> new Vector3f(0.0F, -magnitude, 0.0F);
        };
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private IngredientRenderOffset() {
    }
}
