package com.moigferdsrte.kaleidoscopehodgepodge.client.render.item;

public class PlateItemRenderer extends CustomFeastItemRenderer {

    @Override
    protected float offsetX(int rot) {
        if (rot == 1 || rot == 2) return  0.5f;
        return -0.5f;
    }

    @Override
    protected float offsetY(int rot) {
        return -0.5f;
    }

    @Override
    protected float offsetZ(int rot) {
        if (rot == 2 || rot == 3) return 0.5f;
        return -0.5f;
    }
}
