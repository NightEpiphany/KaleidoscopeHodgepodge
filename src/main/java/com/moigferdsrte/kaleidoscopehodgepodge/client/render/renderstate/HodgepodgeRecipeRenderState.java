package com.moigferdsrte.kaleidoscopehodgepodge.client.render.renderstate;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.core.Direction;

@Environment(EnvType.CLIENT)
public final class HodgepodgeRecipeRenderState extends BlockEntityRenderState {
    public String code = "";
    public ItemStackRenderState targetItem = new ItemStackRenderState();
    public Direction facing = Direction.NORTH;
    public AttachFace attachFace = AttachFace.WALL;
    public boolean valid;
    public boolean isSoup;
    public int dishSize;
}
