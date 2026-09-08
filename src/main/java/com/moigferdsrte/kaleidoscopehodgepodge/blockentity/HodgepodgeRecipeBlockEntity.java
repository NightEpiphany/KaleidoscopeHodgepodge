package com.moigferdsrte.kaleidoscopehodgepodge.blockentity;

import com.moigferdsrte.kaleidoscopehodgepodge.core.HodgepodgeRecipeData;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlockEntities;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

/** 存储杂烩食材和结构信息的菜谱 */
public final class HodgepodgeRecipeBlockEntity extends BlockEntity {
    private static final String SHOW_ITEMS = "ShowItems";
    private ItemStack item = ItemStack.EMPTY;

    public HodgepodgeRecipeBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.RECIPE, pos, state);
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack value) {
        item = value == null || value.isEmpty() ? ItemStack.EMPTY : value.copyWithCount(1);
        setChanged();
    }

    public HodgepodgeRecipeData recipe() {
        return item.get(KHDataComponents.HODGEPODGE_RECIPE);
    }

    public void setRecipe(HodgepodgeRecipeData value) {
        if (value == null) {
            setItem(ItemStack.EMPTY);
            return;
        }
        ItemStack stack = new ItemStack(KHItems.HODGEPODGE_RECIPE);
        stack.set(KHDataComponents.HODGEPODGE_RECIPE, value);
        setItem(stack);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        item = input.read(SHOW_ITEMS, ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        if (!item.isEmpty()) output.store(SHOW_ITEMS, ItemStack.CODEC, item);
    }

    @Override
    public @NonNull CompoundTag getUpdateTag(HolderLookup.@NonNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
