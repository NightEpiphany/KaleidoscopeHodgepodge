package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 最多保存九个材料模型的不可变纸袋内容。 */
public record PackingBagContents(List<BaggedIngredient> ingredients) {
    public static final int MAX_INGREDIENTS = 9;
    public static final PackingBagContents EMPTY = new PackingBagContents(List.of());
    public static final Codec<PackingBagContents> CODEC = BaggedIngredient.CODEC.listOf()
            .xmap(PackingBagContents::new, PackingBagContents::ingredients);
    public static final StreamCodec<RegistryFriendlyByteBuf, PackingBagContents> STREAM_CODEC =
            BaggedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INGREDIENTS))
                    .map(PackingBagContents::new, PackingBagContents::ingredients);

    public PackingBagContents {
        ingredients = List.copyOf(ingredients.subList(0, Math.min(ingredients.size(), MAX_INGREDIENTS)));
    }

    public static PackingBagContents single(BaggedIngredient ingredient) {
        return new PackingBagContents(List.of(ingredient));
    }

    public boolean isEmpty() {
        return ingredients.isEmpty();
    }

    public boolean isFull() {
        return ingredients.size() >= MAX_INGREDIENTS;
    }

    public Optional<BaggedIngredient> first() {
        return ingredients.stream().findFirst();
    }

    public PackingBagContents withoutFirst() {
        return ingredients.size() <= 1 ? EMPTY : new PackingBagContents(ingredients.subList(1, ingredients.size()));
    }

    public Optional<PackingBagContents> with(BaggedIngredient ingredient) {
        if (isFull()) return Optional.empty();
        List<BaggedIngredient> appended = new ArrayList<>(ingredients);
        appended.add(ingredient);
        return Optional.of(new PackingBagContents(appended));
    }

    public Optional<PackingBagContents> withAll(List<BaggedIngredient> additions) {
        if (ingredients.size() + additions.size() > MAX_INGREDIENTS) return Optional.empty();
        List<BaggedIngredient> appended = new ArrayList<>(ingredients.size() + additions.size());
        appended.addAll(ingredients);
        appended.addAll(additions);
        return Optional.of(new PackingBagContents(appended));
    }

    public PackingBagContents rotateFirstClockwise() {
        if (ingredients.isEmpty()) return this;
        List<BaggedIngredient> rotated = new ArrayList<>(ingredients);
        rotated.set(0, rotated.getFirst().rotateClockwise());
        return new PackingBagContents(rotated);
    }
}
