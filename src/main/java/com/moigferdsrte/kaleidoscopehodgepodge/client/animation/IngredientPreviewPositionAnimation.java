package com.moigferdsrte.kaleidoscopehodgepodge.client.animation;

import com.moigferdsrte.kaleidoscopehodgepodge.api.ICustomAnimation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** 客户端专用放置预览的平滑像素坐标移动。 */
@Environment(EnvType.CLIENT)
public final class IngredientPreviewPositionAnimation implements ICustomAnimation {
    public static final long DURATION_MILLIS = 60L;

    private BlockPos blockPos;
    private Identifier ingredientId;
    private int contentRevision = Integer.MIN_VALUE;
    private float startX;
    private float startY;
    private float startZ;
    private float targetX;
    private float targetY;
    private float targetZ;
    private float sampleX;
    private float sampleY;
    private float sampleZ;
    private long startedAt;
    private boolean active;
    private boolean visible;
    private boolean visibleLastFrame;

    /** 标记新的轮廓渲染通道的开始。 */
    public void beginFrame() {
        visibleLastFrame = visible;
        visible = false;
    }

    /** 在不为每帧渲染分配向量的情况下以像素更新目标。 */
    public void update(BlockPos pos, Identifier id, int revision,
                       int x, int y, int z, long nowNanos) {
        update(pos, id, revision, x, y, z, nowNanos, true);
    }

    public void update(BlockPos pos, Identifier id, int revision,
                       int x, int y, int z, long nowNanos, boolean animate) {
        if (!animate) {
            blockPos = pos;
            ingredientId = id;
            contentRevision = revision;
            startX = targetX = sampleX = x;
            startY = targetY = sampleY = y;
            startZ = targetZ = sampleZ = z;
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
            startX = targetX = sampleX = x;
            startY = targetY = sampleY = y;
            startZ = targetZ = sampleZ = z;
            startedAt = nowNanos;
            active = true;
            visible = true;
            return;
        }

        visible = true;
        sample(nowNanos);
        if (targetX == x && targetY == y && targetZ == z) return;
        startX = sampleX;
        startY = sampleY;
        startZ = sampleZ;
        targetX = x;
        targetY = y;
        targetZ = z;
        startedAt = nowNanos;
    }

    public void sample(long nowNanos) {
        if (!active) return;
        long elapsedNanos = Math.max(0L, nowNanos - startedAt);
        if (elapsedNanos >= DURATION_MILLIS * 1_000_000L) {
            sampleX = targetX;
            sampleY = targetY;
            sampleZ = targetZ;
            return;
        }
        float progress = elapsedNanos / (float) (DURATION_MILLIS * 1_000_000L);
        float eased = progress * progress * (3.0F - 2.0F * progress);
        sampleX = startX + (targetX - startX) * eased;
        sampleY = startY + (targetY - startY) * eased;
        sampleZ = startZ + (targetZ - startZ) * eased;
    }

    public float x() {
        return sampleX;
    }

    public float y() {
        return sampleY;
    }

    public float z() {
        return sampleZ;
    }
}
