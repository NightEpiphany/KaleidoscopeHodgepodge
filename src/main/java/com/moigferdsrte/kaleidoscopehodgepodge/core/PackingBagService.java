package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.api.Service;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/*食物打包逻辑服务化*/
@Service(usedFor = Service.UsedFor.ITEM)
public final class PackingBagService {
    public static PackingBagContents get(ItemStack stack) {
        PackingBagContents contents = stack.get(KHDataComponents.PACKING_BAG_CONTENTS);
        if (contents != null) return contents;
        String legacyId = stack.get(KHDataComponents.PACKING_BAG_INGREDIENT);
        if (legacyId == null) return PackingBagContents.EMPTY;
        try {
            return PackingBagContents.single(new BaggedIngredient(Identifier.parse(legacyId)));
        } catch (RuntimeException ignored) {
            return PackingBagContents.EMPTY;
        }
    }

    public static boolean has(ItemStack stack) {
        return !get(stack).equals(PackingBagContents.EMPTY);
    }

    public static void set(ItemStack stack, PackingBagContents contents) {
        stack.remove(KHDataComponents.PACKING_BAG_INGREDIENT);
        if (contents.isEmpty()) stack.remove(KHDataComponents.PACKING_BAG_CONTENTS);
        else stack.set(KHDataComponents.PACKING_BAG_CONTENTS, contents);
    }

    public static PackingBagMode getMode(ItemStack stack) {
        PackingBagMode explicit = stack.get(KHDataComponents.PACKING_BAG_MODE);
        if (explicit != null) return explicit;
        // 未写入模式的旧纸袋沿用原本的空袋收纳、满袋放置行为。
        return get(stack).isEmpty() ? PackingBagMode.STORAGE : PackingBagMode.PLACEMENT;
    }

    public static void setMode(ItemStack stack, PackingBagMode mode) {
        stack.set(KHDataComponents.PACKING_BAG_MODE, mode);
    }

    public static PackingBagContents fromWholeDish(List<PackingIngredients> ingredients) {
        return fromWholeDish(ingredients, IngredientFoodData.EMPTY);
    }

    public static PackingBagContents fromWholeDish(List<PackingIngredients> ingredients, Identifier sourceFoodId) {
        return fromWholeDish(ingredients, sourceFoodId, IngredientFoodData.EMPTY);
    }

    public static PackingBagContents fromWholeDish(List<PackingIngredients> ingredients, IngredientFoodData food) {
        return packWholeDish(ingredients, null, food);
    }

    public static PackingBagContents fromWholeDish(List<PackingIngredients> ingredients, Identifier sourceFoodId,
                                                    IngredientFoodData food) {
        return packWholeDish(ingredients, sourceFoodId, food);
    }

    private static PackingBagContents packWholeDish(List<PackingIngredients> ingredients,
                                                     @Nullable Identifier sourceFoodId,
                                                     IngredientFoodData food) {
        List<BaggedIngredient> packed = new ArrayList<>(PackingBagContents.MAX_INGREDIENTS);
        for (PackingIngredients ingredient : ingredients) {
            Integer count = sourceFoodId == null
                    ? ingredient.getCountPerDish()
                    : ingredient.getCountPerDish(sourceFoodId);
            if (count == null) continue;
            for (int index = 0; index < count && packed.size() < PackingBagContents.MAX_INGREDIENTS; index++) {
                packed.add(new BaggedIngredient(ingredient.getId(), 0, food));
            }
            if (packed.size() == PackingBagContents.MAX_INGREDIENTS) break;
        }
        return new PackingBagContents(packed);
    }

    /** 修改手中一个纸袋；若物品堆叠，则把未修改的其余纸袋移回背包。 */
    public static void replaceHeldBag(ItemStack heldStack, @Nullable Player player, PackingBagContents contents) {
        PackingBagMode mode = getMode(heldStack);
        mutateHeldBag(heldStack, player, stack -> {
            set(stack, contents);
            setMode(stack, mode);
        });
    }

    public static void replaceHeldBagMode(ItemStack heldStack, @Nullable Player player, PackingBagMode mode) {
        mutateHeldBag(heldStack, player, stack -> setMode(stack, mode));
    }

    private static void mutateHeldBag(ItemStack heldStack, @Nullable Player player, Consumer<ItemStack> mutation) {
        if (heldStack.getCount() <= 1 || player == null) {
            mutation.accept(heldStack);
            return;
        }
        ItemStack remainder = heldStack.copyWithCount(heldStack.getCount() - 1);
        heldStack.setCount(1);
        mutation.accept(heldStack);
        if (!player.addItem(remainder)) player.drop(remainder, false);
    }

    private PackingBagService() {
    }
}
