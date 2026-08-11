package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor.FoodBiteBlockAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class IngredientFoodService {
    private static final ConcurrentHashMap<Identifier, IngredientFoodData> LEGACY_CACHE = new ConcurrentHashMap<>();

    public static IngredientFoodData capture(Block source) {
        if (source instanceof CakeBlock) {
            return new IngredientFoodData(2, 0.1F, List.of());
        }
        if (source instanceof FoodBiteBlock) {
            FoodBiteBlockAccessor accessor = (FoodBiteBlockAccessor) source;
            return snapshot(accessor.kaleidoscopeHodgepodge$getFoodProperties(),
                    accessor.kaleidoscopeHodgepodge$getConsumable());
        }
        Identifier sourceId = BuiltInRegistries.BLOCK.getKey(source);
        ItemStack sourceItem = BuiltInRegistries.ITEM.getValue(sourceId).getDefaultInstance();
        return snapshot(sourceItem.get(DataComponents.FOOD), sourceItem.get(DataComponents.CONSUMABLE));
    }

    public static IngredientFoodData resolve(Identifier ingredientId, IngredientFoodData stored) {
        var ingredient = PackingIngredientRegistry.byId(ingredientId);
        if (ingredient.filter(value -> !value.hasNutrition()).isPresent()) {
            return IngredientFoodData.EMPTY;
        }
        if (!stored.isEmpty()) return stored;
        return LEGACY_CACHE.computeIfAbsent(ingredientId, id -> ingredient
                .map(value -> capture(BuiltInRegistries.BLOCK.getValue(value.getSrcFoodIds().getFirst())))
                .orElse(IngredientFoodData.EMPTY));
    }

    public static IngredientFoodData resolveForConsumption(Identifier ingredientId, IngredientFoodData stored) {
        IngredientFoodData food = resolve(ingredientId, stored);
        int modelStack = PackingIngredientRegistry.byId(ingredientId)
                .map(value -> value.getModelStack())
                .orElse(1);
        return food.multiplyNutrition(modelStack);
    }

    public static void applyAll(Level level, Player player, List<IngredientFoodData> foods) {
        int nutrition = 0;
        double saturation = 0.0;
        ConcurrentHashMap<EffectKey, Integer> durations = new ConcurrentHashMap<>();
        for (IngredientFoodData food : foods) {
            nutrition = Math.min(20, saturatingAdd(nutrition, food.nutrition()));
            saturation = Math.min(20.0, saturation + food.saturation());
            collectEffects(level, food.effects(), durations);
        }
        if (nutrition > 0 || saturation > 0.0) {
            player.getFoodData().eat(new FoodProperties(nutrition, (float) saturation, true));
        }
        durations.forEach((effect, duration) -> BuiltInRegistries.MOB_EFFECT.getOptional(effect.id())
                .ifPresent(type -> player.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(type), duration, effect.amplifier(),
                        effect.ambient(), effect.visible(), effect.showIcon()))));
    }

    private static IngredientFoodData snapshot(FoodProperties food, Consumable consumable) {
        if (food == null && consumable == null) return IngredientFoodData.EMPTY;
        List<IngredientEffectGroup> effects = consumable == null ? List.of()
                : consumable.onConsumeEffects().stream()
                .filter(ApplyStatusEffectsConsumeEffect.class::isInstance)
                .map(ApplyStatusEffectsConsumeEffect.class::cast)
                .map(group -> new IngredientEffectGroup(group.probability(), group.effects().stream()
                        .map(effect -> new IngredientStatusEffect(
                                BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()),
                                effect.getDuration(), effect.getAmplifier(), effect.isAmbient(),
                                effect.isVisible(), effect.showIcon()))
                        .toList()))
                .toList();
        return new IngredientFoodData(food == null ? 0 : food.nutrition(),
                food == null ? 0.0F : food.saturation(), effects);
    }

    private static void collectEffects(Level level, List<IngredientEffectGroup> groups,
                                       ConcurrentHashMap<EffectKey, Integer> durations) {
        for (IngredientEffectGroup group : groups) {
            if (level.getRandom().nextFloat() >= group.probability()) continue;
            for (IngredientStatusEffect effect : group.effects()) {
                EffectKey key = new EffectKey(effect.id(), effect.amplifier(), effect.ambient(),
                        effect.visible(), effect.showIcon());
                durations.merge(key, effect.duration(), IngredientFoodService::mergeDuration);
            }
        }
    }

    private static int saturatingAdd(int first, int second) {
        return (int) Math.min(Integer.MAX_VALUE, (long) first + second);
    }

    private static int mergeDuration(int first, int second) {
        if (first == MobEffectInstance.INFINITE_DURATION || second == MobEffectInstance.INFINITE_DURATION) {
            return MobEffectInstance.INFINITE_DURATION;
        }
        return saturatingAdd(first, second);
    }

    private record EffectKey(Identifier id, int amplifier, boolean ambient,
                             boolean visible, boolean showIcon) {}

    private IngredientFoodService() {}
}
