package com.moigferdsrte.kaleidoscopehodgepodge.client.render.item;

public class MedianPlateItemRenderer extends CustomFeastItemRenderer {
    @Override
    protected float offsetX(int rot) {
        return switch (rot) {
            case 0, 3 -> -1f;
            default -> 0;
        };
    }

    @Override
    protected float offsetY(int rot) {
        return -0.5f;
    }

    @Override
    protected float offsetZ(int rot) {
        return switch (rot) {
            case 0 -> -0.5f;
            case 1 -> -0.445f;
            case 2, 3 -> 0.565f;
            default -> 0;
        };
    }
}
