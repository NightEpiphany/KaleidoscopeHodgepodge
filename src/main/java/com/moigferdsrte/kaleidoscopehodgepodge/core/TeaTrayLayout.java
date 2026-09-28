package com.moigferdsrte.kaleidoscopehodgepodge.core;

import net.minecraft.core.Direction;

public final class TeaTrayLayout {
    public static final int CAPACITY = 4;
    public static final double CUP_WIDTH = 5.0 / 16.0;
    public static final double SURFACE_Y = 1.0 / 16.0;

    public static double centerX(int slot) {
        return (4.5 + (slot % 2) * 7.0) / 16.0;
    }

    public static double centerZ(int slot) {
        return (4.5 + (slot / 2) * 7.0) / 16.0;
    }

    public static int slotAt(double blockX, double blockZ, Direction facing) {
        // 坐标视角映射投影
        double localX = switch (facing) {
            case EAST -> blockZ;
            case SOUTH -> 1.0 - blockX;
            case WEST -> 1.0 - blockZ;
            default -> blockX;
        };
        double localZ = switch (facing) {
            case EAST -> 1.0 - blockX;
            case SOUTH -> 1.0 - blockZ;
            case WEST -> blockX;
            default -> blockZ;
        };
        return (localX >= 0.5 ? 1 : 0) + (localZ >= 0.5 ? 2 : 0);
    }

    private TeaTrayLayout() {}
}
