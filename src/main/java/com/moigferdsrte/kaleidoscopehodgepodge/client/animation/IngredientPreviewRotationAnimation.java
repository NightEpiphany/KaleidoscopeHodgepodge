package com.moigferdsrte.kaleidoscopehodgepodge.client.animation;

import com.moigferdsrte.kaleidoscopehodgepodge.api.ICustomAnimation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** 顺时针旋转模型的平滑动画 */
@Environment(EnvType.CLIENT)
public final class IngredientPreviewRotationAnimation implements ICustomAnimation {
    public static final long DURATION_MILLIS = 240L;
    private static final float FULL_TURN = 360.0F;
    private static final float EPSILON = 0.001F;

    private BlockPos blockPos;
    private Identifier ingredientId;
    private int contentRevision = Integer.MIN_VALUE;
    private float startAngle;
    private float targetAngle;
    private long startedAt;
    private boolean active;
    private boolean visible;
    private boolean visibleLastFrame;

    public void beginFrame() {
        visibleLastFrame = visible;
        visible = false;
    }


    public void update(BlockPos pos, Identifier id, int revision, int rotation, long nowNanos) {
        update(pos, id, revision, rotation, nowNanos, true);
    }

    public void update(BlockPos pos, Identifier id, int revision, int rotation,
                       long nowNanos, boolean animate) {
        float requested = -90.0F * Math.floorMod(rotation, 4);
        if (!animate) {
            blockPos = pos;
            ingredientId = id;
            contentRevision = revision;
            startAngle = targetAngle = requested;
            startedAt = nowNanos;
            active = true;
            visible = true;
            return;
        }
        boolean newPreview = !active || !visibleLastFrame || blockPos == null || !blockPos.equals(pos)
                || ingredientId == null || !ingredientId.equals(id) || contentRevision != revision;
        if (newPreview) {
            blockPos = pos;
            ingredientId = id;
            contentRevision = revision;
            startAngle = requested;
            targetAngle = requested;
            startedAt = nowNanos;
            active = true;
            visible = true;
            return;
        }

        visible = true;

        float current = sample(nowNanos);
        float equivalentTarget = targetAngle;
        float requestedDelta = clockwiseDistance(equivalentTarget, requested);
        if (requestedDelta > EPSILON) {
            equivalentTarget -= requestedDelta;
            startAngle = current;
            targetAngle = equivalentTarget;
            startedAt = nowNanos;
        }
    }

    public float sample(long nowNanos) {
        if (!active) return 0.0F;
        long elapsedNanos = Math.max(0L, nowNanos - startedAt);
        if (elapsedNanos >= DURATION_MILLIS * 1_000_000L) return targetAngle;
        float progress = elapsedNanos / (float) (DURATION_MILLIS * 1_000_000L);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        return startAngle + (targetAngle - startAngle) * eased;
    }

    private static float clockwiseDistance(float current, float requested) {
        float distance = (current - requested) % FULL_TURN;
        if (distance < 0.0F) distance += FULL_TURN;
        return distance;
    }
}
