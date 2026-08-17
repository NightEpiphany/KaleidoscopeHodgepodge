package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteOneByTwoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

/** 负责在装袋时无掉落地拆除厨房模组的多方块菜品。 */
public final class FoodBiteStructureService {
    private static final int SILENT_UPDATE = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final ThreadLocal<Integer> DROP_SUPPRESSION_DEPTH = new ThreadLocal<>();

    public static void removePackedDish(Level level, BlockPos clickedPos, BlockState clickedState) {
        if (!(clickedState.getBlock() instanceof FoodBiteOneByTwoBlock)) {
            level.removeBlock(clickedPos, false);
            return;
        }

        List<BlockPos> parts = oneByTwoParts(level, clickedPos, clickedState);
        if (parts.size() != 2) {
            level.removeBlock(clickedPos, false);
            return;
        }

        suppressDrops(() -> {
            List<PartState> snapshots = parts.stream()
                    .map(part -> new PartState(part, level.getBlockState(part)))
                    .toList();
            // 先快照再替换，避免第一半的形态更新使第二半状态丢失。
            for (PartState part : snapshots) {
                BlockState replacement = part.state().getValue(BlockStateProperties.WATERLOGGED)
                        ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                level.setBlock(part.pos(), replacement, SILENT_UPDATE);
            }
            snapshots.forEach(part -> level.updateNeighborsAt(part.pos(), clickedState.getBlock()));
        });
    }

    public static boolean isSuppressingDrops() {
        Integer depth = DROP_SUPPRESSION_DEPTH.get();
        return depth != null && depth > 0;
    }

    private static void suppressDrops(Runnable action) {
        Integer current = DROP_SUPPRESSION_DEPTH.get();
        int previousDepth = current == null ? 0 : current;
        DROP_SUPPRESSION_DEPTH.set(previousDepth + 1);
        try {
            action.run();
        } finally {
            if (previousDepth == 0) DROP_SUPPRESSION_DEPTH.remove();
            else DROP_SUPPRESSION_DEPTH.set(previousDepth);
        }
    }

    private record PartState(BlockPos pos, BlockState state) {
    }

    private static List<BlockPos> oneByTwoParts(Level level, BlockPos clickedPos, BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        int position = state.getValue(FoodBiteOneByTwoBlock.POSITION);
        BlockPos otherPos = position == FoodBiteOneByTwoBlock.LEFT
                ? clickedPos.relative(facing.getCounterClockWise())
                : clickedPos.relative(facing.getClockWise());
        BlockState otherState = level.getBlockState(otherPos);
        int expectedPosition = position == FoodBiteOneByTwoBlock.LEFT
                ? FoodBiteOneByTwoBlock.RIGHT : FoodBiteOneByTwoBlock.LEFT;
        if (!otherState.is(state.getBlock())
                || otherState.getValue(BlockStateProperties.HORIZONTAL_FACING) != facing
                || otherState.getValue(FoodBiteOneByTwoBlock.POSITION) != expectedPosition) {
            return List.of(clickedPos);
        }
        return List.of(clickedPos, otherPos);
    }

    private FoodBiteStructureService() {
    }
}
