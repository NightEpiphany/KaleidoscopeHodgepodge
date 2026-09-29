package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.api.Service;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/*午餐盒逻辑服务*/
@Service(usedFor = Service.UsedFor.ITEM)
public final class LunchBoxService {
    public static LunchBoxContents get(ItemStack stack) {
        return stack.getOrDefault(KHDataComponents.LUNCH_BOX_CONTENTS.get(), LunchBoxContents.EMPTY);
    }

    public static void set(ItemStack stack, LunchBoxContents contents) {
        if (contents.isEmpty()) stack.remove(KHDataComponents.LUNCH_BOX_CONTENTS.get());
        else stack.set(KHDataComponents.LUNCH_BOX_CONTENTS.get(), contents);
        normalizeSelection(stack, contents);
    }

    public static PackingBagMode getMode(ItemStack stack) {
        return stack.getOrDefault(KHDataComponents.LUNCH_BOX_MODE.get(), PackingBagMode.STORAGE);
    }

    public static void setMode(ItemStack stack, PackingBagMode mode) {
        stack.set(KHDataComponents.LUNCH_BOX_MODE.get(), mode);
    }

    public static int selectedSlot(ItemStack stack) {
        int selected = stack.getOrDefault(KHDataComponents.LUNCH_BOX_SELECTED_SLOT.get(), -1);
        return selected >= 0 && selected < LunchBoxContents.SLOT_COUNT
                && !get(stack).slot(selected).isEmpty() ? selected : -1;
    }

    public static void select(ItemStack stack, int slot) {
        if (slot < 0 || slot >= LunchBoxContents.SLOT_COUNT || get(stack).slot(slot).isEmpty()) {
            stack.set(KHDataComponents.LUNCH_BOX_SELECTED_SLOT.get(), -1);
        } else {
            stack.set(KHDataComponents.LUNCH_BOX_SELECTED_SLOT.get(), slot);
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
        ResourceLocation id = IngredientModelService.resolveIngredientId(display);
        if (id == null) return null;
        int rotation = display.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_ROTATION.get(), 0);
        IngredientFoodData food = display.getOrDefault(KHDataComponents.INGREDIENT_DISPLAY_FOOD.get(),
                IngredientFoodData.EMPTY);
        return new BaggedIngredient(id, rotation, food);
    }

    /** Migrates the old nine-slot CONTAINER format exactly once. */
    public static void migrateIfNeeded(ItemStack stack, Player player) {
        if (stack.has(KHDataComponents.LUNCH_BOX_CONTENTS.get()) || !stack.has(DataComponents.CONTAINER)) {
            if (!stack.has(KHDataComponents.LUNCH_BOX_MODE.get())) setMode(stack, PackingBagMode.STORAGE);
            if (!stack.has(KHDataComponents.LUNCH_BOX_SELECTED_SLOT.get())) select(stack, -1);
            return;
        }

        ItemContainerContents old = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> values = NonNullList.withSize(256, ItemStack.EMPTY);
        old.copyInto(values);
        LunchBoxContents converted = LunchBoxContents.EMPTY;
        for (ItemStack value : values) {
            if (value.isEmpty()) continue;
            if (value.is(KHItems.WRAPPING_BAG.get())) {
                LunchBoxContents.InsertResult result = converted.insert(PackingBagService.get(value).ingredients());
                converted = result.contents();
                ItemStack returnedBag = value.copy();
                PackingBagService.set(returnedBag, new PackingBagContents(result.remainder()));
                returnToPlayer(player, returnedBag);
            } else if (value.is(KHItems.INGREDIENT_DISPLAY.get())) {
                BaggedIngredient ingredient = toBaggedIngredient(value);
                if (ingredient != null) {
                    List<BaggedIngredient> additions = java.util.Collections.nCopies(value.getCount(), ingredient);
                    LunchBoxContents.InsertResult result = converted.insert(additions);
                    converted = result.contents();
                    if (!result.remainder().isEmpty()) {
                        returnToPlayer(player, value.copyWithCount(result.remainder().size()));
                    }
                } else {
                    returnToPlayer(player, value);
                }
            } else {
                returnToPlayer(player, value);
            }
        }
        stack.set(KHDataComponents.LUNCH_BOX_CONTENTS.get(), converted);
        stack.remove(DataComponents.CONTAINER);
        setMode(stack, PackingBagMode.STORAGE);
        select(stack, -1);
    }

    private static void normalizeSelection(ItemStack stack, LunchBoxContents contents) {
        int selected = stack.getOrDefault(KHDataComponents.LUNCH_BOX_SELECTED_SLOT.get(), -1);
        if (selected < 0 || selected >= LunchBoxContents.SLOT_COUNT || contents.slot(selected).isEmpty()) {
            stack.set(KHDataComponents.LUNCH_BOX_SELECTED_SLOT.get(), -1);
        }
    }

    private static void returnToPlayer(Player player, ItemStack value) {
        if (!player.addItem(value)) player.drop(value, false);
    }

    private LunchBoxService() {}
}
