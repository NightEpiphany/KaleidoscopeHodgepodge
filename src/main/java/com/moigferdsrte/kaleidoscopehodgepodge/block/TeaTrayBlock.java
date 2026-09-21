package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.TeaTrayBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TeaTrayLayout;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TrayTeacup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class TeaTrayBlock extends HorizontalDirectionalBlock implements EntityBlock, SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 1, 15);

    public TeaTrayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    @Override
    public @NonNull InteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state,
            @NonNull Level level, @NonNull BlockPos pos, @NonNull Player player,
            @NonNull InteractionHand hand, @NonNull BlockHitResult hit) {
        if (player.isSpectator() || !player.mayBuild()) return InteractionResult.PASS;
        if (stack.isEmpty()) {
            return hand == InteractionHand.MAIN_HAND
                    ? useEmptyHand(state, level, pos, player, hit) : InteractionResult.PASS;
        }
        if (!TeaTrayBlockEntity.accepts(stack)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof TeaTrayBlockEntity tray)) return InteractionResult.PASS;
        int slot = targetedSlot(state, pos, hit);
        if (!tray.canInsert(slot)) return InteractionResult.CONSUME;
        if (!level.isClientSide() && tray.insert(stack, slot)) {
            if (!player.isCreative()) stack.shrink(1);
            level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, @NonNull Level level,
            @NonNull BlockPos pos, @NonNull Player player, @NonNull BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || player.isSpectator() || !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        return useEmptyHand(state, level, pos, player, hit);
    }

    private static int targetedSlot(BlockState state, BlockPos pos, BlockHitResult hit) {
        return TeaTrayLayout.slotAt(hit.getLocation().x - pos.getX(), hit.getLocation().z - pos.getZ(),
                state.getValue(FACING));
    }

    private InteractionResult useEmptyHand(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TeaTrayBlockEntity tray) || tray.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            int slot = targetedSlot(state, pos, hit);
            if (!tray.hasCup(slot)) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, tray.removeAt(slot));
                level.playSound(null, pos, soundType.getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            ItemStack tea = tray.removeFirst();
            if (!TeaTrayBlockEntity.isTea(tea)) {
                returnCup(level, pos, player, tea);
                level.playSound(null, pos, soundType.getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                return InteractionResult.SUCCESS;
            }
            ItemStack remainder = tea.getItem().finishUsingItem(tea, level, player);
            // Creative TeacupItem already gives back its cup and returns the unconsumed tea.
            if (!remainder.isEmpty() && !TeaTrayBlockEntity.isTea(remainder)) {
                returnCup(level, pos, player, remainder);
            }
            level.playSound(null, pos, SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.DRINK, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static void returnCup(Level level, BlockPos pos, Player player, ItemStack remainder) {
        Inventory inventory = player.getInventory();
        // Keep the interacting hand empty so the next cup can be drunk immediately.
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE && !remainder.isEmpty(); slot++) {
            if (slot != inventory.getSelectedSlot()) inventory.add(slot, remainder);
        }
        if (!remainder.isEmpty()) Block.popResource(level, pos, remainder);
    }

    @Override
    public @NonNull List<ItemStack> getDrops(@NonNull BlockState state, LootParams.@NonNull Builder params) {
        List<ItemStack> drops = new ArrayList<>(TeaTrayLayout.CAPACITY + 1);
        drops.addAll(super.getDrops(state, params));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof TeaTrayBlockEntity tray) {
            for (TrayTeacup cup : tray.cups()) drops.add(cup.tea());
        }
        return drops;
    }

    @Override
    public @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level,
            @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @NonNull BlockState getStateForPlacement(@NonNull BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected @NonNull BlockState rotate(@NonNull BlockState state, @NonNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NonNull BlockState mirror(@NonNull BlockState state, @NonNull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @NonNull FluidState getFluidState(@NonNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @NonNull BlockState updateShape(@NonNull BlockState state, @NonNull LevelReader level,
            @NonNull ScheduledTickAccess ticks, @NonNull BlockPos pos, @NonNull Direction direction,
            @NonNull BlockPos neighborPos, @NonNull BlockState neighborState, @NonNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public @NonNull BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new TeaTrayBlockEntity(pos, state);
    }
}
