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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public @NotNull InteractionResult useWithoutItem(@NotNull BlockState state,
            @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player,
            @NotNull BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty() || player.isSpectator() || !player.mayBuild()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof HodgepodgeRecipeBlockEntity entity && !entity.getItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, entity.getItem().copyWithCount(1));
            level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction,
            @NotNull BlockState neighborState, net.minecraft.world.level.@NotNull LevelAccessor level,
            @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
            @NotNull BlockPos pos, @NotNull CollisionContext context) {
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
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        if (state.getValue(FACE) == AttachFace.FLOOR || state.getValue(FACE) == AttachFace.CEILING)
            state = state.setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        return state.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
            @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HodgepodgeRecipeBlockEntity entity) {
            entity.setItem(stack.copyWithCount(1));
        }
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos,
            @NotNull BlockState state) {
        if (level.getBlockEntity(pos) instanceof HodgepodgeRecipeBlockEntity entity && !entity.getItem().isEmpty()) {
            return entity.getItem().copy();
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
        List<ItemStack> drops = new java.util.ArrayList<>(1);
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
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
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new HodgepodgeRecipeBlockEntity(pos, state);
    }
}
