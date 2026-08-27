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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Server-authoritative menu whose visible slots are projections of LunchBoxContents. */
public final class LunchBoxMenu extends AbstractContainerMenu {
    public static final int BOX_SLOT_COUNT = LunchBoxContents.SLOT_COUNT;
    private final @Nullable ItemStack boxStack;
    private final @Nullable InteractionHand hand;
    private final Container projectedContents;
    private LunchBoxContents lunchBoxContents;
    private PackingBagMode mode;
    private int selectedSlot;
    private int dragStatus;
    private int dragType;
    private final Set<Integer> dragSlots = new LinkedHashSet<>();

    public LunchBoxMenu(int containerId, Inventory inventory) {
        ItemStack clientBox = findClientBox(inventory);
        this(containerId, inventory, LunchBoxContents.EMPTY, null, null,
                clientBox == null ? PackingBagMode.STORAGE : LunchBoxService.getMode(clientBox),
                clientBox == null ? -1 : LunchBoxService.selectedSlot(clientBox));
    }

    public LunchBoxMenu(int containerId, Inventory inventory, ItemStack boxStack, InteractionHand hand) {
        this(containerId, inventory, initialize(boxStack, inventory.player), boxStack, hand,
                LunchBoxService.getMode(boxStack), LunchBoxService.selectedSlot(boxStack));
    }

    private LunchBoxMenu(int containerId, Inventory inventory, LunchBoxContents contents,
                         @Nullable ItemStack boxStack, @Nullable InteractionHand hand,
                         PackingBagMode mode, int selectedSlot) {
        super(KHMenus.LUNCH_BOX, containerId);
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
        addStandardInventorySlots(inventory, 8, 84);
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
                && player.getItemInHand(hand).is(KHItems.LUNCH_BOX);
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
    public boolean stillValid(@NonNull Player player) {
        return boxStack == null || findBox(player) != null;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, @NonNull ContainerInput containerInput, @NonNull Player player) {
        if (blocksOpenBoxInteraction(slotIndex, buttonNum, containerInput, player)) {
            return;
        }
        if (containerInput == ContainerInput.QUICK_CRAFT) {
            handleQuickCraft(slotIndex, buttonNum, player);
            return;
        }
        if (slotIndex >= 0 && slotIndex < BOX_SLOT_COUNT) {
            handleBoxSlotClick(slotIndex, buttonNum, containerInput, player);
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    /** Keep the item that owns this menu anchored in the player's inventory. */
    private boolean blocksOpenBoxInteraction(int slotIndex, int buttonNum,
                                             ContainerInput input, Player player) {
        if (boxStack == null) return false;
        if (getCarried() == boxStack) return true;

        if (slotIndex >= BOX_SLOT_COUNT && slotIndex < slots.size()
                && slots.get(slotIndex).getItem() == boxStack) {
            return true;
        }

        if (input == ContainerInput.SWAP
                && ((buttonNum >= 0 && buttonNum < 9) || buttonNum == 40)) {
            int inventorySlot = buttonNum == 40 ? 40 : buttonNum;
            return player.getInventory().getItem(inventorySlot) == boxStack;
        }
        return false;
    }

    private void handleBoxSlotClick(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        ItemStack carried = getCarried();
        if (input == ContainerInput.PICKUP && !carried.isEmpty()) {
            insertFromCarried(player, buttonNum == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY);
            return;
        }
        if (input == ContainerInput.QUICK_MOVE && !carried.isEmpty()) {
            insertFromCarried(player, ClickAction.PRIMARY);
            return;
        }
        if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE
                || input == ContainerInput.SWAP || input == ContainerInput.THROW
                || input == ContainerInput.CLONE || input == ContainerInput.PICKUP_ALL) {
            select(slotIndex, player);
        }
    }

    private void handleQuickCraft(int slotIndex, int buttonNum, Player player) {
        int header = getQuickcraftHeader(buttonNum);
        if (header == 0) {
            dragStatus = 1;
            dragType = getQuickcraftType(buttonNum);
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
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size() || slotIndex < BOX_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        Slot source = slots.get(slotIndex);
        ItemStack stack = source.getItem();
        if (!canInsert(stack)) return ItemStack.EMPTY;
        ItemStack original = stack.copy();
        insertFromSlot(player, source, true);
        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    @Override
    public boolean clickMenuButton(@NonNull Player player, int buttonId) {
        if (buttonId != 0) return false;
        mode = mode.next();
        if (boxStack != null) {
            LunchBoxService.setMode(boxStack, mode);
            persist(player);
        }
        return true;
    }

    private void select(int slot, Player player) {
        selectedSlot = lunchBoxContents.slot(slot).isEmpty() ? -1 : slot;
        if (boxStack != null) {
            LunchBoxService.select(boxStack, selectedSlot);
            persist(player);
        }
    }

    private void insertFromCarried(Player player, ClickAction action) {
        ItemStack carried = getCarried();
        if (!canInsert(carried)) return;
        int amount = action == ClickAction.PRIMARY ? carried.getCount() : 1;
        if (carried.is(KHItems.WRAPPING_BAG)) {
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
            PackingBagService.set(carried,
                    new PackingBagContents(bag.ingredients().subList(consumed, bag.ingredients().size())));
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
        setCarried(stack);
        insertFromCarried(player, all ? ClickAction.PRIMARY : ClickAction.SECONDARY);
        ItemStack remainder = getCarried();
        source.setByPlayer(remainder.isEmpty() ? ItemStack.EMPTY : remainder);
        setCarried(ItemStack.EMPTY);
        source.setChanged();
    }

    private boolean canInsert(ItemStack stack) {
        return stack.is(KHItems.WRAPPING_BAG) || stack.is(KHItems.INGREDIENT_DISPLAY);
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
    public void removed(@NonNull Player player) {
        super.removed(player);
        projectedContents.stopOpen(player);
        if (!player.level().isClientSide()) persist(player);
    }

    private @Nullable ItemStack findBox(Player player) {
        if (boxStack == null) return null;
        ItemStack held = hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
        if (held == boxStack && held.is(KHItems.LUNCH_BOX)) return held;
        for (ItemStack value : player.getInventory()) {
            if (value == boxStack && value.is(KHItems.LUNCH_BOX)) return value;
        }
        return null;
    }

    private static @Nullable ItemStack findClientBox(Inventory inventory) {
        ItemStack main = inventory.player.getMainHandItem();
        if (main.is(KHItems.LUNCH_BOX)) return main;
        ItemStack off = inventory.player.getOffhandItem();
        if (off.is(KHItems.LUNCH_BOX)) return off;
        for (ItemStack value : inventory) {
            if (value.is(KHItems.LUNCH_BOX)) return value;
        }
        return null;
    }

    private static final class LunchBoxSlot extends Slot {
        private LunchBoxSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(KHItems.WRAPPING_BAG) || stack.is(KHItems.INGREDIENT_DISPLAY);
        }

        @Override
        public boolean mayPickup(@NonNull Player player) {
            return false;
        }
    }
}
