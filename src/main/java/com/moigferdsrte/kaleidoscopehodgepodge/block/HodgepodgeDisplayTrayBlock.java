package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HodgepodgeDisplayTrayBlock extends AbstractHodgepodgeFeastBlock {
    public HodgepodgeDisplayTrayBlock(Properties properties) {
        super(properties, CustomFeastData.ContainerKind.DISH);
    }

    public VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.0625, 0.25, 0.0625, 0.9375, 0.375, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.375, 0.125, 0.375, 0.625, 0.25, 0.625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.25, 0, 0.25, 0.75, 0.125, 0.75), BooleanOp.OR);

        return shape;
    }

    @Override
    protected VoxelShape getContainerShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        // 射线命中必须落在托盘实际的 6px 顶面，否则 x/z 投影会沿用木盘的 2px 底面。
        return makeShape();
    }

    @Override
    public VoxelShape containerOutlineShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return makeShape();
    }
}
