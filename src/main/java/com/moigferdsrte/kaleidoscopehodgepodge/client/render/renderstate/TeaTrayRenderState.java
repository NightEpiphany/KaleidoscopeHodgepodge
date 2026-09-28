package com.moigferdsrte.kaleidoscopehodgepodge.client.render.renderstate;

import com.moigferdsrte.kaleidoscopehodgepodge.client.render.TeaCupTransform;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TeaTrayLayout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

@Environment(EnvType.CLIENT)
public final class TeaTrayRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState[] cups = new ItemStackRenderState[TeaTrayLayout.CAPACITY];
    public final TeaCupTransform[] transforms = new TeaCupTransform[TeaTrayLayout.CAPACITY];
    public Direction facing = Direction.NORTH;

    public TeaTrayRenderState() {
        for (int slot = 0; slot < cups.length; slot++) cups[slot] = new ItemStackRenderState();
    }
}
