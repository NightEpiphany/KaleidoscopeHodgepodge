package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgePlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgeSoupBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgeDisplayTrayBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientModelService;
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
import com.moigferdsrte.kaleidoscopehodgepodge.interaction.PackingBagRotationHandler;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.StackableFoodBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

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
    public void ingredientCollisionUsesCoarseQuarterHeightMap(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        assert feast != null;

        var state = helper.getLevel().getBlockState(target);
        double emptyOutlineTop = state.getShape(helper.getLevel(), target, CollisionContext.empty()).bounds().maxY;
        feast.setIngredients(List.of(new PlacedIngredient(PackingIngredients.RED_BERRY.getId(),
                4, 12, 4, 2, 2, 2)));

        double ingredientOutlineTop = state.getShape(helper.getLevel(), target, CollisionContext.empty()).bounds().maxY;
        var collision = state.getCollisionShape(helper.getLevel(), target, CollisionContext.empty());
        double collisionTop = collision.bounds().maxY;
        helper.assertTrue(ingredientOutlineTop > emptyOutlineTop,
                "Ingredient change did not invalidate the selectable outline cache");
        helper.assertValueEqual(collisionTop, 14.0D / 16.0D,
                "Ingredient height was not retained in entity collision");
        helper.assertTrue(collision.toAabbs().stream().anyMatch(box -> box.minX <= 0.0D && box.maxX >= 0.5D
                        && box.minZ <= 0.0D && box.maxZ >= 0.5D && box.maxY == 14.0D / 16.0D),
                "Ingredient collision did not expand to its 8px by 8px quarter");
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
    public void topFaceHitStacksAtAnyDistanceAndViewAngle(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        assert feast != null;
        helper.assertTrue(feast.add(PackingIngredients.ICE_CUBE, 8, 8).success(), "Ingredient setup failed");
        PlacedIngredient cube = feast.ingredients().getFirst();
        double top = target.getY() + (cube.y() + cube.sizeY()) / 16.0;

        // 俯角从陡到极浅、视距从贴脸到 20 格以上；瞄点覆盖顶面正中与两个半像素角带。
        Vec3[] eyes = {
                new Vec3(target.getX() + 0.5, top + 1.6, target.getZ() + 0.2),
                new Vec3(target.getX() - 12.0, top + 2.0, target.getZ() + 0.5),
                new Vec3(target.getX() + 22.0, top + 1.9, target.getZ() + 18.0)
        };
        double[][] aims = {
                {cube.xMin() / 32.0, cube.zMin() / 32.0},
                {cube.xMax() / 32.0, cube.zMax() / 32.0},
                {0.5, 0.5}
        };
        for (Vec3 eye : eyes) {
            for (double[] aim : aims) {
                BlockHitResult topHit = new BlockHitResult(
                        new Vec3(target.getX() + aim[0], top, target.getZ() + aim[1]),
                        Direction.UP, target, false);
                IngredientPlacementTarget.Pixel pixel = IngredientPlacementTarget.resolve(
                        feast.renderIngredients(), target, eye, topHit,
                        PackingIngredients.RED_BERRY, 0).orElseThrow();
                helper.assertTrue(2 * pixel.x() >= cube.xMin() && 2 * pixel.x() < cube.xMax()
                                && 2 * pixel.z() >= cube.zMin() && 2 * pixel.z() < cube.zMax(),
                        "Top-face hit left the stack column: " + pixel.x() + "," + pixel.z());
                PlacementSpace.Result result = PlacementSpace.place(feast.renderIngredients(),
                        PackingIngredients.RED_BERRY, pixel.x(), pixel.z(), 40, 2,
                        feast.placementBounds(), 0);
                helper.assertTrue(result.success(), "Top-face placement was rejected");
                helper.assertValueEqual(result.placement().orElseThrow().y(), cube.y() + cube.sizeY(),
                        "Top-face placement did not stack on the ingredient");
            }
        }
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
        assertTrayLimits(helper);
        assertCapacity(helper, KHBlocks.PORCELAIN_PLATE, 40);
        helper.succeed();
    }

    private static void assertTrayLimits(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(new BlockPos(10, 1, 1));
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.BAMBOO_DISPLAY_TRAY.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected bamboo tray block entity");
        assert feast != null;
        helper.assertValueEqual(feast.limits().capacity(), 20, "bamboo tray capacity");
        helper.assertValueEqual(feast.limits().baseHeight(), 6, "bamboo tray base height");
        helper.assertValueEqual(feast.limits().maxHeight(), 24, "bamboo tray max height");
        helper.assertTrue(Math.abs(((HodgepodgeDisplayTrayBlock) KHBlocks.BAMBOO_DISPLAY_TRAY)
                        .makeShape().max(Direction.Axis.Y) - 6.0 / 16.0) < 1.0E-6,
                "bamboo tray selection shape still uses the wooden plate height");
    }

    @GameTest
    public void lunchBoxConvertsAndStacksIngredients(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack lunchBox = KHItems.LUNCH_BOX.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, lunchBox);
        LunchBoxMenu menu = new LunchBoxMenu(1, player.getInventory(), lunchBox, InteractionHand.MAIN_HAND);
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        PackingBagService.set(bag, PackingBagContents.single(
                new BaggedIngredient(PackingIngredients.RED_BERRY.getId())));
        menu.setCarried(bag);
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        helper.assertValueEqual(LunchBoxService.get(lunchBox).slot(0).size(), 1, "converted ingredient count");
        helper.assertTrue(PackingBagService.get(bag).isEmpty(), "bag was not emptied");

        ItemStack display = IngredientModelService.createDisplay(PackingIngredients.RED_BERRY.getId());
        display.setCount(20);
        menu.setCarried(display);
        menu.clicked(0, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        helper.assertValueEqual(LunchBoxService.get(lunchBox).slot(0).size(), 16, "stack limit");
        helper.assertValueEqual(menu.getCarried().getCount(), 5, "display overflow");
        helper.assertTrue(!menu.slots.getFirst().mayPickup(player), "ingredient display can be extracted");
        helper.succeed();
    }

    @GameTest
    public void lunchBoxPlacesAndRetrievesSelectedIngredient(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(target.getX() + 0.5, target.getY() + 2.0, target.getZ() + 0.5);
        BlockHitResult hit = new BlockHitResult(
                new Vec3(target.getX() + 0.5, target.getY() + 2.0 / 16.0, target.getZ() + 0.5),
                Direction.UP, target, false);
        ItemStack lunchBox = KHItems.LUNCH_BOX.getDefaultInstance();
        LunchBoxService.insert(lunchBox, List.of(new BaggedIngredient(PackingIngredients.RED_BERRY.getId())));
        LunchBoxService.select(lunchBox, 0);
        LunchBoxService.setMode(lunchBox, PackingBagMode.PLACEMENT);
        player.setItemInHand(InteractionHand.MAIN_HAND, lunchBox);

        InteractionResult placed = ((HodgepodgePlateBlock) KHBlocks.PORCELAIN_PLATE).useItemOn(
                lunchBox, helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, hit);
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(placed.consumesAction(), "Lunch box placement did not consume the action");
        helper.assertTrue(feast != null && feast.ingredients().size() == 1, "Lunch box did not place ingredient");
        helper.assertTrue(LunchBoxService.get(lunchBox).isEmpty(), "Placed ingredient remained in lunch box");

        LunchBoxService.setMode(lunchBox, PackingBagMode.STORAGE);
        InteractionResult retrieved = ((HodgepodgePlateBlock) KHBlocks.PORCELAIN_PLATE).useItemOn(
                lunchBox, helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(retrieved.consumesAction(), "Lunch box retrieval did not consume the action");
        helper.assertTrue(feast.ingredients().isEmpty(), "Retrieved ingredient remained on plate");
        helper.assertValueEqual(LunchBoxService.get(lunchBox).slot(0).size(), 1,
                "Retrieved ingredient count");
        helper.succeed();
    }

    @GameTest
    public void leftClickRotatesNextBagIngredient(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        PackingBagService.set(bag,
                PackingBagContents.single(new BaggedIngredient(PackingIngredients.MUTTON.getId())));
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);

        for (int rotation = 1; rotation <= 4; rotation++) {
            helper.assertTrue(PackingBagRotationHandler.rotate(player, helper.getLevel(), target),
                    "Server rejected bag rotation");
            helper.assertValueEqual(PackingBagService.get(bag).first().orElseThrow().rotation(), rotation % 4,
                    "bag rotation");
        }

        PackingBagRotationHandler.rotate(player, helper.getLevel(), target);
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
