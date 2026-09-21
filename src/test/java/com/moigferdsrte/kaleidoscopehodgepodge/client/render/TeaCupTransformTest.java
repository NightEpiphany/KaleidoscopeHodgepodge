package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.core.TeaTrayLayout;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeaCupTransformTest {
    @Test
    void slotsLeaveTwoPixelGapsAndOnePixelTrayMargins() {
        assertEquals(2.0 / 16, TeaTrayLayout.centerX(1) - TeaTrayLayout.centerX(0) - TeaTrayLayout.CUP_WIDTH);
        assertEquals(2.0 / 16, TeaTrayLayout.centerZ(2) - TeaTrayLayout.centerZ(0) - TeaTrayLayout.CUP_WIDTH);
        assertEquals(1.0 / 16, TeaTrayLayout.centerX(0) - TeaTrayLayout.CUP_WIDTH / 2 - 1.0 / 16);
        assertEquals(1.0 / 16, 15.0 / 16 - TeaTrayLayout.centerX(3) - TeaTrayLayout.CUP_WIDTH / 2);
    }

    @Test
    void asymmetricTallCupFitsFivePixelFootprintWithoutRestrictingHeight() {
        AABB bounds = new AABB(-0.2, -0.5, -0.3, 0.15, 1.4, 0.1);
        TeaCupTransform transform = TeaCupTransform.fit(bounds);
        assertEquals(TeaTrayLayout.CUP_WIDTH, bounds.getXsize() * transform.scaleX(), 1.0E-6);
        assertEquals(TeaTrayLayout.CUP_WIDTH, bounds.getZsize() * transform.scaleZ(), 1.0E-6);
        assertEquals(-0.025, transform.centerX(), 1.0E-6);
        assertEquals(-0.1, transform.centerZ(), 1.0E-6);
        assertEquals(TeaTrayLayout.SURFACE_Y, bounds.minY - transform.bottomY() + TeaTrayLayout.SURFACE_Y);
        assertEquals(bounds.getYsize() + TeaTrayLayout.SURFACE_Y,
                bounds.maxY - transform.bottomY() + TeaTrayLayout.SURFACE_Y);
    }

    @Test
    void emptyModelBoundsDoNotProduceInfiniteScale() {
        TeaCupTransform transform = TeaCupTransform.fit(new AABB(0, 0, 0, 0, 0, 0));
        assertTrue(Float.isFinite(transform.scaleX()));
        assertTrue(Float.isFinite(transform.scaleZ()));
    }
}
