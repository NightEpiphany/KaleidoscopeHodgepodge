package com.moigferdsrte.kaleidoscopehodgepodge.client.render.renderstate;

import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Vector3f;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class HodgepodgeFeastRenderState extends BlockEntityRenderState {
    public List<PlacedIngredient> placements = List.of();
    public ItemStackRenderState[] models = new ItemStackRenderState[0];
    public Vector3f[] microOffsets = new Vector3f[0];
    public boolean microOffsetMode;
    public int contentRevision = -1;
    public int placementAnimationRevision = -1;
    public int placementAnimationIndex = -1;
    public long placementAnimationStartedAt;
}
