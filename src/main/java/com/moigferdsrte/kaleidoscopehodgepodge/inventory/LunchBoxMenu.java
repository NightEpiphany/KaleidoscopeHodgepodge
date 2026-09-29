package com.moigferdsrte.kaleidoscopehodgepodge.inventory;

import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHMenus;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 服务端午餐盒页面定义 */
public final class LunchBoxMenu extends AbstractContainerMenu {
    public static final int BOX_SLOT_COUNT = LunchBoxContents.SLOT_COUNT;
    private final @Nullable ItemStack boxStack;
    private InteractionHand hand;
    private final Container projectedContents;
    private LunchBoxContents lunchBoxContents;
    private PackingBagMode mode;
    private int selectedSlot;
    private int dragStatus;
    private int dragType;
    private final Set<Integer> dragSlots = new LinkedHashSet<>(BOX_SLOT_COUNT * 2);

    public LunchBoxMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, LunchBoxContents.EMPTY, null, InteractionHand.MAIN_HAND,
                PackingBagMode.STORAGE, -1);
    }

    public LunchBoxMenu(int containerId, Inventory inventory, ItemStack boxStack, InteractionHand hand) {
        this(containerId, inventory, initialize(boxStack, inventory.player), boxStack, hand,
                LunchBoxService.getMode(boxStack), LunchBoxService.selectedSlot(boxStack));
    }

    private LunchBoxMenu(int containerId, Inventory inventory, LunchBoxContents contents,
                         @Nullable ItemStack boxStack, @Nullable InteractionHand hand,
                         PackingBagMode mode, int selectedSlot) {
        super(KHMenus.LUNCH_BOX.get(), containerId);
        this.boxStack = boxStack;
        this.hand = hand;
        this.lunchBoxContents = contents;
        this.mode = mode;
        this.selectedSlot = selectedSlot;
        this.projectedContents = new SimpleContainer(BOX_SLOT_COUNT);
        this.dragStatus = 0;
        this.dragType = 0;
        projectedContents.startOpen(inventory.player);
        refreshProjection();
        for (int slot = 0; slot < BOX_SLOT_COUNT; slot++) {
            int x = slot % 5;
            int y = slot / 5;
            addSlot(new LunchBoxSlot(projectedContents, slot, 26 + x * 18, 17 + y * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlot(new DataSlot() {
            public int get() { return LunchBoxMenu.this.mode.ordinal(); }
            public void set(int value) { LunchBoxMenu.this.mode = value == 1 ? PackingBagMode.PLACEMENT : PackingBagMode.STORAGE; }
        });
        addDataSlot(new DataSlot() {
            public int get() { return LunchBoxMenu.this.selectedSlot; }
            public void set(int value) { LunchBoxMenu.this.selectedSlot = value >= 0 && value < BOX_SLOT_COUNT ? value : -1; }
        });
        addDataSlot(new DataSlot() {
            public int get() { return LunchBoxMenu.this.hand.ordinal(); }
            public void set(int value) { LunchBoxMenu.this.hand = value == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND; }
        });
    }

    private static LunchBoxContents initialize(ItemStack stack, Player player) {
        if (!player.level().isClientSide()) LunchBoxService.migrateIfNeeded(stack, player);
        return LunchBoxService.get(stack);
    }

    public LunchBoxContents contents() {
        return lunchBoxContents;
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public PackingBagMode mode() {
        return mode;
    }

    public InteractionHand hand() {
        return hand == null ? InteractionHand.MAIN_HAND : hand;
    }

    public void setClientSelectedSlot(int slot) {
        selectedSlot = slot >= 0 && slot < BOX_SLOT_COUNT && hasProjectedItem(slot) ? slot : -1;
    }

    public void setClientMode(PackingBagMode mode) {
        this.mode = mode;
    }

    public boolean ownsBox(Player player, InteractionHand hand) {
        return boxStack != null && player.getItemInHand(hand) == boxStack
                && player.getItemInHand(hand).is(KHItems.LUNCH_BOX.get());
    }

    /** Client-side identity check used by the item model while this menu is open. */
    public boolean isOpenFor(ItemStack stack) {
        return boxStack != null && boxStack == stack;
    }

    public void selectFromNetwork(int slot, Player player) {
        select(slot, player);
    }

    public ItemStack previewStack() {
        if (selectedSlot < 0) return ItemStack.EMPTY;
        ItemStack display = boxStack == null ? projectedContents.getItem(selectedSlot)
                : LunchBoxService.displayStack(lunchBoxContents, selectedSlot);
        return display.copyWithCount(1);
    }

    public boolean hasProjectedItem(int slot) {
        return slot >= 0 && slot < BOX_SLOT_COUNT
                && (boxStack == null ? !projectedContents.getItem(slot).isEmpty()
                : !lunchBoxContents.slot(slot).isEmpty());
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return boxStack == null || findBox(player) != null;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, @NotNull ClickType containerInput, @NotNull Player player) {
        if (blocksOpenBoxInteraction(slotIndex, buttonNum, containerInput, player)) {
            return;
        }
        if (containerInput == ClickType.QUICK_CRAFT) {
            handleQuickCraft(slotIndex, buttonNum, player);
            // Vanilla still owns dragging within the player's inventory.
            if (slotIndex < 0 || slotIndex >= BOX_SLOT_COUNT) {
                super.clicked(slotIndex, buttonNum, containerInput, player);
            }
            return;
        }
        dragStatus = 0;
        dragSlots.clear();
        if (slotIndex >= 0 && slotIndex < BOX_SLOT_COUNT) {
            handleBoxSlotClick(slotIndex, buttonNum, containerInput, player);
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    /** Keep the item that owns this menu anchored in the player's inventory. */
    private boolean blocksOpenBoxInteraction(int slotIndex, int buttonNum,
                                             ClickType input, Player player) {
        if (boxStack == null) return false;
        if (getCarried() == boxStack) return true;

        if (slotIndex >= BOX_SLOT_COUNT && slotIndex < slots.size()
                && slots.get(slotIndex).getItem() == boxStack) {
            return true;
        }

        if (input == ClickType.SWAP
                && ((buttonNum >= 0 && buttonNum < 9) || buttonNum == 40)) {
            int inventorySlot = buttonNum == 40 ? 40 : buttonNum;
            return player.getInventory().getItem(inventorySlot) == boxStack;
        }
        return false;
    }

    private void handleBoxSlotClick(int slotIndex, int buttonNum, ClickType input, Player player) {
        ItemStack carried = getCarried();
        if (input == ClickType.PICKUP && !carried.isEmpty()) {
            insertFromCarried(player, buttonNum == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY);
            return;
        }
        if (input == ClickType.QUICK_MOVE && !carried.isEmpty()) {
            insertFromCarried(player, ClickAction.PRIMARY);
            return;
        }
        if (input == ClickType.PICKUP || input == ClickType.QUICK_MOVE
                || input == ClickType.SWAP || input == ClickType.THROW
                || input == ClickType.CLONE || input == ClickType.PICKUP_ALL) {
            select(slotIndex, player);
        }
    }

    private void handleQuickCraft(int slotIndex, int buttonNum, Player player) {
        int header = getQuickcraftHeader(buttonNum);
        if (header == 0) {
            dragType = getQuickcraftType(buttonNum);
            dragStatus = isValidQuickcraftType(dragType, player) ? 1 : 0;
            dragSlots.clear();
            return;
        }
        if (header == 1) {
            if (dragStatus == 1 && slotIndex >= 0 && slotIndex < BOX_SLOT_COUNT
                    && !getCarried().isEmpty() && canInsert(getCarried())) {
                dragSlots.add(slotIndex);
            }
            return;
        }
        if (header == 2) {
            if (dragStatus == 1) {
                ClickAction action = dragType == 1 ? ClickAction.SECONDARY : ClickAction.PRIMARY;
                for (int ignored : dragSlots) {
                    if (getCarried().isEmpty()) break;
                    insertFromCarried(player, action);
                }
            }
            dragStatus = 0;
            dragSlots.clear();
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size() || slotIndex < BOX_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        Slot source = slots.get(slotIndex);
        ItemStack stack = source.getItem();
        if (!canInsert(stack)) return ItemStack.EMPTY;
        ItemStack original = stack.copy();
        insertFromSlot(player, source, true);
        return ItemStack.matches(stack, original) ? ItemStack.EMPTY : original;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (buttonId != 0) return false;
        mode = mode.next();
        if (boxStack != null) {
            LunchBoxService.setMode(boxStack, mode);
            persist(player);
        }
        return true;
    }

    private void select(int slot, Player player) {
        selectedSlot = hasProjectedItem(slot) ? slot : -1;
        if (boxStack != null) {
            LunchBoxService.select(boxStack, selectedSlot);
            persist(player);
        }
    }

    private void insertFromCarried(Player player, ClickAction action) {
        ItemStack carried = getCarried();
        if (!canInsert(carried)) return;
        int amount = action == ClickAction.PRIMARY ? carried.getCount() : 1;
        if (carried.is(KHItems.WRAPPING_BAG.get())) {
            PackingBagContents bag = PackingBagService.get(carried);
            if (bag.isEmpty()) return;
            // A wrapping bag is itself unstackable, so its ItemStack count is
            // always one. Primary clicks must use the bag's ingredient count.
            amount = action == ClickAction.PRIMARY ? bag.ingredients().size() : 1;
            List<BaggedIngredient> additions = new ArrayList<>(bag.ingredients().subList(0, amount));
            LunchBoxContents.InsertResult result = lunchBoxContents.insert(additions);
            int consumed = additions.size() - result.remainder().size();
            if (consumed <= 0) return;
            lunchBoxContents = result.contents();
            List<BaggedIngredient> remainder = new ArrayList<>(bag.ingredients().size() - consumed);
            remainder.addAll(result.remainder());
            remainder.addAll(bag.ingredients().subList(amount, bag.ingredients().size()));
            PackingBagService.set(carried, new PackingBagContents(remainder));
        } else {
            BaggedIngredient ingredient = LunchBoxService.toBaggedIngredient(carried);
            if (ingredient == null) return;
            amount = Math.min(amount, carried.getCount());
            List<BaggedIngredient> additions = new ArrayList<>(amount);
            for (int index = 0; index < amount; index++) additions.add(ingredient);
            LunchBoxContents.InsertResult result = lunchBoxContents.insert(additions);
            int consumed = additions.size() - result.remainder().size();
            if (consumed <= 0) return;
            lunchBoxContents = result.contents();
            carried.shrink(consumed);
        }
        persistContents(player);
    }

    private void insertFromSlot(Player player, Slot source, boolean all) {
        ItemStack stack = source.getItem();
        if (!canInsert(stack)) return;
        ItemStack previousCarried = getCarried();
        setCarried(stack);
        try {
            insertFromCarried(player, all ? ClickAction.PRIMARY : ClickAction.SECONDARY);
            ItemStack remainder = getCarried();
            source.setByPlayer(remainder.isEmpty() ? ItemStack.EMPTY : remainder);
        } finally {
            setCarried(previousCarried);
        }
        source.setChanged();
    }

    private boolean canInsert(ItemStack stack) {
        return stack.is(KHItems.WRAPPING_BAG.get()) || stack.is(KHItems.INGREDIENT_DISPLAY.get());
    }

    private void persistContents(Player player) {
        refreshProjection();
        if (boxStack != null) {
            LunchBoxService.set(boxStack, lunchBoxContents);
            LunchBoxService.select(boxStack, selectedSlot);
            persist(player);
        }
        slotsChanged(player.getInventory());
    }

    private void refreshProjection() {
        for (int slot = 0; slot < BOX_SLOT_COUNT; slot++) {
            projectedContents.setItem(slot, LunchBoxService.displayStack(lunchBoxContents, slot));
        }
    }

    private void persist(Player player) {
        if (boxStack == null) return;
        ItemStack target = findBox(player);
        if (target != null) {
            LunchBoxService.set(target, lunchBoxContents);
            LunchBoxService.setMode(target, mode);
            LunchBoxService.select(target, selectedSlot);
        }
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        projectedContents.stopOpen(player);
        if (!player.level().isClientSide()) persist(player);
    }

    private @Nullable ItemStack findBox(Player player) {
        if (boxStack == null) return null;
        ItemStack held = hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
        if (held == boxStack && held.is(KHItems.LUNCH_BOX.get())) return held;
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            ItemStack value = player.getInventory().getItem(index);
            if (value == boxStack && value.is(KHItems.LUNCH_BOX.get())) return value;
        }
        return null;
    }

    private static @Nullable ItemStack findClientBox(Inventory inventory) {
        ItemStack main = inventory.player.getMainHandItem();
        if (main.is(KHItems.LUNCH_BOX.get())) return main;
        ItemStack off = inventory.player.getOffhandItem();
        if (off.is(KHItems.LUNCH_BOX.get())) return off;
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            ItemStack value = inventory.getItem(index);
            if (value.is(KHItems.LUNCH_BOX.get())) return value;
        }
        return null;
    }

    private static final class LunchBoxSlot extends Slot {
        private LunchBoxSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(KHItems.WRAPPING_BAG.get()) || stack.is(KHItems.INGREDIENT_DISPLAY.get());
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return false;
        }
    }
}
