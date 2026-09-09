package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.mojang.serialization.MapCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeRecipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class HodgepodgeRecipeBlock extends FaceAttachedHorizontalDirectionalBlock
        implements EntityBlock, SimpleWaterloggedBlock {
    public static final MapCodec<HodgepodgeRecipeBlock> CODEC = simpleCodec(HodgepodgeRecipeBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape CEILING_AABB_X = Block.box(1.5D, 15.75D, 3.0D, 14.5D, 16.0D, 13.0D);
    private static final VoxelShape CEILING_AABB_Z = Block.box(3.0D, 15.75D, 1.5D, 13.0D, 16.0D, 14.5D);
    private static final VoxelShape FLOOR_AABB_X = Block.box(1.5D, 0.0D, 3.0D, 14.5D, 0.25D, 13.0D);
    private static final VoxelShape FLOOR_AABB_Z = Block.box(3.0D, 0.0D, 1.5D, 13.0D, 0.25D, 14.5D);
    private static final VoxelShape NORTH_AABB = Block.box(3.0D, 1.5D, 15.75D, 13.0D, 14.5D, 16.0D);
    private static final VoxelShape SOUTH_AABB = Block.box(3.0D, 1.5D, 0.0D, 13.0D, 14.5D, 0.25D);
    private static final VoxelShape WEST_AABB = Block.box(15.75D, 1.5D, 3.0D, 16.0D, 14.5D, 13.0D);
    private static final VoxelShape EAST_AABB = Block.box(0.0D, 1.5D, 3.0D, 0.25D, 14.5D, 13.0D);

    public HodgepodgeRecipeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.WALL).setValue(WATERLOGGED, false));
    }

    @Override
    public @NonNull InteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state,
            @NonNull Level level, @NonNull BlockPos pos, @NonNull Player player,
            @NonNull InteractionHand hand, @NonNull BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof HodgepodgeRecipeBlockEntity entity && !entity.getItem().isEmpty()) {
            player.setItemInHand(hand, entity.getItem().copyWithCount(1));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NonNull BlockState updateShape(@NonNull BlockState state, @NonNull LevelReader level,
            net.minecraft.world.level.@NonNull ScheduledTickAccess ticks, @NonNull BlockPos pos,
            @NonNull Direction direction, @NonNull BlockPos neighborPos, @NonNull BlockState neighborState,
            @NonNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level,
            @NonNull BlockPos pos, @NonNull CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(FACE)) {
            case FLOOR -> facing.getAxis() == Direction.Axis.X ? FLOOR_AABB_X : FLOOR_AABB_Z;
            case WALL -> switch (facing) {
                case EAST -> EAST_AABB;
                case WEST -> WEST_AABB;
                case SOUTH -> SOUTH_AABB;
                case NORTH, UP, DOWN -> NORTH_AABB;
            };
            default -> facing.getAxis() == Direction.Axis.X ? CEILING_AABB_X : CEILING_AABB_Z;
        };
    }

    @Override
    protected @NonNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    public @Nullable BlockState getStateForPlacement(@NonNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        if (state.getValue(FACE) == AttachFace.FLOOR || state.getValue(FACE) == AttachFace.CEILING)
            state = state.setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        return state.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public void setPlacedBy(@NonNull Level level, @NonNull BlockPos pos, @NonNull BlockState state,
            @Nullable LivingEntity placer, @NonNull ItemStack stack) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HodgepodgeRecipeBlockEntity entity) {
            entity.setItem(stack.copyWithCount(1));
        }
    }

    @Override
    protected @NonNull ItemStack getCloneItemStack(@NonNull LevelReader level, @NonNull BlockPos pos,
            @NonNull BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof HodgepodgeRecipeBlockEntity entity && !entity.getItem().isEmpty()) {
            return entity.getItem().copy();
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    public @NonNull List<ItemStack> getDrops(@NonNull BlockState state, LootParams.@NonNull Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity entity = params.getParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof HodgepodgeRecipeBlockEntity recipe && !recipe.getItem().isEmpty()) {
            drops.add(recipe.getItem().copyWithCount(1));
        }
        return drops;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, WATERLOGGED);
    }

    @Override
    public @NonNull FluidState getFluidState(@NonNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @NonNull BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new HodgepodgeRecipeBlockEntity(pos, state);
    }
}
