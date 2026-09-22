package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.TeacupRegistry;
import com.moigferdsrte.kaleidoscopehodgepodge.block.TeaTrayBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.TeaTrayBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.compat.Compat;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TrayTeacup;
import com.moigferdsrte.kaleidoscopehodgepodge.core.TeaTrayLayout;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class TeaTrayGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest
    public void chineseFoodCupsPreserveIdentityAndModelsInClientUpdate(GameTestHelper helper) {
        if (!FabricLoader.getInstance().isModLoaded(Compat.KCH)) {
            helper.succeed();
            return;
        }
        TeaTrayBlockEntity tray = place(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        List<String> teas = List.of("hk_milk_tea", "dianhong_tea");
        for (int slot = 0; slot < teas.size(); slot++) {
            Identifier id = Identifier.fromNamespaceAndPath(Compat.KCH, teas.get(slot));
            ItemStack stack = BuiltInRegistries.ITEM.getValue(id).getDefaultInstance();
            helper.assertTrue(TeaTrayBlockEntity.isTea(stack), "Chinese Food tea not registered: " + id);
            stack.setCount(2);
            helper.assertTrue(useAt(helper, player, stack, hitSlot(tray.getBlockPos(), slot, Direction.NORTH))
                    .consumesAction(), "Chinese Food cup placement not handled");
            helper.assertValueEqual(stack.getCount(), 1, "placement must consume exactly one cup");
            helper.assertValueEqual(tray.cups().get(slot).slot(), slot, "placed cup slot");
            helper.assertTrue(ItemStack.isSameItemSameComponents(stack, tray.cups().get(slot).tea()),
                    "placement changed the tea identity or item model");
        }
        TeaTrayBlockEntity synced = new TeaTrayBlockEntity(tray.getBlockPos(), tray.getBlockState());
        synced.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(), tray.getUpdateTag(helper.getLevel().registryAccess())));
        helper.assertValueEqual(synced.cups().size(), teas.size(), "synced cup count");
        for (int slot = 0; slot < teas.size(); slot++) {
            helper.assertValueEqual(synced.cups().get(slot).slot(), slot, "synced cup slot");
            helper.assertTrue(ItemStack.isSameItemSameComponents(tray.cups().get(slot).tea(),
                    synced.cups().get(slot).tea()), "client update lost tea identity or item model");
        }
        helper.succeed();
    }

    @GameTest
    public void mixedTeasFillFourSlotsAndRejectFifthWithoutConsuming(GameTestHelper helper) {
        TeaTrayBlockEntity tray = place(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(tray.isEmpty(), "new tray must be empty");
        List<Identifier> teas = List.of(TeacupRegistry.BARLEY_TEA, TeacupRegistry.OOLONG,
                TeacupRegistry.FLOWER_TEA, TeacupRegistry.MYSTERY_TEA);
        for (int slot = 0; slot < teas.size(); slot++) {
            ItemStack stack = tea(teas.get(slot));
            stack.setCount(2);
            helper.assertTrue(useAt(helper, player, stack, hitSlot(tray.getBlockPos(), slot, Direction.NORTH))
                    .consumesAction(), "tea placement not handled");
            helper.assertValueEqual(stack.getCount(), 1, "placement must consume exactly one cup");
            TrayTeacup placed = tray.cups().get(slot);
            helper.assertValueEqual(placed.slot(), slot, "slot order");
            helper.assertTrue(placed.tea().is(stack.getItem()), "mixed tea identity was lost");
        }
        ItemStack extra = tea(TeacupRegistry.BILUOCHUN);
        helper.assertTrue(use(helper, player, extra).consumesAction(), "full tray must consume interaction");
        helper.assertValueEqual(extra.getCount(), 1, "full tray consumed tea");
        helper.assertValueEqual(tray.cups().size(), 4, "capacity");
        helper.succeed();
    }

    @GameTest
    public void drinkingUsesTeaEffectsAndReturnsCupsInFifoOrder(GameTestHelper helper) {
        TeaTrayBlockEntity tray = place(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        tray.insert(tea(TeacupRegistry.OOLONG), 0);
        tray.insert(tea(TeacupRegistry.MYSTERY_TEA), 1);
        helper.assertTrue(use(helper, player, ItemStack.EMPTY).consumesAction(), "drink not handled");
        helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING) && player.hasEffect(MobEffects.JUMP_BOOST),
                "oolong effects missing");
        helper.assertTrue(!player.hasEffect(MobEffects.BLINDNESS), "wrong tea was drunk first");
        helper.assertValueEqual(player.getEffect(MobEffects.SLOW_FALLING).getDuration(), 6 * 60 * 20,
                "oolong duration");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "returned cup blocks further empty-hand drinking");
        use(helper, player, ItemStack.EMPTY);
        helper.assertTrue(player.hasEffect(MobEffects.BLINDNESS), "mystery tea effect missing");
        helper.assertValueEqual(player.getEffect(MobEffects.BLINDNESS).getDuration(), 15 * 20, "mystery duration");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.EMPTY_CUP), 2, "returned empty cups");
        helper.assertTrue(tray.isEmpty(), "drinking did not free slots");
        helper.assertBlockPresent(KHBlocks.TEA_TRAY, TARGET);
        helper.assertTrue(!use(helper, player, ItemStack.EMPTY).consumesAction(), "empty tray consumed interaction");
        helper.succeed();
    }

    @GameTest
    public void refillUsesVacantSlotWithoutChangingExistingCupsOrFifoOrder(GameTestHelper helper) {
        TeaTrayBlockEntity tray = place(helper);
        tray.insert(tea(TeacupRegistry.OOLONG), 0);
        tray.insert(tea(TeacupRegistry.MYSTERY_TEA), 1);
        tray.removeFirst();
        tray.insert(tea(TeacupRegistry.FLOWER_TEA), 0);
        helper.assertValueEqual(tray.cups().getFirst().slot(), 1, "existing cup moved");
        helper.assertValueEqual(tray.cups().getLast().slot(), 0, "vacant slot was not reused");
        helper.assertTrue(tray.removeFirst().is(TeacupRegistry.getItem(TeacupRegistry.MYSTERY_TEA)),
                "newly inserted tea jumped the drinking queue");
        helper.assertTrue(tray.removeFirst().is(TeacupRegistry.getItem(TeacupRegistry.FLOWER_TEA)), "refill lost");
        helper.succeed();
    }

    @GameTest
    public void saveAndUpdateTagPreserveSlotsOrderAndItemComponents(GameTestHelper helper) {
        TeaTrayBlockEntity tray = place(helper);
        tray.insert(tea(TeacupRegistry.OOLONG), 0);
        tray.insert(tea(TeacupRegistry.MYSTERY_TEA), 1);
        tray.removeFirst();
        ItemStack named = tea(TeacupRegistry.FLOWER_TEA);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Afternoon tea"));
        tray.insert(named, 0);
        named.setCount(0);
        var tag = tray.getUpdateTag(helper.getLevel().registryAccess());
        TeaTrayBlockEntity reloaded = new TeaTrayBlockEntity(tray.getBlockPos(), tray.getBlockState());
        reloaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(), tag));
        helper.assertValueEqual(reloaded.cups().size(), 2, "reloaded cup count");
        helper.assertValueEqual(reloaded.cups().getFirst().slot(), 1, "reloaded oldest slot");
        helper.assertValueEqual(reloaded.cups().getLast().slot(), 0, "reloaded refill slot");
        helper.assertTrue(reloaded.removeFirst().is(TeacupRegistry.getItem(TeacupRegistry.MYSTERY_TEA)), "saved order");
        helper.assertValueEqual(reloaded.removeFirst().get(DataComponents.CUSTOM_NAME),
                Component.literal("Afternoon tea"), "tea item components");
        helper.assertTrue(tray.getUpdatePacket() != null, "missing client update packet");
        helper.succeed();
    }

    @GameTest
    public void breakingDropsTrayAndEachRemainingTeaExactlyOnce(GameTestHelper helper) {
        TeaTrayBlockEntity tray = place(helper);
        tray.insert(tea(TeacupRegistry.OOLONG), 0);
        tray.insert(tea(TeacupRegistry.FLOWER_TEA), 1);
        List<ItemStack> drops = Block.getDrops(tray.getBlockState(), helper.getLevel(), tray.getBlockPos(), tray);
        helper.assertValueEqual(drops.size(), 3, "drop count");
        helper.assertValueEqual(drops.stream().filter(stack -> stack.is(KHItems.TEA_TRAY))
                .mapToInt(ItemStack::getCount).sum(), 1, "tray drop count");
        helper.assertValueEqual(drops.stream().filter(stack -> stack.is(TeacupRegistry.getItem(TeacupRegistry.OOLONG)))
                .mapToInt(ItemStack::getCount).sum(), 1, "oolong drop count");
        helper.assertValueEqual(drops.stream().filter(stack -> stack.is(TeacupRegistry.getItem(TeacupRegistry.FLOWER_TEA)))
                .mapToInt(ItemStack::getCount).sum(), 1, "flower tea drop count");
        helper.succeed();
    }

    @GameTest
    public void creativePlacementAndDrinkingDoNotDuplicateTea(GameTestHelper helper) {
        TeaTrayBlockEntity tray = place(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack tea = tea(TeacupRegistry.OOLONG);
        use(helper, player, tea);
        helper.assertValueEqual(tea.getCount(), 1, "creative placement consumed the held item");
        use(helper, player, ItemStack.EMPTY);
        helper.assertTrue(tray.isEmpty() && player.hasEffect(MobEffects.SLOW_FALLING), "creative drinking failed");
        helper.assertValueEqual(player.getInventory().countItem(TeacupRegistry.getItem(TeacupRegistry.OOLONG)),
                0, "creative drinking returned an extra full cup");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.EMPTY_CUP), 1, "creative cup duplicated");
        helper.succeed();
    }

    @GameTest
    public void unsupportedItemsAndOffhandEmptyUseLeaveTeaUntouched(GameTestHelper helper) {
        TeaTrayBlockEntity tray = place(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        tray.insert(tea(TeacupRegistry.OOLONG), 0);
        ItemStack stone = new ItemStack(Items.STONE);
        helper.assertTrue(!use(helper, player, stone).consumesAction(), "non-tea item accepted");
        helper.assertTrue(!TeaTrayBlockEntity.isTea(new ItemStack(ModItems.EMPTY_CUP)), "empty cup treated as tea");
        BlockPos pos = tray.getBlockPos();
        InteractionResult result = block().useItemOn(ItemStack.EMPTY, tray.getBlockState(), helper.getLevel(),
                pos, player, InteractionHand.OFF_HAND, hit(pos));
        helper.assertTrue(!result.consumesAction() && tray.cups().size() == 1, "offhand consumed another cup");
        helper.succeed();
    }

    @GameTest
    public void aimedPlacementSelectsEverySlotInEveryFacingWithoutFallback(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(TARGET);
        int[] order = {3, 0, 2, 1};
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            helper.getLevel().setBlockAndUpdate(pos, KHBlocks.TEA_TRAY.defaultBlockState()
                    .setValue(TeaTrayBlock.FACING, facing));
            TeaTrayBlockEntity tray = (TeaTrayBlockEntity) helper.getLevel().getBlockEntity(pos);
            while (!tray.isEmpty()) tray.removeFirst();
            for (int slot : order) {
                ItemStack stack = slot % 2 == 0 ? new ItemStack(ModItems.EMPTY_CUP, 2) : tea(TeacupRegistry.OOLONG);
                useAt(helper, player, stack, hitSlot(pos, slot, facing));
                helper.assertValueEqual(tray.cups().getLast().slot(), slot, "aimed placement slot for " + facing);
                helper.assertTrue(slot % 2 == 0 ? stack.getCount() == 1 : stack.isEmpty(),
                        "placement did not consume one cup");
                int count = tray.cups().size();
                ItemStack rejected = tea(TeacupRegistry.FLOWER_TEA);
                helper.assertTrue(useAt(helper, player, rejected, hitSlot(pos, slot, facing)).consumesAction(),
                        "occupied target did not block item use");
                helper.assertValueEqual(rejected.getCount(), 1, "occupied slot consumed held tea");
                helper.assertValueEqual(tray.cups().size(), count, "occupied slot fell back to another slot");
            }
            for (int slot : order) {
                helper.assertValueEqual(tray.cups().getFirst().slot(), slot, "placement changed FIFO order");
                tray.removeFirst();
            }
        }
        helper.succeed();
    }

    @GameTest
    public void emptyCupsPersistDropAndCanBeRetrievedWithoutDrinking(GameTestHelper helper) {
        TeaTrayBlockEntity tray = place(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack emptyCup = new ItemStack(ModItems.EMPTY_CUP, 2);
        emptyCup.set(DataComponents.CUSTOM_NAME, Component.literal("My cup"));
        useAt(helper, player, emptyCup, hitSlot(tray.getBlockPos(), 2, Direction.NORTH));
        helper.assertValueEqual(emptyCup.getCount(), 1, "empty cup placement consumption");
        tray.insert(tea(TeacupRegistry.OOLONG), 0);

        var saved = tray.getUpdateTag(helper.getLevel().registryAccess());
        tray.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(), saved));
        helper.assertValueEqual(tray.cups().getFirst().slot(), 2, "empty cup slot not preserved");
        helper.assertTrue(tray.cups().getFirst().tea().is(ModItems.EMPTY_CUP), "empty cup not preserved");
        List<ItemStack> drops = Block.getDrops(tray.getBlockState(), helper.getLevel(), tray.getBlockPos(), tray);
        helper.assertValueEqual(drops.stream().filter(stack -> stack.is(ModItems.EMPTY_CUP))
                .mapToInt(ItemStack::getCount).sum(), 1, "empty cup drop count");

        use(helper, player, ItemStack.EMPTY);
        helper.assertValueEqual(tray.cups().size(), 1, "retrieving empty cup consumed tea too");
        helper.assertTrue(!player.hasEffect(MobEffects.SLOW_FALLING), "empty cup triggered tea effect");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.EMPTY_CUP), 1, "cup not returned");
        helper.assertTrue(player.getInventory().contains(stack -> stack.is(ModItems.EMPTY_CUP)
                        && Component.literal("My cup").equals(stack.get(DataComponents.CUSTOM_NAME))),
                "retrieved empty cup lost its components");
        use(helper, player, ItemStack.EMPTY);
        helper.assertTrue(tray.isEmpty() && player.hasEffect(MobEffects.SLOW_FALLING), "next tea cannot be drunk");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.EMPTY_CUP), 2, "drinking lost its empty cup");
        helper.succeed();
    }

    @GameTest
    public void creativeEmptyCupPlacementDoesNotConsumeHeldStack(GameTestHelper helper) {
        TeaTrayBlockEntity tray = place(helper);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack emptyCup = new ItemStack(ModItems.EMPTY_CUP);
        useAt(helper, player, emptyCup, hitSlot(tray.getBlockPos(), 1, Direction.NORTH));
        helper.assertValueEqual(emptyCup.getCount(), 1, "creative empty cup consumed");
        helper.assertValueEqual(tray.cups().getFirst().slot(), 1, "creative placement ignored aim");
        use(helper, player, ItemStack.EMPTY);
        helper.assertTrue(tray.isEmpty(), "creative empty cup not retrieved");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.EMPTY_CUP), 1, "creative retrieval duplicated cup");
        helper.succeed();
    }

    @GameTest
    public void shiftRightClickPicksUpWholeTeaThroughItemInteraction(GameTestHelper helper) {
        verifyShiftPickup(helper, false);
    }

    @GameTest
    public void shiftRightClickPicksUpWholeTeaThroughEmptyHandInteraction(GameTestHelper helper) {
        verifyShiftPickup(helper, true);
    }

    private static void verifyShiftPickup(GameTestHelper helper, boolean withoutItem) {
        for (GameType mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
            TeaTrayBlockEntity tray = place(helper);
            Player player = helper.makeMockPlayer(mode);
            ItemStack first = tea(TeacupRegistry.OOLONG);
            Component name = Component.literal("Tea to take away");
            first.set(DataComponents.CUSTOM_NAME, name);
            tray.insert(tea(TeacupRegistry.MYSTERY_TEA), 0);
            tray.insert(first, 3);
            player.setShiftKeyDown(true);
            BlockPos pos = tray.getBlockPos();
            InteractionResult result = withoutItem
                    ? block().useWithoutItem(tray.getBlockState(), helper.getLevel(), pos, player,
                            hitSlot(pos, 3, Direction.NORTH))
                    : useAt(helper, player, ItemStack.EMPTY, hitSlot(pos, 3, Direction.NORTH));
            helper.assertTrue(result.consumesAction(), "shift pickup not handled");
            helper.assertTrue(player.getMainHandItem().is(first.getItem()), "whole tea not put in main hand");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "pickup stack size");
            helper.assertValueEqual(player.getMainHandItem().get(DataComponents.CUSTOM_NAME), name,
                    "pickup lost item components");
            helper.assertTrue(!player.hasEffect(MobEffects.SLOW_FALLING)
                    && !player.hasEffect(MobEffects.JUMP_BOOST) && !player.hasEffect(MobEffects.BLINDNESS),
                    "pickup incorrectly applied drinking effects");
            helper.assertValueEqual(player.getInventory().countItem(ModItems.EMPTY_CUP), 0,
                    "pickup generated an empty cup");
            helper.assertValueEqual(tray.cups().size(), 1, "pickup removed more than one cup");
            helper.assertValueEqual(tray.cups().getFirst().slot(), 0, "pickup moved the remaining cup");
            helper.assertTrue(!block().useWithoutItem(tray.getBlockState(), helper.getLevel(), pos, player, hit(pos))
                    .consumesAction(), "empty-hand fallback took another cup with an occupied hand");

            player.setShiftKeyDown(false);
            use(helper, player, ItemStack.EMPTY);
            helper.assertTrue(player.hasEffect(MobEffects.BLINDNESS), "ordinary right click no longer drinks tea");
            helper.assertTrue(tray.isEmpty(), "ordinary drinking did not empty tray");
        }
        helper.succeed();
    }

    @GameTest
    public void shiftRightClickRetrievesEmptyCupAndEmptyTrayDoesNothing(GameTestHelper helper) {
        TeaTrayBlockEntity tray = place(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        tray.insert(new ItemStack(ModItems.EMPTY_CUP), 1);
        player.setShiftKeyDown(true);
        useAt(helper, player, ItemStack.EMPTY, hitSlot(tray.getBlockPos(), 1, Direction.NORTH));
        helper.assertTrue(player.getMainHandItem().is(ModItems.EMPTY_CUP), "shift pickup lost empty cup");
        helper.assertValueEqual(player.getInventory().countItem(ModItems.EMPTY_CUP), 1, "empty cup duplicated");
        helper.assertTrue(tray.isEmpty(), "empty cup slot not cleared");
        helper.assertTrue(!use(helper, player, ItemStack.EMPTY).consumesAction(), "empty tray consumed interaction");
        helper.succeed();
    }

    @GameTest
    public void aimedPickupSelectsEverySlotInEveryFacingAndPreservesRemainingOrder(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(TARGET);
        int[] insertionOrder = {3, 0, 2, 1};
        int[] pickupOrder = {2, 1, 3, 0};
        for (GameType mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                for (boolean withoutItem : new boolean[] {false, true}) {
                    helper.getLevel().setBlockAndUpdate(pos, KHBlocks.TEA_TRAY.defaultBlockState()
                            .setValue(TeaTrayBlock.FACING, facing));
                    TeaTrayBlockEntity tray = (TeaTrayBlockEntity) helper.getLevel().getBlockEntity(pos);
                    Player player = helper.makeMockPlayer(mode);
                    player.setShiftKeyDown(true);
                    for (int slot : insertionOrder) {
                        ItemStack stack = slot % 2 == 0 ? new ItemStack(ModItems.EMPTY_CUP) : tea(TeacupRegistry.OOLONG);
                        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Cup " + slot));
                        tray.insert(stack, slot);
                    }
                    int occupied = (1 << TeaTrayLayout.CAPACITY) - 1;
                    for (int slot : pickupOrder) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        BlockHitResult hit = hitSlot(pos, slot, facing);
                        InteractionResult result = withoutItem
                                ? block().useWithoutItem(tray.getBlockState(), helper.getLevel(), pos, player, hit)
                                : useAt(helper, player, ItemStack.EMPTY, hit);
                        helper.assertTrue(result.consumesAction(), "aimed pickup not handled for " + facing);
                        ItemStack picked = player.getMainHandItem();
                        helper.assertTrue(picked.is(slot % 2 == 0 ? ModItems.EMPTY_CUP
                                : TeacupRegistry.getItem(TeacupRegistry.OOLONG)), "wrong cup picked");
                        helper.assertValueEqual(picked.get(DataComponents.CUSTOM_NAME),
                                Component.literal("Cup " + slot), "pickup ignored pointed slot for " + facing);
                        helper.assertValueEqual(picked.getCount(), 1, "pickup count");
                        helper.assertTrue(!player.hasEffect(MobEffects.SLOW_FALLING), "pickup drank tea");
                        occupied &= ~(1 << slot);
                        int index = 0;
                        for (int remaining : insertionOrder) {
                            if ((occupied & (1 << remaining)) != 0) {
                                helper.assertValueEqual(tray.cups().get(index++).slot(), remaining,
                                        "pickup moved or reordered remaining cups");
                            }
                        }
                        helper.assertValueEqual(tray.cups().size(), index, "wrong remaining cup count");
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        result = withoutItem
                                ? block().useWithoutItem(tray.getBlockState(), helper.getLevel(), pos, player, hit)
                                : useAt(helper, player, ItemStack.EMPTY, hit);
                        helper.assertTrue(!result.consumesAction(), "vacant slot consumed pickup");
                        helper.assertTrue(player.getMainHandItem().isEmpty(), "vacant slot picked another cup");
                        helper.assertValueEqual(tray.cups().size(), index, "vacant slot removed another cup");
                        helper.assertTrue(!player.hasEffect(MobEffects.SLOW_FALLING), "vacant slot drank tea");
                    }
                }
            }
        }
        helper.succeed();
    }

    private static TeaTrayBlockEntity place(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(pos, KHBlocks.TEA_TRAY.defaultBlockState());
        return (TeaTrayBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static ItemStack tea(Identifier id) {
        return TeacupRegistry.getItem(id).getDefaultInstance();
    }

    private static TeaTrayBlock block() {
        return (TeaTrayBlock) KHBlocks.TEA_TRAY;
    }

    private static InteractionResult use(GameTestHelper helper, Player player, ItemStack stack) {
        return useAt(helper, player, stack, hit(helper.absolutePos(TARGET)));
    }

    private static InteractionResult useAt(GameTestHelper helper, Player player, ItemStack stack, BlockHitResult hit) {
        BlockPos pos = helper.absolutePos(TARGET);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return block().useItemOn(stack, helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                player, InteractionHand.MAIN_HAND, hit);
    }

    private static BlockHitResult hitSlot(BlockPos pos, int slot, Direction facing) {
        double x = TeaTrayLayout.centerX(slot);
        double z = TeaTrayLayout.centerZ(slot);
        double worldX = switch (facing) {
            case EAST -> 1 - z;
            case SOUTH -> 1 - x;
            case WEST -> z;
            default -> x;
        };
        double worldZ = switch (facing) {
            case EAST -> x;
            case SOUTH -> 1 - z;
            case WEST -> 1 - x;
            default -> z;
        };
        return new BlockHitResult(new Vec3(pos.getX() + worldX, pos.getY() + TeaTrayLayout.SURFACE_Y,
                pos.getZ() + worldZ), Direction.UP, pos, false);
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
