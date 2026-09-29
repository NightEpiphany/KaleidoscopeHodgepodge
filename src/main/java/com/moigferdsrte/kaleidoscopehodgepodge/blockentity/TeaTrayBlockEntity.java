package com.moigferdsrte.kaleidoscopehodgepodge.blockentity;

import com.github.ysbbbbbb.kaleidoscopecookery.item.TeacupItem;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TeaTrayLayout;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TrayTeacup;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlockEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.moigferdsrte.kaleidoscopehodgepodge.util.NbtCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public final class TeaTrayBlockEntity extends BlockEntity {
    private static final Codec<List<TrayTeacup>> CUPS_CODEC = TrayTeacup.CODEC.listOf(0, TeaTrayLayout.CAPACITY);

    private final List<TrayTeacup> cups = new ArrayList<>(TeaTrayLayout.CAPACITY);

    public TeaTrayBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.TEA_TRAY.get(), pos, state);
    }

    public static boolean accepts(ItemStack stack) {
        return isTea(stack) || stack.is(ModItems.EMPTY_CUP.get());
    }

    public static boolean isTea(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TeacupItem;
    }

    public List<TrayTeacup> cups() {
        return List.copyOf(cups);
    }

    public boolean isFull() {
        return cups.size() == TeaTrayLayout.CAPACITY;
    }

    public boolean isEmpty() {
        return cups.isEmpty();
    }

    public boolean canInsert(int slot) {
        if (slot < 0 || slot >= TeaTrayLayout.CAPACITY || isFull()) return false;
        return !hasCup(slot);
    }

    public boolean hasCup(int slot) {
        for (TrayTeacup cup : cups) {
            if (cup.slot() == slot) return true;
        }
        return false;
    }

    public boolean insert(ItemStack stack, int slot) {
        if (!accepts(stack) || !canInsert(slot)) return false;
        cups.add(new TrayTeacup(slot, stack));
        contentsChanged();
        return true;
    }

    public ItemStack removeFirst() {
        if (cups.isEmpty()) return ItemStack.EMPTY;
        ItemStack tea = cups.removeFirst().tea();
        contentsChanged();
        return tea;
    }

    public ItemStack removeAt(int slot) {
        for (int index = 0; index < cups.size(); index++) {
            if (cups.get(index).slot() == slot) {
                ItemStack tea = cups.remove(index).tea();
                contentsChanged();
                return tea;
            }
        }
        return ItemStack.EMPTY;
    }

    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag input, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(input, registries);
        cups.clear();
        int occupied = 0;
        for (TrayTeacup cup : NbtCodecs.read(input, "Cups", CUPS_CODEC, registries).orElse(List.of())) {
            int mask = 1 << cup.slot();
            if ((occupied & mask) != 0 || !accepts(cup.tea())) continue;
            cups.add(cup);
            occupied |= mask;
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag output, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(output, registries);
        NbtCodecs.write(output, "Cups", CUPS_CODEC, cups, registries);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @NotNull ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
