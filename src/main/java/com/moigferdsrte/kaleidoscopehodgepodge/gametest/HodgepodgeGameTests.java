package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgePlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgeSoupBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.StackableFoodBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class HodgepodgeGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest
    public void blockEntityStoresVerticallyStackedIngredients(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        helper.assertTrue(feast.add(PackingIngredients.RED_BERRY, 8, 8).success(), "First ingredient must fit");
        helper.assertTrue(feast.add(PackingIngredients.RED_BERRY, 8, 8).success(), "Second ingredient must stack");
        helper.assertValueEqual(feast.ingredients().get(1).y(), 4, "second ingredient y");
        helper.succeed();
    }

    @GameTest
    public void itemSnapshotRestoresAfterPlacement(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        ItemStack stack = new ItemStack(KHBlocks.WOODEN_PLATE);
        PlacedIngredient ingredient = new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 8, 2, 8, 2, 2, 2);
        stack.set(KHDataComponents.CUSTOM_FEAST,
                new CustomFeastData(CustomFeastData.ContainerKind.DISH, Direction.NORTH, List.of(ingredient)));
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.WOODEN_PLATE.defaultBlockState());
        ((HodgepodgePlateBlock) KHBlocks.WOODEN_PLATE).setPlacedBy(helper.getLevel(), target,
                KHBlocks.WOODEN_PLATE.defaultBlockState(), null, stack);
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null && feast.ingredients().equals(List.of(ingredient)), "Snapshot was not restored");
        helper.succeed();
    }

    @GameTest
    public void wrappingBagAdvancesFoodBites(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "blaze_lamb_chop"));
        helper.assertTrue(block instanceof FoodBiteBlock, "Expected blaze lamb chop FoodBiteBlock");
        FoodBiteBlock food = (FoodBiteBlock) block;
        helper.getLevel().setBlockAndUpdate(target, food.defaultBlockState());
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        UseOnContext context = new UseOnContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, bag,
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
        ((WrappingBagItem) KHItems.WRAPPING_BAG).useOn(context);
        helper.assertValueEqual(helper.getLevel().getBlockState(target).getValue(food.getBites()), 1, "bites");
        helper.assertTrue(bag.has(KHDataComponents.PACKING_BAG_INGREDIENT), "Bag did not receive an ingredient");
        helper.succeed();
    }

    @GameTest
    public void wrappingBagDecrementsStackableFood(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "bamboo_tube_rice"));
        helper.assertTrue(block instanceof StackableFoodBlock, "Expected bamboo tube rice StackableFoodBlock");
        StackableFoodBlock food = (StackableFoodBlock) block;
        helper.getLevel().setBlockAndUpdate(target,
                food.defaultBlockState().setValue(food.getCountProperty(), food.getMaxCount()));
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        ((WrappingBagItem) KHItems.WRAPPING_BAG).useOn(new UseOnContext(helper.getLevel(), null,
                InteractionHand.MAIN_HAND, bag, new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false)));
        helper.assertValueEqual(helper.getLevel().getBlockState(target).getValue(food.getCountProperty()),
                food.getMaxCount() - 1, "count");
        helper.succeed();
    }

    @GameTest
    public void platePlacementConsumesBagComponent(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        bag.set(KHDataComponents.PACKING_BAG_INGREDIENT, PackingIngredients.RED_BERRY.getId().toString());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target).add(0, 0.5, 0),
                Direction.UP, target, false);
        InteractionResult result = ((HodgepodgePlateBlock) KHBlocks.PORCELAIN_PLATE).useItemOn(bag,
                helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, hit);
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(result.consumesAction(), "Placement did not consume the action");
        helper.assertTrue(!bag.has(KHDataComponents.PACKING_BAG_INGREDIENT), "Bag component was not cleared");
        helper.assertValueEqual(feast.ingredients().size(), 1, "ingredient count");
        helper.succeed();
    }

    @GameTest
    public void soupRejectsDishOnlyIngredient(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_SOUP_BOWL.defaultBlockState());
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        bag.set(KHDataComponents.PACKING_BAG_INGREDIENT, PackingIngredients.MUTTON.getId().toString());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InteractionResult result = ((HodgepodgeSoupBlock) KHBlocks.PORCELAIN_SOUP_BOWL).useItemOn(bag,
                helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(target).add(0, 0.5, 0),
                        Direction.UP, target, false));
        helper.assertTrue(result == InteractionResult.FAIL, "Soup should reject dish-only ingredient");
        helper.assertTrue(bag.has(KHDataComponents.PACKING_BAG_INGREDIENT), "Rejected placement consumed the bag");
        helper.succeed();
    }

    @GameTest
    public void creativeBreakDropsSnapshot(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.WOODEN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        feast.add(PackingIngredients.RED_BERRY, 8, 8);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        KHBlocks.WOODEN_PLATE.playerWillDestroy(helper.getLevel(), target,
                helper.getLevel().getBlockState(target), player);
        List<ItemEntity> drops = helper.getLevel().getEntities(EntityTypes.ITEM,
                new AABB(target).inflate(2.0), Entity::isAlive);
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().has(KHDataComponents.CUSTOM_FEAST)),
                "Creative drop did not preserve custom feast data");
        helper.succeed();
    }
}
