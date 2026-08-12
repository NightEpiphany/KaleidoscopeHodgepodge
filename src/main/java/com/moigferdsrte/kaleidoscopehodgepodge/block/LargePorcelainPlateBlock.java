package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public final class LargePorcelainPlateBlock extends AbstractMultiBlockPlateBlock {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public LargePorcelainPlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(PART, Part.CENTER));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos center = context.getClickedPos();
        boolean clear = Arrays.stream(Part.values())
                .filter(part -> part != Part.CENTER)
                .map(part -> center.offset(part.x(), 0, part.z()))
                .allMatch(pos -> canReplace(context, pos));
        BlockState placed = super.getStateForPlacement(context);
        return clear && placed != null ? placed.setValue(PART, Part.CENTER) : null;
    }

    @Override
    protected List<StructurePart> structure(BlockPos pos, BlockState state) {
        Part current = state.getValue(PART);
        BlockPos center = pos.offset(-current.x(), 0, -current.z());
        return Arrays.stream(Part.values())
                .map(part -> new StructurePart(center.offset(part.x(), 0, part.z()),
                        defaultBlockState().setValue(PART, part), part.x() * 16, part.z() * 16))
                .toList();
    }

    @Override
    public PlacementSpace.Bounds placementBounds(BlockState state, int maxHeight) {
        Part part = state.getValue(PART);
        return new PlacementSpace.Bounds(-13 - part.x() * 16, 29 - part.x() * 16,
                -13 - part.z() * 16, 29 - part.z() * 16, maxHeight);
    }

    @Override
    protected VoxelShape getContainerShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return state.getValue(PART).shape();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    public enum Part implements StringRepresentable {
        NORTH_WEST("north_west", -1, -1),
        NORTH("north", 0, -1),
        NORTH_EAST("north_east", 1, -1),
        WEST("west", -1, 0),
        CENTER("center", 0, 0),
        EAST("east", 1, 0),
        SOUTH_WEST("south_west", -1, 1),
        SOUTH("south", 0, 1),
        SOUTH_EAST("south_east", 1, 1);

        private final String name;
        private final int x;
        private final int z;
        private final VoxelShape shape;

        Part(String name, int x, int z) {
            this.name = name;
            this.x = x;
            this.z = z;
            this.shape = Block.box(x < 0 ? 4 : 0, 0, z < 0 ? 4 : 0,
                    x > 0 ? 12 : 16, 2, z > 0 ? 12 : 16);
        }

        public int x() {
            return x;
        }

        public int z() {
            return z;
        }

        public VoxelShape shape() {
            return shape;
        }

        @Override
        public @NonNull String getSerializedName() {
            return name;
        }
    }
}
