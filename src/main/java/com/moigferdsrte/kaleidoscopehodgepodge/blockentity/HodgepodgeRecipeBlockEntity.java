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
import com.moigferdsrte.kaleidoscopehodgepodge.util.NbtCodecs;
import org.jetbrains.annotations.NotNull;

/** 存储杂烩食材和结构信息的菜谱 */
public final class HodgepodgeRecipeBlockEntity extends BlockEntity {
    private static final String SHOW_ITEMS = "ShowItems";
    private ItemStack item = ItemStack.EMPTY;

    public HodgepodgeRecipeBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.RECIPE.get(), pos, state);
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack value) {
        item = value == null || value.isEmpty() ? ItemStack.EMPTY : value.copyWithCount(1);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public HodgepodgeRecipeData recipe() {
        return item.get(KHDataComponents.HODGEPODGE_RECIPE.get());
    }

    public void setRecipe(HodgepodgeRecipeData value) {
        if (value == null) {
            setItem(ItemStack.EMPTY);
            return;
        }
        ItemStack stack = new ItemStack(KHItems.HODGEPODGE_RECIPE.get());
        stack.set(KHDataComponents.HODGEPODGE_RECIPE.get(), value);
        setItem(stack);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag input, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(input, registries);
        item = NbtCodecs.read(input, SHOW_ITEMS, ItemStack.CODEC, registries).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag output, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(output, registries);
        if (!item.isEmpty()) NbtCodecs.write(output, SHOW_ITEMS, ItemStack.CODEC, item, registries);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
