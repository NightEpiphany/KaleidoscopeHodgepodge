package com.moigferdsrte.kaleidoscopehodgepodge.inventory;

import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

/** 九格午餐盒菜单，客户端与服务端共用相同的纸袋槽位限制。 */
public final class LunchBoxMenu extends AbstractContainerMenu {
    private static final int BOX_SLOT_COUNT = 9;
    private static final int INVENTORY_SLOT_END = 45;
    private final @Nullable ItemStack boxStack;
    private final @Nullable InteractionHand hand;
    private final Container lunchBoxContents;

    public LunchBoxMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(BOX_SLOT_COUNT), null, null);
    }

    public LunchBoxMenu(int containerId, Inventory inventory, ItemStack boxStack, InteractionHand hand) {
        this(containerId, inventory, createContents(boxStack), boxStack, hand);
    }

    private LunchBoxMenu(int containerId, Inventory inventory, Container contents,
                         @Nullable ItemStack boxStack, @Nullable InteractionHand hand) {
        super(KHMenus.LUNCH_BOX, containerId);
        checkContainerSize(contents, BOX_SLOT_COUNT);
        this.boxStack = boxStack;
        this.hand = hand;
        this.lunchBoxContents = contents;
        contents.startOpen(inventory.player);
        for (int slot = 0; slot < BOX_SLOT_COUNT; slot++) {
            int x = slot % 3;
            int y = slot / 3;
            addSlot(new BagSlot(contents, slot, 62 + x * 18, 17 + y * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return lunchBoxContents.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < BOX_SLOT_COUNT) {
            if (!moveItemStackTo(stack, BOX_SLOT_COUNT, INVENTORY_SLOT_END, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, BOX_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        lunchBoxContents.stopOpen(player);
        if (player.level().isClientSide()) return;
        ItemStack target = findBox(player);
        if (target != null) {
            NonNullList<ItemStack> values = NonNullList.withSize(BOX_SLOT_COUNT, ItemStack.EMPTY);
            for (int slot = 0; slot < values.size(); slot++) {
                values.set(slot, lunchBoxContents.getItem(slot));
            }
            target.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(values));
            return;
        }
        for (int slot = 0; slot < lunchBoxContents.getContainerSize(); slot++) {
            ItemStack value = lunchBoxContents.removeItemNoUpdate(slot);
            if (!value.isEmpty() && !player.addItem(value)) player.drop(value, false);
        }
    }

    private ItemStack findBox(Player player) {
        if (boxStack == null || hand == null) return null;
        ItemStack held = player.getItemInHand(hand);
        if (held == boxStack && held.is(KHItems.LUNCH_BOX)) return held;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack value = inventory.getItem(slot);
            if (value == boxStack && value.is(KHItems.LUNCH_BOX)) return value;
        }
        return null;
    }

    private static Container createContents(ItemStack boxStack) {
        SimpleContainer container = new SimpleContainer(BOX_SLOT_COUNT);
        ItemContainerContents contents = boxStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> values = NonNullList.withSize(BOX_SLOT_COUNT, ItemStack.EMPTY);
        contents.copyInto(values);
        for (int slot = 0; slot < values.size(); slot++) {
            ItemStack value = values.get(slot);
            container.setItem(slot, value);
        }
        return container;
    }

    private static final class BagSlot extends Slot {
        private BagSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(KHItems.WRAPPING_BAG);
        }
    }
}
