package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MedianPorcelainPlateBlock extends AbstractMultiBlockPlateBlock {
    private static final int PLATE_PART_SIZE = 15;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public MedianPorcelainPlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(PART, Part.LEFT));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos rightPos = context.getClickedPos().relative(facing.getClockWise());
        BlockState placed = super.getStateForPlacement(context);
        return placed != null && canReplace(context, rightPos)
                ? placed.setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(PART, Part.LEFT)
                : null;
    }

    @Override
    protected List<StructurePart> structure(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction right = facing.getClockWise();
        BlockPos leftPos = state.getValue(PART) == Part.LEFT ? pos : pos.relative(right.getOpposite());
        BlockState left = defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(PART, Part.LEFT);
        BlockState rightState = left.setValue(PART, Part.RIGHT);
        return List.of(new StructurePart(leftPos, left, 0, 0),
                new StructurePart(leftPos.relative(right), rightState, 16, 0));
    }

    @Override
    public PlacementSpace.Bounds placementBounds(BlockState state, int maxHeight) {
        boolean left = state.getValue(PART) == Part.LEFT;
        return switch (quarterTurns(state.getValue(BlockStateProperties.HORIZONTAL_FACING))) {
            case 1 -> new PlacementSpace.Bounds(0, 16, left ? 0 : -16, left ? 32 : 16, maxHeight);
            case 2 -> new PlacementSpace.Bounds(left ? -16 : 0, left ? 16 : 32, 0, 16, maxHeight);
            case 3 -> new PlacementSpace.Bounds(0, 16, left ? -16 : 0, left ? 16 : 32, maxHeight);
            default -> new PlacementSpace.Bounds(left ? 0 : -16, left ? 32 : 16, 0, 16, maxHeight);
        };
    }

    @Override
    protected VoxelShape getContainerShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return state.getValue(PART).shape(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }

    @Override
    protected PlacedIngredient toItemCoordinates(StructurePart part, PlacedIngredient ingredient) {
        int turns = quarterTurns(part.state().getValue(BlockStateProperties.HORIZONTAL_FACING));
        Pixel pixel = rotateToNorth(ingredient.x(), ingredient.z(), turns);
        boolean swap = turns % 2 == 1;
        return new PlacedIngredient(ingredient.id(), pixel.x() + part.pixelOffsetX(), ingredient.y(), pixel.z(),
                swap ? ingredient.sizeZ() : ingredient.sizeX(), ingredient.sizeY(),
                swap ? ingredient.sizeX() : ingredient.sizeZ(), ingredient.rotation() - turns, ingredient.food());
    }

    @Override
    protected @Nullable PlacedIngredient toLocalCoordinates(StructurePart part, PlacedIngredient ingredient) {
        int canonicalX = ingredient.x() - part.pixelOffsetX();
        int canonicalZ = ingredient.z();
        if (canonicalX < 0 || canonicalX >= 16 || canonicalZ < 0 || canonicalZ >= 16) return null;
        return fromItemCoordinates(part, ingredient);
    }

    @Override
    protected PlacedIngredient fromItemCoordinates(StructurePart part, PlacedIngredient ingredient) {
        int canonicalX = ingredient.x() - part.pixelOffsetX();
        int canonicalZ = ingredient.z();
        int turns = quarterTurns(part.state().getValue(BlockStateProperties.HORIZONTAL_FACING));
        Pixel pixel = rotateFromNorth(canonicalX, canonicalZ, turns);
        boolean swap = turns % 2 == 1;
        return new PlacedIngredient(ingredient.id(), pixel.x(), ingredient.y(), pixel.z(),
                swap ? ingredient.sizeZ() : ingredient.sizeX(), ingredient.sizeY(),
                swap ? ingredient.sizeX() : ingredient.sizeZ(), ingredient.rotation() + turns, ingredient.food());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.HORIZONTAL_FACING, PART);
    }

    private static int quarterTurns(Direction facing) {
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    private static Pixel rotateToNorth(int x, int z, int turns) {
        return switch (turns) {
            case 1 -> new Pixel(z, PLATE_PART_SIZE - x);
            case 2 -> new Pixel(PLATE_PART_SIZE - x, PLATE_PART_SIZE - z);
            case 3 -> new Pixel(PLATE_PART_SIZE - z, x);
            default -> new Pixel(x, z);
        };
    }

    private static Pixel rotateFromNorth(int x, int z, int turns) {
        return switch (turns) {
            case 1 -> new Pixel(PLATE_PART_SIZE - z, x);
            case 2 -> new Pixel(PLATE_PART_SIZE - x, PLATE_PART_SIZE - z);
            case 3 -> new Pixel(z, PLATE_PART_SIZE - x);
            default -> new Pixel(x, z);
        };
    }

    private record Pixel(int x, int z) {}

    public enum Part implements StringRepresentable {
        LEFT("left"),
        RIGHT("right");

        private final String name;

        private final VoxelShape shape = Block.box(0, 0, 0, 16, 2, 16);

        Part(String name) {
            this.name = name;
        }

        public VoxelShape shape(Direction direction) {
            switch (direction) {
                case WEST -> {
                    return this == Part.LEFT ? Block.box(1, 0, 0, 15, 2, 15)
                            : Block.box(1, 0, 1, 15, 2, 16);
                }
                case EAST -> {
                    return this == Part.LEFT ? Block.box(1, 0, 1, 15, 2, 16)
                            : Block.box(1, 0, 0, 15, 2, 15);
                }
                case NORTH -> {
                    return this == Part.LEFT ? Block.box(1, 0, 1, 16, 2, 15)
                            : Block.box(0, 0, 1, 15, 2, 15);
                }
                case SOUTH -> {
                    return this == Part.LEFT ? Block.box(0, 0, 1, 15, 2, 15)
                            : Block.box(1, 0, 1, 16, 2, 15);
                }
            }
            return shape;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
