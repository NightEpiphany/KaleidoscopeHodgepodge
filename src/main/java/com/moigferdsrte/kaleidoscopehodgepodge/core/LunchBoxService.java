package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.api.Service;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/*午餐盒逻辑服务*/
@Service(usedFor = Service.UsedFor.ITEM)
public final class LunchBoxService {
    public static LunchBoxContents get(ItemStack stack) {
        return stack.getOrDefault(KHDataComponents.LUNCH_BOX_CONTENTS, LunchBoxContents.EMPTY);
    }

    public static void set(ItemStack stack, LunchBoxContents contents) {
        if (contents.isEmpty()) stack.remove(KHDataComponents.LUNCH_BOX_CONTENTS);
        else stack.set(KHDataComponents.LUNCH_BOX_CONTENTS, contents);
        normalizeSelection(stack, contents);
    }

    public static PackingBagMode getMode(ItemStack stack) {
        return stack.getOrDefault(KHDataComponents.LUNCH_BOX_MODE, PackingBagMode.STORAGE);
    }

    public static void setMode(ItemStack stack, PackingBagMode mode) {
        stack.set(KHDataComponents.LUNCH_BOX_MODE, mode);
    }

    public static int selectedSlot(ItemStack stack) {
        int selected = stack.getOrDefault(KHDataComponents.LUNCH_BOX_SELECTED_SLOT, -1);
        return selected >= 0 && selected < LunchBoxContents.SLOT_COUNT
                && !get(stack).slot(selected).isEmpty() ? selected : -1;
    }

    public static void select(ItemStack stack, int slot) {
        if (slot < 0 || slot >= LunchBoxContents.SLOT_COUNT || get(stack).slot(slot).isEmpty()) {
            stack.set(KHDataComponents.LUNCH_BOX_SELECTED_SLOT, -1);
        } else {
            stack.set(KHDataComponents.LUNCH_BOX_SELECTED_SLOT, slot);
        }
    }

    public static @Nullable BaggedIngredient selectedIngredient(ItemStack stack) {
        int selected = selectedSlot(stack);
        return selected < 0 ? null : get(stack).first(selected).orElse(null);
    }

    public static boolean rotateSelected(ItemStack stack) {
        int selected = selectedSlot(stack);
        if (selected < 0) return false;
        set(stack, get(stack).rotateFirst(selected));
        return true;
    }

    public static LunchBoxContents.InsertResult insert(ItemStack stack, List<BaggedIngredient> additions) {
        LunchBoxContents.InsertResult result = get(stack).insert(additions);
        if (result.contents().unitCount() != get(stack).unitCount()) set(stack, result.contents());
        return result;
    }

    public static ItemStack displayStack(LunchBoxContents contents, int slot) {
        List<BaggedIngredient> units = contents.slot(slot);
        if (units.isEmpty()) return ItemStack.EMPTY;
        ItemStack display = IngredientModelService.createDisplay(units.getFirst().id());
        display.setCount(units.size());
        return display;
    }

    public static @Nullable BaggedIngredient toBaggedIngredient(ItemStack display) {
        Identifier id = IngredientModelService.resolveIngredientId(display);
        if (id == null) return null;
        int rotation = display.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_ROTATION, 0);
        IngredientFoodData food = display.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_FOOD,
                IngredientFoodData.EMPTY);
        return new BaggedIngredient(id, rotation, food);
    }

    /** Migrates the old nine-slot CONTAINER format exactly once. */
    public static void migrateIfNeeded(ItemStack stack, Player player) {
        if (stack.has(KHDataComponents.LUNCH_BOX_CONTENTS) || !stack.has(DataComponents.CONTAINER)) {
            if (!stack.has(KHDataComponents.LUNCH_BOX_MODE)) setMode(stack, PackingBagMode.STORAGE);
            if (!stack.has(KHDataComponents.LUNCH_BOX_SELECTED_SLOT)) select(stack, -1);
            return;
        }

        ItemContainerContents old = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> values = NonNullList.withSize(256, ItemStack.EMPTY);
        old.copyInto(values);
        LunchBoxContents converted = LunchBoxContents.EMPTY;
        for (ItemStack value : values) {
            if (value.isEmpty()) continue;
            List<BaggedIngredient> additions = new ArrayList<>();
            if (value.is(KHItems.WRAPPING_BAG)) {
                additions.addAll(PackingBagService.get(value).ingredients());
                if (additions.isEmpty()) returnToPlayer(player, value);
            } else if (value.is(KHItems.INGREDIENT_DISPLAY)) {
                BaggedIngredient ingredient = toBaggedIngredient(value);
                if (ingredient != null) {
                    int amount = Math.min(value.getCount(), LunchBoxContents.MAX_STACK_SIZE);
                    for (int index = 0; index < amount; index++) additions.add(ingredient);
                } else {
                    returnToPlayer(player, value);
                }
            } else {
                returnToPlayer(player, value);
            }
            if (!additions.isEmpty()) {
                // Legacy overflow is intentionally truncated after conversion.
                converted = converted.insert(additions).contents();
            }
        }
        stack.set(KHDataComponents.LUNCH_BOX_CONTENTS, converted);
        stack.remove(DataComponents.CONTAINER);
        setMode(stack, PackingBagMode.STORAGE);
        select(stack, -1);
    }

    private static void normalizeSelection(ItemStack stack, LunchBoxContents contents) {
        int selected = stack.getOrDefault(KHDataComponents.LUNCH_BOX_SELECTED_SLOT, -1);
        if (selected < 0 || selected >= LunchBoxContents.SLOT_COUNT || contents.slot(selected).isEmpty()) {
            stack.set(KHDataComponents.LUNCH_BOX_SELECTED_SLOT, -1);
        }
    }

    private static void returnToPlayer(Player player, ItemStack value) {
        if (!player.addItem(value)) player.drop(value, false);
    }

    private LunchBoxService() {}
}
