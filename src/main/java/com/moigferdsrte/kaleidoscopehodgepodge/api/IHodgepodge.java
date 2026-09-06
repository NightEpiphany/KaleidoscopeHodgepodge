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

/*杂烩API元定义接口*/
public interface IHodgepodge {
    default PlacementSpace.Bounds placementBounds(BlockState state, int maxHeight) {
        return PlacementSpace.Bounds.full(maxHeight);
    }

    default boolean allowsBoundaryPlacementProjection() {
        return false;
    }

    default BlockPos recipeControllerPos(BlockPos pos, BlockState state) {
        return pos;
    }

    default BlockPos recipePlacementPos(BlockPos pos, BlockState state, PlacedIngredient target) {
        return pos;
    }

    default List<PlacedIngredient> placementIngredients(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos)
                instanceof HodgepodgeFeastBlockEntity feast
                ? feast.renderIngredients() : List.of();
    }

    /** Converts an item-level recipe target to the local coordinates of this block. */
    default PlacedIngredient recipePlacementTarget(BlockPos pos, BlockState state,
                                                    PlacedIngredient target) {
        return target;
    }

    default VoxelShape containerOutlineShape(BlockState state, BlockGetter level, BlockPos pos,
                                             CollisionContext context) {
        return Shapes.empty();
    }
}
