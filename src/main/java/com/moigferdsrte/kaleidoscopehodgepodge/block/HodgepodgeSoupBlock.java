package com.moigferdsrte.kaleidoscopehodgepodge.block;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.mojang.datafixers.util.Unit;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class HodgepodgeSoupBlock extends AbstractHodgepodgeFeastBlock {
    public static final BooleanProperty HAS_SOUP = BooleanProperty.create("has_soup");

    public HodgepodgeSoupBlock(Properties properties) {
        super(properties, CustomFeastData.ContainerKind.SOUP);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false).setValue(HAS_SOUP, true));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NonNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        return state.setValue(HAS_SOUP, context.getItemInHand().has(KHDataComponents.SOUP_BASE));
    }

    @Override
    public @NonNull InteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state,
                                                @NonNull Level level, @NonNull BlockPos pos,
                                                @NonNull Player player, @NonNull InteractionHand hand,
                                                @NonNull BlockHitResult hit) {
        if (!state.getValue(HAS_SOUP) && stack.is(ModItems.PORK_BONE_SOUP)) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            level.setBlock(pos, state.setValue(HAS_SOUP, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.isCreative()) stack.shrink(1);
            ItemStack bowl = new ItemStack(Items.BOWL);
            if (!player.addItem(bowl)) player.drop(bowl, false);
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected void applyContainerStateToItem(ItemStack stack, BlockState state) {
        if (state.getValue(HAS_SOUP)) stack.set(KHDataComponents.SOUP_BASE,
                Unit.INSTANCE);
        else stack.remove(KHDataComponents.SOUP_BASE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_SOUP);
    }
}
