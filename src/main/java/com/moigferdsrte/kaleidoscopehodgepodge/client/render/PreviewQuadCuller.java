package com.moigferdsrte.kaleidoscopehodgepodge.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/*模型culling处理*/
@Environment(EnvType.CLIENT)
final class PreviewQuadCuller {
    private static final float GEOMETRY_EPSILON = 1.0E-4F;
    private static final float PROJECTION_EPSILON = 1.0E-5F;
    private static final float[] SAMPLE_POINTS = {0.02F, 0.25F, 0.5F, 0.75F, 0.98F};

    static List<BakedQuad> cull(List<BakedQuad> quads, Matrix4fc pose, Matrix4fc viewRotation) {
        if (quads.size() < 2) return quads;

        Matrix4f modelView = new Matrix4f(viewRotation).mul(pose);
        List<ProjectedQuad> projected = new ArrayList<>(quads.size());
        boolean hasEligibleQuad = false;
        for (BakedQuad quad : quads) {
            ProjectedQuad view = ProjectedQuad.create(quad, modelView);
            projected.add(view);
            hasEligibleQuad |= view.eligible();
        }
        if (!hasEligibleQuad) return quads;

        BitSet hidden = new BitSet(quads.size());
        for (int candidateIndex = 0; candidateIndex < projected.size(); candidateIndex++) {
            ProjectedQuad candidate = projected.get(candidateIndex);
            if (!candidate.eligible()) continue;
            if (fullyCovered(candidateIndex, candidate, projected)) hidden.set(candidateIndex);
        }
        if (hidden.isEmpty()) return quads;

        List<BakedQuad> visible = new ArrayList<>(quads.size() - hidden.cardinality());
        for (int index = 0; index < quads.size(); index++) {
            if (!hidden.get(index)) visible.add(quads.get(index));
        }
        return visible;
    }

    private static boolean fullyCovered(int candidateIndex, ProjectedQuad candidate,
                                        List<ProjectedQuad> projected) {
        for (float u : SAMPLE_POINTS) {
            for (float v : SAMPLE_POINTS) {
                ScreenPoint sample = candidate.sample(u, v);
                if (!coveredAt(candidateIndex, candidate.nearDepth(), sample, projected)) return false;
            }
        }
        return true;
    }

    private static boolean coveredAt(int candidateIndex, float candidateDepth, ScreenPoint sample,
                                     List<ProjectedQuad> projected) {
        for (int coverIndex = 0; coverIndex < projected.size(); coverIndex++) {
            if (coverIndex == candidateIndex) continue;
            ProjectedQuad cover = projected.get(coverIndex);
            if (!cover.eligible() || !cover.frontFacing()
                    || cover.farDepth() >= candidateDepth - GEOMETRY_EPSILON) {
                continue;
            }
            if (cover.contains(sample)) return true;
        }
        return false;
    }

    private record ScreenPoint(float x, float y) {
    }

    private record ProjectedQuad(ScreenPoint[] vertices, float nearDepth, float farDepth,
                                 boolean eligible, boolean frontFacing) {
        private static ProjectedQuad create(BakedQuad quad, Matrix4fc modelView) {
            if (quad.materialInfo().layer() != ChunkSectionLayer.SOLID || !isAxisAligned(quad)) {
                return ineligible();
            }

            ScreenPoint[] screen = new ScreenPoint[4];
            Vector3f centroid = new Vector3f();
            float nearDepth = Float.POSITIVE_INFINITY;
            float farDepth = Float.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3f transformed = new Vector3f(quad.position(vertex));
                modelView.transformPosition(transformed);
                float depth = -transformed.z;
                if (depth <= GEOMETRY_EPSILON) return ineligible();
                screen[vertex] = new ScreenPoint(transformed.x / depth, transformed.y / depth);
                nearDepth = Math.min(nearDepth, depth);
                farDepth = Math.max(farDepth, depth);
                centroid.add(transformed);
            }
            if (Math.abs(signedArea(screen)) <= PROJECTION_EPSILON) return ineligible();

            centroid.mul(0.25F);
            Vector3f normal = new Vector3f(quad.direction().getUnitVec3f());
            modelView.transformDirection(normal).normalize();
            boolean frontFacing = normal.dot(-centroid.x, -centroid.y, -centroid.z) > GEOMETRY_EPSILON;
            return new ProjectedQuad(screen, nearDepth, farDepth, true, frontFacing);
        }

        private static ProjectedQuad ineligible() {
            return new ProjectedQuad(new ScreenPoint[0], 0.0F, 0.0F, false, false);
        }

        private ScreenPoint sample(float u, float v) {
            ScreenPoint top = lerp(vertices[0], vertices[1], u);
            ScreenPoint bottom = lerp(vertices[3], vertices[2], u);
            return lerp(top, bottom, v);
        }

        private boolean contains(ScreenPoint point) {
            float sign = 0.0F;
            for (int index = 0; index < vertices.length; index++) {
                ScreenPoint from = vertices[index];
                ScreenPoint to = vertices[(index + 1) % vertices.length];
                float cross = (to.x - from.x) * (point.y - from.y)
                        - (to.y - from.y) * (point.x - from.x);
                if (Math.abs(cross) <= PROJECTION_EPSILON) continue;
                if (sign == 0.0F) sign = cross;
                else if (cross * sign < -PROJECTION_EPSILON) return false;
            }
            return sign != 0.0F;
        }

        private static boolean isAxisAligned(BakedQuad quad) {
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3fc position = quad.position(vertex);
                minX = Math.min(minX, position.x());
                minY = Math.min(minY, position.y());
                minZ = Math.min(minZ, position.z());
                maxX = Math.max(maxX, position.x());
                maxY = Math.max(maxY, position.y());
                maxZ = Math.max(maxZ, position.z());
            }
            int fixedAxes = 0;
            if (maxX - minX <= GEOMETRY_EPSILON) fixedAxes++;
            if (maxY - minY <= GEOMETRY_EPSILON) fixedAxes++;
            if (maxZ - minZ <= GEOMETRY_EPSILON) fixedAxes++;
            return fixedAxes == 1;
        }

        private static float signedArea(ScreenPoint[] points) {
            float area = 0.0F;
            for (int index = 0; index < points.length; index++) {
                ScreenPoint from = points[index];
                ScreenPoint to = points[(index + 1) % points.length];
                area += from.x * to.y - to.x * from.y;
            }
            return area * 0.5F;
        }

        private static ScreenPoint lerp(ScreenPoint from, ScreenPoint to, float delta) {
            return new ScreenPoint(from.x + (to.x - from.x) * delta,
                    from.y + (to.y - from.y) * delta);
        }
    }

    private PreviewQuadCuller() {
    }
}
