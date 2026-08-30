package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Canonical lunch-box storage: fifteen stable model slots, each with FIFO units. */
public record LunchBoxContents(List<List<BaggedIngredient>> slots) {
    public static final int SLOT_COUNT = 15;
    public static final int MAX_STACK_SIZE = 16;
    public static final int MAX_UNITS = SLOT_COUNT * MAX_STACK_SIZE;
    public static final LunchBoxContents EMPTY = new LunchBoxContents(List.of());

    private static final Codec<List<BaggedIngredient>> SLOT_CODEC =
            BaggedIngredient.CODEC.sizeLimitedListOf(MAX_STACK_SIZE).xmap(List::copyOf, value -> value);
    public static final Codec<LunchBoxContents> CODEC = SLOT_CODEC.sizeLimitedListOf(SLOT_COUNT)
            .xmap(LunchBoxContents::new, LunchBoxContents::slots);

    private static final StreamCodec<RegistryFriendlyByteBuf, List<BaggedIngredient>> SLOT_STREAM_CODEC =
            BaggedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_STACK_SIZE));
    public static final StreamCodec<RegistryFriendlyByteBuf, LunchBoxContents> STREAM_CODEC =
            SLOT_STREAM_CODEC.apply(ByteBufCodecs.list(SLOT_COUNT))
                    .map(LunchBoxContents::new, LunchBoxContents::slots);

    public LunchBoxContents {
        List<List<BaggedIngredient>> normalized = new ArrayList<>(SLOT_COUNT);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            List<BaggedIngredient> value = slot < slots.size() && slots.get(slot) != null
                    ? slots.get(slot) : List.of();
            normalized.add(List.copyOf(value.subList(0, Math.min(value.size(), MAX_STACK_SIZE))));
        }
        slots = List.copyOf(normalized);
    }

    public List<BaggedIngredient> slot(int index) {
        return index >= 0 && index < SLOT_COUNT ? slots.get(index) : List.of();
    }

    public boolean isEmpty() {
        return slots.stream().allMatch(List::isEmpty);
    }

    public int unitCount() {
        return slots.stream().mapToInt(List::size).sum();
    }

    public int occupiedSlotCount() {
        int occupied = 0;
        for (List<BaggedIngredient> slot : slots) {
            if (!slot.isEmpty()) occupied++;
        }
        return occupied;
    }

    public Optional<BaggedIngredient> first(int index) {
        return slot(index).stream().findFirst();
    }

    public int findSlot(Identifier id) {
        for (int index = 0; index < SLOT_COUNT; index++) {
            List<BaggedIngredient> slot = slots.get(index);
            if (!slot.isEmpty() && slot.getFirst().id().equals(id)) return index;
        }
        return -1;
    }

    public int findEmptySlot() {
        for (int index = 0; index < SLOT_COUNT; index++) {
            if (slots.get(index).isEmpty()) return index;
        }
        return -1;
    }

    public InsertResult insert(List<BaggedIngredient> additions) {
        List<List<BaggedIngredient>> mutable = new ArrayList<>(SLOT_COUNT);
        for (List<BaggedIngredient> slot : slots) mutable.add(new ArrayList<>(slot));
        List<BaggedIngredient> remainder = new ArrayList<>();

        for (BaggedIngredient ingredient : additions) {
            int slotIndex = -1;
            boolean modelExists = false;
            for (int index = 0; index < SLOT_COUNT; index++) {
                List<BaggedIngredient> slot = mutable.get(index);
                if (!slot.isEmpty() && slot.getFirst().id().equals(ingredient.id())) {
                    modelExists = true;
                    if (slot.size() < MAX_STACK_SIZE) {
                        slotIndex = index;
                        break;
                    }
                }
            }
            if (slotIndex < 0 && !modelExists) {
                for (int index = 0; index < SLOT_COUNT; index++) {
                    if (mutable.get(index).isEmpty()) {
                        slotIndex = index;
                        break;
                    }
                }
            }
            if (slotIndex < 0) {
                remainder.add(ingredient);
            } else {
                mutable.get(slotIndex).add(ingredient);
            }
        }
        return new InsertResult(new LunchBoxContents(mutable), List.copyOf(remainder));
    }

    public RemovalResult removeFirst(int index) {
        List<BaggedIngredient> current = slot(index);
        if (current.isEmpty()) return new RemovalResult(this, Optional.empty());
        List<List<BaggedIngredient>> mutable = new ArrayList<>(SLOT_COUNT);
        for (List<BaggedIngredient> slot : slots) mutable.add(new ArrayList<>(slot));
        BaggedIngredient removed = mutable.get(index).removeFirst();
        return new RemovalResult(new LunchBoxContents(mutable), Optional.of(removed));
    }

    public LunchBoxContents rotateFirst(int index) {
        List<BaggedIngredient> current = slot(index);
        if (current.isEmpty()) return this;
        List<List<BaggedIngredient>> mutable = new ArrayList<>(SLOT_COUNT);
        for (List<BaggedIngredient> slot : slots) mutable.add(new ArrayList<>(slot));
        mutable.get(index).set(0, current.getFirst().rotateClockwise());
        return new LunchBoxContents(mutable);
    }

    public record InsertResult(LunchBoxContents contents, List<BaggedIngredient> remainder) {}

    public record RemovalResult(LunchBoxContents contents, Optional<BaggedIngredient> removed) {}
}
