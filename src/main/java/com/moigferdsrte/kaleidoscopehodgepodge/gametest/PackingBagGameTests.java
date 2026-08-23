package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.PlateBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.StackableFoodBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgePlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor.PlateBlockAccessor;
import com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor.StackableFoodBlockAccessor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.stream.Collectors;

public final class PackingBagGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void wrappingBagPacksWholeFoodBiteDish(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "blaze_lamb_chop"));
        helper.assertTrue(block instanceof FoodBiteBlock, "Expected blaze lamb chop FoodBiteBlock");
        assert block instanceof FoodBiteBlock;
        FoodBiteBlock food = (FoodBiteBlock) block;
        helper.getLevel().setBlockAndUpdate(target,
                food.defaultBlockState().setValue(food.getBites(), 1));
        ItemStack bittenDishBag = KHItems.WRAPPING_BAG.getDefaultInstance();
        InteractionResult bittenResult = ((WrappingBagItem) KHItems.WRAPPING_BAG).useOn(new UseOnContext(
                helper.getLevel(), null, InteractionHand.MAIN_HAND, bittenDishBag,
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false)));
        helper.assertTrue(bittenResult == InteractionResult.FAIL, "Bitten FoodBiteBlock was packed");
        helper.assertTrue(PackingBagService.get(bittenDishBag).isEmpty(), "Rejected packing filled the bag");
        helper.assertValueEqual(helper.getLevel().getBlockState(target).getValue(food.getBites()), 1,
                "rejected dish bites");

        helper.getLevel().setBlockAndUpdate(target, food.defaultBlockState());
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        InteractionResult result = ((WrappingBagItem) KHItems.WRAPPING_BAG).useOn(new UseOnContext(
                helper.getLevel(), null, InteractionHand.MAIN_HAND, bag,
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false)));
        PackingBagContents contents = PackingBagService.get(bag);
        Map<ResourceLocation, Long> counts = contents.ingredients().stream()
                .collect(Collectors.groupingBy(BaggedIngredient::id, Collectors.counting()));
        helper.assertTrue(result.consumesAction(), "Whole dish packing did not consume the action");
        helper.assertValueEqual(contents.ingredients().size(), 8, "packed ingredient count");
        helper.assertValueEqual(counts.get(PackingIngredients.RED_BERRY.getId()), 1L, "red berry count");
        helper.assertValueEqual(counts.get(PackingIngredients.MUTTON.getId()), 1L, "mutton count");
        helper.assertValueEqual(counts.get(PackingIngredients.ARDENT_CORE.getId()), 2L, "ardent core count");
        helper.assertValueEqual(counts.get(PackingIngredients.BLAZE_ROD.getId()), 4L, "blaze rod count");
        helper.assertTrue(!helper.getLevel().getBlockState(target).is(block), "Packed whole dish remained");

        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PackingBagService.setMode(bag, PackingBagMode.PLACEMENT);
        ((HodgepodgePlateBlock) KHBlocks.PORCELAIN_PLATE).useItemOn(bag,
                helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(target).add(0, 0.5, 0),
                        Direction.UP, target, false));
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertValueEqual(PackingBagService.get(bag).ingredients().size(), 7, "remaining bag contents");
        helper.assertValueEqual(feast.ingredients().getFirst().id(), PackingIngredients.RED_BERRY.getId(),
                "first placed whole-dish ingredient");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void wrappingBagPacksWholePlateThroughBlockInteraction(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "fruit_platter"));
        helper.assertTrue(block instanceof PlateBlock, "Expected fruit platter PlateBlock");
        assert block instanceof PlateBlock;
        PlateBlock plate = (PlateBlock) block;
        PlateBlockAccessor plateAccessor = (PlateBlockAccessor) plate;
        BlockState wholePlate = plate.defaultBlockState().setValue(
                plateAccessor.kaleidoscopeHodgepodge$getServings(), plateAccessor.kaleidoscopeHodgepodge$getMaxCount());
        helper.getLevel().setBlockAndUpdate(target, wholePlate);

        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        ItemInteractionResult result = helper.getLevel().getBlockState(target).useItemOn(
                bag, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);

        helper.assertTrue(result.consumesAction(), "Plate interaction did not pack the dish");
        helper.assertTrue(!PackingBagService.get(bag).isEmpty(), "Packed plate left the wrapping bag empty");
        helper.assertTrue(helper.getLevel().getBlockState(target).isAir(), "Packed plate remained in the world");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void wrappingBagOverridesFoodBiteEating(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "blaze_lamb_chop"));
        helper.assertTrue(block instanceof FoodBiteBlock, "Expected blaze lamb chop FoodBiteBlock");
        FoodBiteBlock food = (FoodBiteBlock) block;
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);

        helper.getLevel().setBlockAndUpdate(target, food.defaultBlockState());
        ItemStack storageBag = KHItems.WRAPPING_BAG.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, storageBag);
        InteractionResult packed = food.useWithoutItem(helper.getLevel().getBlockState(target),
                helper.getLevel(), target, player, hit);
        helper.assertTrue(packed.consumesAction(), "FoodBiteBlock right click did not pack the dish");
        helper.assertTrue(!PackingBagService.get(storageBag).isEmpty(), "Right click left the wrapping bag empty");
        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 10, "Packing triggered eat");
        helper.assertTrue(!helper.getLevel().getBlockState(target).is(food), "Packed dish remained in the world");

        helper.getLevel().setBlockAndUpdate(target, food.defaultBlockState());
        ItemStack placementBag = KHItems.WRAPPING_BAG.getDefaultInstance();
        PackingBagService.setMode(placementBag, PackingBagMode.PLACEMENT);
        player.setItemInHand(InteractionHand.MAIN_HAND, placementBag);
        InteractionResult blocked = food.useWithoutItem(helper.getLevel().getBlockState(target),
                helper.getLevel(), target, player, hit);
        helper.assertTrue(blocked == InteractionResult.FAIL, "Placement mode reached FoodBiteBlock eat");
        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 10, "Blocked paper bag interaction triggered eat");
        helper.assertValueEqual(helper.getLevel().getBlockState(target).getValue(food.getBites()), 0,
                "Blocked paper bag interaction consumed a bite");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void wrappingBagDecrementsStackableFood(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "bamboo_tube_rice"));
        helper.assertTrue(block instanceof StackableFoodBlock, "Expected bamboo tube rice StackableFoodBlock");
        assert block instanceof StackableFoodBlock;
        StackableFoodBlock food = (StackableFoodBlock) block;
        StackableFoodBlockAccessor foodAccessor = (StackableFoodBlockAccessor) food;
        helper.getLevel().setBlockAndUpdate(target,
                food.defaultBlockState().setValue(foodAccessor.kaleidoscopeHodgepodge$getCountProperty(),
                        foodAccessor.kaleidoscopeHodgepodge$getMaxCount()));
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        ((WrappingBagItem) KHItems.WRAPPING_BAG).useOn(new UseOnContext(helper.getLevel(), null,
                InteractionHand.MAIN_HAND, bag, new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false)));
        helper.assertValueEqual(helper.getLevel().getBlockState(target)
                        .getValue(foodAccessor.kaleidoscopeHodgepodge$getCountProperty()),
                foodAccessor.kaleidoscopeHodgepodge$getMaxCount() - 1, "count");
        helper.assertValueEqual(PackingBagService.get(bag).first().orElseThrow().id(),
                PackingIngredients.BAMBOO_TUBE_RICE.getId(), "stackable ingredient");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void wrappingBagPacksWholeVanillaCakeWithoutEating(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);

        helper.getLevel().setBlockAndUpdate(target, Blocks.CAKE.defaultBlockState());
        ItemInteractionResult route = helper.getLevel().getBlockState(target).useItemOn(bag,
                helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(route == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                "Cake interaction did not route through useWithoutItem");
        InteractionResult packed = helper.getLevel().getBlockState(target)
                .useWithoutItem(helper.getLevel(), player, hit);

        helper.assertTrue(packed.consumesAction(), "Whole cake was not packed");
        helper.assertValueEqual(PackingBagService.get(bag).first().orElseThrow().id(),
                PackingIngredients.CAKE.getId(), "packed cake ingredient");
        helper.assertTrue(helper.getLevel().getBlockState(target).isAir(), "Packed cake remained in the world");
        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 10, "Packing cake triggered eating");

        ItemStack secondBag = KHItems.WRAPPING_BAG.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, secondBag);
        helper.getLevel().setBlockAndUpdate(target,
                Blocks.CAKE.defaultBlockState().setValue(CakeBlock.BITES, 1));
        InteractionResult rejected = helper.getLevel().getBlockState(target)
                .useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(rejected == InteractionResult.FAIL, "Partially eaten cake was packed");
        helper.assertTrue(PackingBagService.get(secondBag).isEmpty(), "Rejected cake filled the bag");
        helper.assertValueEqual(helper.getLevel().getBlockState(target).getValue(CakeBlock.BITES), 1,
                "Rejected cake lost a bite");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void packingBagModesSeparateStorageAndPlacement(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        assert feast != null;
        helper.assertTrue(feast.add(PackingIngredients.RED_BERRY, 8, 8).success(), "Ingredient setup failed");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(target.getX() + 0.5, target.getY() + 2.0, target.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(target.getX() + 0.5, target.getY() + 2.0 / 16.0, target.getZ() + 0.5),
                Direction.UP, target, false);
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        PackingBagService.setMode(bag, PackingBagMode.PLACEMENT);

        InteractionResult emptyPlacement = ((HodgepodgePlateBlock) KHBlocks.PORCELAIN_PLATE).useItemOn(bag,
                helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, hit).result();
        helper.assertTrue(emptyPlacement == InteractionResult.FAIL, "Empty placement bag was accepted");
        helper.assertValueEqual(feast.ingredients().size(), 1, "Empty placement changed feast contents");

        PackingBagService.setMode(bag, PackingBagMode.STORAGE);
        InteractionResult storage = ((HodgepodgePlateBlock) KHBlocks.PORCELAIN_PLATE).useItemOn(bag,
                helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, hit).result();
        helper.assertTrue(storage.consumesAction(), "Storage mode did not retrieve targeted ingredient");
        helper.assertValueEqual(PackingBagService.get(bag).ingredients().size(), 1, "Stored ingredient count");
        helper.assertTrue(feast.ingredients().isEmpty(), "Stored ingredient remained on plate");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void shiftRightClickTogglesPackingBagMode(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        player.setShiftKeyDown(true);

        ((WrappingBagItem) KHItems.WRAPPING_BAG).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(PackingBagService.getMode(bag), PackingBagMode.PLACEMENT,
                "First mode switch");
        ((WrappingBagItem) KHItems.WRAPPING_BAG).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(PackingBagService.getMode(bag), PackingBagMode.STORAGE,
                "Second mode switch");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void fullStorageBagDoesNotRemoveTarget(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        assert feast != null;
        helper.assertTrue(feast.add(PackingIngredients.RED_BERRY, 8, 8).success(), "Ingredient setup failed");

        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        PackingBagService.set(bag, new PackingBagContents(java.util.Collections.nCopies(
                PackingBagContents.MAX_INGREDIENTS, new BaggedIngredient(PackingIngredients.RED_BERRY.getId()))));
        PackingBagService.setMode(bag, PackingBagMode.STORAGE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(target.getX() + 0.5, target.getY() + 2.0, target.getZ() + 0.5);
        InteractionResult result = ((HodgepodgePlateBlock) KHBlocks.PORCELAIN_PLATE).useItemOn(bag,
                helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, new BlockHitResult(
                        new Vec3(target.getX() + 0.5, target.getY() + 2.0 / 16.0, target.getZ() + 0.5),
                        Direction.UP, target, false)).result();

        helper.assertTrue(result == InteractionResult.FAIL, "Full storage bag accepted another ingredient");
        helper.assertValueEqual(feast.ingredients().size(), 1, "Full storage bag removed its target");
        helper.assertValueEqual(PackingBagService.get(bag).ingredients().size(), 9, "Full bag content count");
        helper.succeed();
    }
}
