package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.api.Service;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** 使用transfer api事务化提交将刚刚烹饪完成的菜品数据并解构为食材 */
@Service(usedFor = Service.UsedFor.ITEM)
public final class CookwarePackingService {
    @SuppressWarnings("all")
    public static boolean tryPack(Level level, LivingEntity user, ItemStack container, ItemStack product) {
        PackPlan plan = createPlan(product).orElse(null);
        if (plan == null) return false;

        Player player = user instanceof Player value ? value : null;
        if (container.is(KHItems.WRAPPING_BAG)) {
            if (PackingBagService.getMode(container) != PackingBagMode.STORAGE) return false;
            PackingBagContents updated = PackingBagService.get(container)
                    .withAll(plan.contents().ingredients()).orElse(null);
            if (updated == null) return false;
            if (!level.isClientSide()) PackingBagService.replaceHeldBag(container, player, updated);
            return true;
        }

        if (!container.is(KHItems.LUNCH_BOX)
                || LunchBoxService.getMode(container) != PackingBagMode.STORAGE) {
            return false;
        }
        LunchBoxContents.InsertResult inserted = LunchBoxService.get(container)
                .insert(plan.contents().ingredients());
        if (!inserted.remainder().isEmpty()) return false;
        if (!level.isClientSide()) LunchBoxService.set(container, inserted.contents());
        return true;
    }

    public static Optional<PackPlan> createPlan(ItemStack product) {
        if (!(product.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof FoodBiteBlock food)) {
            return Optional.empty();
        }
        Identifier sourceId = BuiltInRegistries.BLOCK.getKey(food);
        List<PackingIngredients> ingredients = PackingIngredientRegistry.bySource(sourceId);
        if (ingredients.isEmpty()) return Optional.empty();
        PackingBagContents contents = PackingBagService.fromWholeDish(
                ingredients, sourceId, IngredientFoodService.capture(food));
        return contents.isEmpty() ? Optional.empty() : Optional.of(new PackPlan(sourceId, contents));
    }

    public record PackPlan(Identifier sourceId, PackingBagContents contents) {}

    private CookwarePackingService() {}
}
