package com.moigferdsrte.kaleidoscopehodgepodge.api;

import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public interface IHodgepodge {
    default PlacementSpace.Bounds placementBounds(BlockState state, int maxHeight) {
        return PlacementSpace.Bounds.full(maxHeight);
    }

    default List<PlacedIngredient> placementIngredients(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos)
                instanceof HodgepodgeFeastBlockEntity feast
                ? feast.renderIngredients() : List.of();
    }

    default VoxelShape containerOutlineShape(BlockState state, BlockGetter level, BlockPos pos,
                                             CollisionContext context) {
        return Shapes.empty();
    }
}
