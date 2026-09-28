package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public record TrayTeacup(int slot, ItemStack tea) {
    public static final Codec<TrayTeacup> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, TeaTrayLayout.CAPACITY - 1).fieldOf("slot").forGetter(TrayTeacup::slot),
            ItemStack.CODEC.fieldOf("tea").forGetter(TrayTeacup::tea)
    ).apply(instance, TrayTeacup::new));

    public TrayTeacup {
        if (slot < 0 || slot >= TeaTrayLayout.CAPACITY) throw new IllegalArgumentException("Invalid tea slot");
        tea = tea.copyWithCount(1);
    }

    @Override
    public ItemStack tea() {
        return tea.copy();
    }
}
