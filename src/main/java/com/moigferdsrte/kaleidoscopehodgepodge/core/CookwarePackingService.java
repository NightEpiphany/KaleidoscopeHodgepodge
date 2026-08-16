package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** 将锅的最终成品转换为纸袋内容，不依赖普通或 Flex 配方类型。 */
public final class CookwarePackingService {
    public static boolean tryPack(Level level, LivingEntity user, ItemStack bag, ItemStack product) {
        if (!isEmptyStorageBag(bag)) return false;
        PackPlan plan = createPlan(product).orElse(null);
        if (plan == null) return false;
        if (!level.isClientSide()) {
            PackingBagService.replaceHeldBag(bag, user instanceof Player player ? player : null, plan.contents());
        }
        return true;
    }

    public static Optional<PackPlan> createPlan(ItemStack product) {
        if (!(product.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof FoodBiteBlock food)) {
            return Optional.empty();
        }
        ResourceLocation sourceId = BuiltInRegistries.BLOCK.getKey(food);
        List<PackingIngredients> ingredients = PackingIngredientRegistry.bySource(sourceId);
        if (ingredients.isEmpty()) return Optional.empty();
        PackingBagContents contents = PackingBagService.fromWholeDish(
                ingredients, sourceId, IngredientFoodService.capture(food));
        return contents.isEmpty() ? Optional.empty() : Optional.of(new PackPlan(sourceId, contents));
    }

    private static boolean isEmptyStorageBag(ItemStack stack) {
        return stack.is(KHItems.WRAPPING_BAG)
                && PackingBagService.get(stack).isEmpty()
                && PackingBagService.getMode(stack) == PackingBagMode.STORAGE;
    }

    public record PackPlan(ResourceLocation sourceId, PackingBagContents contents) {}

    private CookwarePackingService() {}
}
