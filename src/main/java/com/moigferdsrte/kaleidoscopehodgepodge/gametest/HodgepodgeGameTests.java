package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgePlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgeSoupBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientPlacementTarget;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.FeastIngredientsTooltip;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.StackableFoodBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HodgepodgeGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest
    public void blockEntityStoresVerticallyStackedIngredients(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        assert feast != null;
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

    @GameTest
    public void wrappingBagPacksWholeFoodBiteDish(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "blaze_lamb_chop"));
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
        Map<Identifier, Long> counts = contents.ingredients().stream()
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

    @GameTest
    public void wrappingBagOverridesFoodBiteEating(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "blaze_lamb_chop"));
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

    @GameTest
    public void wrappingBagDecrementsStackableFood(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "bamboo_tube_rice"));
        helper.assertTrue(block instanceof StackableFoodBlock, "Expected bamboo tube rice StackableFoodBlock");
        assert block instanceof StackableFoodBlock;
        StackableFoodBlock food = (StackableFoodBlock) block;
        helper.getLevel().setBlockAndUpdate(target,
                food.defaultBlockState().setValue(food.getCountProperty(), food.getMaxCount()));
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        ((WrappingBagItem) KHItems.WRAPPING_BAG).useOn(new UseOnContext(helper.getLevel(), null,
                InteractionHand.MAIN_HAND, bag, new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false)));
        helper.assertValueEqual(helper.getLevel().getBlockState(target).getValue(food.getCountProperty()),
                food.getMaxCount() - 1, "count");
        helper.assertValueEqual(PackingBagService.get(bag).first().orElseThrow().id(),
                PackingIngredients.BAMBOO_TUBE_RICE.getId(), "stackable ingredient");
        helper.succeed();
    }

    @GameTest
    public void wrappingBagPacksWholeVanillaCakeWithoutEating(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(10);
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);

        helper.getLevel().setBlockAndUpdate(target, Blocks.CAKE.defaultBlockState());
        InteractionResult route = helper.getLevel().getBlockState(target).useItemOn(bag,
                helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(route == InteractionResult.TRY_WITH_EMPTY_HAND,
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
        helper.assertTrue(PackingBagService.get(bag).isEmpty(), "Bag contents were not cleared");
        assert feast != null;
        helper.assertValueEqual(feast.ingredients().size(), 1, "ingredient count");
        helper.succeed();
    }

    @GameTest
    public void emptyBagRetrievesTargetedIngredient(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        assert feast != null;
        helper.assertTrue(feast.add(PackingIngredients.RED_BERRY, 8, 8).success(),
                "Ingredient setup failed");
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(target.getX() + 0.5, target.getY() + 2.0, target.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(target.getX() + 0.5, target.getY() + 2.0 / 16.0, target.getZ() + 0.5),
                Direction.UP, target, false);

        InteractionResult result = ((HodgepodgePlateBlock) KHBlocks.PORCELAIN_PLATE).useItemOn(bag,
                helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, hit);

        helper.assertTrue(result.consumesAction(), "Retrieval did not consume the action");
        helper.assertValueEqual(PackingBagService.get(bag).first().orElseThrow().id(),
                PackingIngredients.RED_BERRY.getId(), "retrieved ingredient");
        helper.assertTrue(feast.ingredients().isEmpty(), "Retrieved ingredient remained in the feast");
        helper.succeed();
    }

    @GameTest
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
                InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(emptyPlacement == InteractionResult.FAIL, "Empty placement bag was accepted");
        helper.assertValueEqual(feast.ingredients().size(), 1, "Empty placement changed feast contents");

        PackingBagService.setMode(bag, PackingBagMode.STORAGE);
        InteractionResult storage = ((HodgepodgePlateBlock) KHBlocks.PORCELAIN_PLATE).useItemOn(bag,
                helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(storage.consumesAction(), "Storage mode did not retrieve targeted ingredient");
        helper.assertValueEqual(PackingBagService.get(bag).ingredients().size(), 1, "Stored ingredient count");
        helper.assertTrue(feast.ingredients().isEmpty(), "Stored ingredient remained on plate");
        helper.succeed();
    }

    @GameTest
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

    @GameTest
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
                        Direction.UP, target, false));

        helper.assertTrue(result == InteractionResult.FAIL, "Full storage bag accepted another ingredient");
        helper.assertValueEqual(feast.ingredients().size(), 1, "Full storage bag removed its target");
        helper.assertValueEqual(PackingBagService.get(bag).ingredients().size(), 9, "Full bag content count");
        helper.succeed();
    }

    @GameTest
    public void sideHitPlacesOutsideExistingIngredientBox(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        assert feast != null;
        helper.assertTrue(feast.add(PackingIngredients.MUTTON, 8, 8).success(), "Ingredient setup failed");
        PlacedIngredient existing = feast.ingredients().getFirst();
        double sideX = target.getX() + existing.xMin() / 32.0;
        Vec3 eye = new Vec3(target.getX() - 1.0, target.getY() + 3.0 / 16.0, target.getZ() + 0.5);
        BlockHitResult sideHit = new BlockHitResult(
                new Vec3(sideX, target.getY() + 3.0 / 16.0, target.getZ() + 0.5),
                Direction.WEST, target, false);

        IngredientPlacementTarget.Pixel placement = IngredientPlacementTarget.resolve(feast.renderIngredients(),
                target, eye, sideHit, PackingIngredients.RED_BERRY, 0).orElseThrow();
        PlacementSpace.Result result = feast.add(PackingIngredients.RED_BERRY, placement.x(), placement.z());
        helper.assertTrue(result.success(), "Side placement was rejected");
        PlacedIngredient placed = result.placement().orElseThrow();
        helper.assertValueEqual(placed.xMax(), existing.xMin(), "Side placement did not touch existing box");
        helper.assertTrue(!placed.intersects(existing), "Side placement overlaps existing ingredient");
        helper.assertTrue(helper.getLevel().getBlockState(target).getShape(helper.getLevel(), target)
                        .bounds().maxY > 2.0 / 16.0,
                "Ingredient size box is missing from the cached block shape");
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
        helper.assertTrue(!PackingBagService.get(bag).isEmpty(), "Rejected placement consumed the bag");
        InteractionResult bagResult = ((WrappingBagItem) KHItems.WRAPPING_BAG).useOn(new UseOnContext(
                helper.getLevel(), player, InteractionHand.MAIN_HAND, bag,
                new BlockHitResult(Vec3.atCenterOf(target).add(0, 0.5, 0), Direction.UP, target, false)));
        helper.assertTrue(bagResult == InteractionResult.PASS,
                "Filled bag must not claim interactions with non-source blocks");
        helper.succeed();
    }

    @GameTest
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

    @GameTest
    public void containerCapacityMatchesMaterial(GameTestHelper helper) {
        assertCapacity(helper, KHBlocks.WOODEN_PLATE, 20);
        assertCapacity(helper, KHBlocks.PORCELAIN_PLATE, 40);
        helper.succeed();
    }

    @GameTest
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
        helper.assertValueEqual((int) contents.nonEmptyItemCopyStream().count(), 2, "lunch box item count");
        helper.succeed();
    }

    @GameTest
    public void leftClickRotatesNextBagIngredient(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        PackingBagService.set(bag,
                PackingBagContents.single(new BaggedIngredient(PackingIngredients.MUTTON.getId())));
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);

        for (int rotation = 1; rotation <= 4; rotation++) {
            InteractionResult result = AttackBlockCallback.EVENT.invoker().interact(
                    player, helper.getLevel(), InteractionHand.MAIN_HAND, target, Direction.UP);
            helper.assertTrue(result == InteractionResult.SUCCESS, "Left click did not replace block breaking");
            helper.assertValueEqual(PackingBagService.get(bag).first().orElseThrow().rotation(), rotation % 4,
                    "bag rotation");
        }

        AttackBlockCallback.EVENT.invoker().interact(
                player, helper.getLevel(), InteractionHand.MAIN_HAND, target, Direction.UP);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target).add(0, 0.5, 0),
                Direction.UP, target, false);
        ((HodgepodgePlateBlock) KHBlocks.PORCELAIN_PLATE).useItemOn(bag,
                helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, hit);
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        PlacedIngredient placed = feast.ingredients().getFirst();
        helper.assertValueEqual(placed.rotation(), 1, "placed rotation");
        helper.assertValueEqual(placed.sizeX(), PackingIngredients.MUTTON.getSize().z(), "rotated size x");
        helper.assertValueEqual(placed.sizeZ(), PackingIngredients.MUTTON.getSize().x(), "rotated size z");
        helper.assertTrue(PackingBagService.get(bag).isEmpty(), "Placed rotated ingredient remained in bag");
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
