package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.core.TeaTrayLayout;
import net.minecraft.world.phys.AABB;

public record TeaCupTransform(double centerX, double bottomY, double centerZ, float scaleX, float scaleZ) {
    public static TeaCupTransform fit(AABB bounds) {
        return new TeaCupTransform((bounds.minX + bounds.maxX) * 0.5, bounds.minY,
                (bounds.minZ + bounds.maxZ) * 0.5, scale(bounds.getXsize()), scale(bounds.getZsize()));
    }

    private static float scale(double extent) {
        return extent > 1.0E-6 ? (float) (TeaTrayLayout.CUP_WIDTH / extent) : 1.0F;
    }
}
