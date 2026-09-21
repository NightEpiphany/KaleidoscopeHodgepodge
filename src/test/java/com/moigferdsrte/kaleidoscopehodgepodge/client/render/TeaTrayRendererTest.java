package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import com.moigferdsrte.kaleidoscopehodgepodge.client.render.renderstate.TeaTrayRenderState;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TeaTrayLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeaTrayRendererTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static TeaTrayBlockEntityRenderer renderer() {
        return new TeaTrayBlockEntityRenderer(new BlockEntityRendererProvider.Context(
                null, null, null, null, null, null, null, null));
    }

    @Test
    void fourIndependentItemSubmissionsHaveCorrectCentersInEveryFacing() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            TeaTrayRenderState state = new TeaTrayRenderState();
            state.facing = facing;
            Vector3fc[] bounds = {
                    new Vector3f(5.5F / 16, 0, 5.5F / 16),
                    new Vector3f(10.5F / 16, 3.0F / 16, 10.5F / 16)
            };
            for (int slot = 0; slot < TeaTrayLayout.CAPACITY; slot++) {
                var layer = state.cups[slot].newLayer();
                layer.setQuads(ItemQuads.EMPTY);
                layer.setExtents(() -> bounds);
                state.transforms[slot] = TeaCupTransform.fit(state.cups[slot].getModelBoundingBox());
            }
            List<Vector3f> centers = new ArrayList<>(TeaTrayLayout.CAPACITY);
            SubmitNodeCollector collector = (SubmitNodeCollector) Proxy.newProxyInstance(
                    SubmitNodeCollector.class.getClassLoader(), new Class<?>[]{SubmitNodeCollector.class},
                    (proxy, method, arguments) -> {
                        assertEquals("submitItem", method.getName());
                        assertInstanceOf(ItemQuads.class, arguments[6]);
                        PoseStack stack = (PoseStack) arguments[0];
                        centers.add(new Vector3f(0.5F, 0, 0.5F).mulPosition(stack.last().pose()));
                        return null;
                    });
            PoseStack poses = new PoseStack();
            renderer().submit(state, poses, collector, new CameraRenderState());
            assertTrue(poses.isEmpty(), "renderer leaked its pose stack");
            assertEquals(4, centers.size());
            for (int slot = 0; slot < centers.size(); slot++) {
                double x = TeaTrayLayout.centerX(slot);
                double z = TeaTrayLayout.centerZ(slot);
                double expectedX = switch (facing) {
                    case EAST -> 1 - z;
                    case SOUTH -> 1 - x;
                    case WEST -> z;
                    default -> x;
                };
                double expectedZ = switch (facing) {
                    case EAST -> x;
                    case SOUTH -> 1 - z;
                    case WEST -> 1 - x;
                    default -> z;
                };
                assertEquals(expectedX, centers.get(slot).x(), 1.0E-6);
                assertEquals(TeaTrayLayout.SURFACE_Y, centers.get(slot).y(), 1.0E-6);
                assertEquals(expectedZ, centers.get(slot).z(), 1.0E-6);
                assertEquals(slot, TeaTrayLayout.slotAt(centers.get(slot).x(), centers.get(slot).z(), facing),
                        "pointing at a rendered cup must select that cup's slot");
            }
        }
    }

    @Test
    void emptySlotsDoNotProduceItemSubmissions() {
        TeaTrayRenderState state = new TeaTrayRenderState();
        PoseStack poses = new PoseStack();
        // An empty tray must not touch the collector at all.
        renderer().submit(state, poses, null, new CameraRenderState());
        assertTrue(poses.isEmpty());
    }
}
