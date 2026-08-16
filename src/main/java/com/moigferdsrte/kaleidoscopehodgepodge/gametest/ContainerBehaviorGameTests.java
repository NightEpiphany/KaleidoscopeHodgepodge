package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgePlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.FeastIngredientsTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class ContainerBehaviorGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void itemSnapshotRestoresAfterPlacement(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        ItemStack stack = new ItemStack(KHBlocks.WOODEN_PLATE);
        PlacedIngredient ingredient = new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 8, 2, 8, 2, 2, 2);
        stack.set(KHDataComponents.CUSTOM_FEAST,
                new CustomFeastData(CustomFeastData.ContainerKind.DISH, Direction.NORTH, List.of(ingredient)));
        FeastIngredientsTooltip tooltip = (FeastIngredientsTooltip)
                ((CustomFeastBlockItem) KHItems.WOODEN_PLATE).getTooltipImage(stack).orElseThrow();
        helper.assertValueEqual(tooltip.ingredientIds(), List.of(PackingIngredients.RED_BERRY.getId()),
                "custom feast tooltip ingredients");
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.WOODEN_PLATE.defaultBlockState());
        ((HodgepodgePlateBlock) KHBlocks.WOODEN_PLATE).setPlacedBy(helper.getLevel(), target,
                KHBlocks.WOODEN_PLATE.defaultBlockState(), null, stack);
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null && feast.ingredients().equals(List.of(ingredient)), "Snapshot was not restored");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void emptyFeastsDropInSurvivalOnly(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        AABB dropArea = new AABB(target).inflate(2.0);

        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        helper.getLevel().destroyBlock(target, true);
        List<ItemEntity> survivalDrops = helper.getLevel().getEntities(EntityType.ITEM, dropArea, Entity::isAlive);
        helper.assertTrue(survivalDrops.stream().anyMatch(drop -> drop.getItem().is(KHItems.PORCELAIN_PLATE)),
                "Empty feast did not drop its item in survival");
        survivalDrops.forEach(Entity::discard);

        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_SOUP_BOWL.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        KHBlocks.PORCELAIN_SOUP_BOWL.playerWillDestroy(helper.getLevel(), target,
                helper.getLevel().getBlockState(target), player);
        List<ItemEntity> creativeDrops = helper.getLevel().getEntities(EntityType.ITEM, dropArea, Entity::isAlive);
        helper.assertTrue(creativeDrops.isEmpty(), "Empty feast dropped an item in creative");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void containerCapacityMatchesMaterial(GameTestHelper helper) {
        assertCapacity(helper, KHBlocks.WOODEN_PLATE, 20);
        assertCapacity(helper, KHBlocks.PORCELAIN_PLATE, 40);
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void lunchBoxAcceptsOnlyWrappingBags(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack lunchBox = KHItems.LUNCH_BOX.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, lunchBox);
        LunchBoxMenu menu = new LunchBoxMenu(1, player.getInventory(), lunchBox, InteractionHand.MAIN_HAND);
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        bag.set(KHDataComponents.PACKING_BAG_INGREDIENT, PackingIngredients.RED_BERRY.getId().toString());

        helper.assertTrue(menu.slots.getFirst().mayPlace(bag), "Filled bag should be accepted");
        ItemStack emptyBag = KHItems.WRAPPING_BAG.getDefaultInstance();
        helper.assertTrue(menu.slots.getFirst().mayPlace(emptyBag), "Empty bag should be accepted");
        helper.assertTrue(!menu.slots.getFirst().mayPlace(Items.STONE.getDefaultInstance()),
                "Non-bag item should be rejected");
        LunchBoxMenu clientMenu = new LunchBoxMenu(2, player.getInventory());
        helper.assertTrue(!clientMenu.slots.getFirst().mayPlace(Items.STONE.getDefaultInstance()),
                "Client menu should reject non-bag items immediately");
        menu.slots.getFirst().set(bag);
        menu.slots.get(1).set(emptyBag);
        menu.removed(player);

        ItemContainerContents contents = lunchBox.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        helper.assertValueEqual((int) contents.nonEmptyStream().count(), 2, "lunch box item count");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void creativeBreakDropsSnapshot(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.WOODEN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        feast.add(PackingIngredients.RED_BERRY, 8, 8);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        KHBlocks.WOODEN_PLATE.playerWillDestroy(helper.getLevel(), target,
                helper.getLevel().getBlockState(target), player);
        List<ItemEntity> drops = helper.getLevel().getEntities(EntityType.ITEM,
                new AABB(target).inflate(2.0), Entity::isAlive);
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().has(KHDataComponents.CUSTOM_FEAST)),
                "Creative drop did not preserve custom feast data");
        helper.succeed();
    }

    private static void assertCapacity(GameTestHelper helper, Block block, int expected) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, block.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        assert feast != null;
        for (int index = 0; index < expected; index++) {
            int x = 1 + (index % 8) * 2;
            int z = 1 + (index / 8) * 2;
            helper.assertTrue(feast.add(PackingIngredients.RED_BERRY, x, z).success(),
                    "Ingredient " + index + " should fit");
        }
        helper.assertTrue(!feast.add(PackingIngredients.RED_BERRY, 15, 15).success(),
                "Container accepted more than " + expected + " ingredients");
    }
}
