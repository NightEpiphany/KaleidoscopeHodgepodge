package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.moigferdsrte.kaleidoscopehodgepodge.block.LargePorcelainPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.MedianPorcelainPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientPlacementTarget;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;

public final class MultiBlockPlateGameTests {
    private static final BlockPos TARGET = new BlockPos(3, 1, 3);

    @GameTest
    public void largePlateRequiresClearAreaAndProvidesNineContainers(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(TARGET);
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE;
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPlaceContext context = placementContext(helper, player, center, KHItems.LARGE_PORCELAIN_PLATE);

        helper.getLevel().setBlockAndUpdate(center.east(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(block.getStateForPlacement(context) == null,
                "Large plate ignored an obstructing block");
        helper.getLevel().setBlockAndUpdate(center.east(), Blocks.AIR.defaultBlockState());

        BlockState centerState = block.getStateForPlacement(context);
        helper.assertTrue(centerState != null, "Large plate rejected a clear 3x3 area");
        assert centerState != null;
        helper.getLevel().setBlockAndUpdate(center, centerState);
        block.setPlacedBy(helper.getLevel(), center, centerState, player,
                KHItems.LARGE_PORCELAIN_PLATE.getDefaultInstance());

        int totalCapacity = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos partPos = center.offset(x, 0, z);
                helper.assertTrue(helper.getLevel().getBlockState(partPos).is(block),
                        "Large plate part is missing at " + x + "," + z);
                HodgepodgeFeastBlockEntity feast = feast(helper, partPos);
                totalCapacity += feast.limits().capacity();
                fill(feast, 40, helper);
            }
        }
        helper.assertValueEqual(totalCapacity, 360, "large plate capacity");
        helper.succeed();
    }

    @GameTest
    public void largePlateMatchesFortyPixelModelAndFortyTwoPixelPlacementArea(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(TARGET);
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE;
        BlockState centerState = block.defaultBlockState().setValue(LargePorcelainPlateBlock.PART,
                LargePorcelainPlateBlock.Part.CENTER);
        helper.getLevel().setBlockAndUpdate(center, centerState);
        block.setPlacedBy(helper.getLevel(), center, centerState, null,
                KHItems.LARGE_PORCELAIN_PLATE.getDefaultInstance());

        for (LargePorcelainPlateBlock.Part part : LargePorcelainPlateBlock.Part.values()) {
            BlockPos partPos = center.offset(part.x(), 0, part.z());
            BlockState state = helper.getLevel().getBlockState(partPos);
            AABB shape = block.getShape(state, helper.getLevel(), partPos, CollisionContext.empty()).bounds();
            double expectedMinX = part.x() < 0 ? 0.25 : 0.0;
            double expectedMaxX = part.x() > 0 ? 0.75 : 1.0;
            double expectedMinZ = part.z() < 0 ? 0.25 : 0.0;
            double expectedMaxZ = part.z() > 0 ? 0.75 : 1.0;
            helper.assertTrue(shape.minX == expectedMinX && shape.maxX == expectedMaxX
                            && shape.minZ == expectedMinZ && shape.maxZ == expectedMaxZ,
                    "Large plate voxel shape does not match part " + part.getSerializedName());
        }

        HodgepodgeFeastBlockEntity northWest = feast(helper, center.offset(-1, 0, -1));
        PlacementSpace.Bounds bounds = northWest.placementBounds();
        helper.assertTrue(bounds.minX() == 3 && bounds.maxX() == 45
                        && bounds.minZ() == 3 && bounds.maxZ() == 45,
                "North-west placement area does not span the complete 42px plate");
        helper.assertTrue(!PlacementSpace.place(List.of(), PackingIngredients.RED_BERRY,
                        3, 4, 40, 2, bounds, 0).success(),
                "Ingredient crossed more than one pixel beyond the plate rim");
        helper.assertTrue(PlacementSpace.place(List.of(), PackingIngredients.RED_BERRY,
                        4, 4, 40, 2, bounds, 0).success(),
                "Ingredient could not use the one-pixel area beyond the plate rim");
        helper.succeed();
    }

    @GameTest
    public void largePlateBuildsCollisionHeightMapsForEachPart(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(TARGET);
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE;
        BlockState centerState = block.defaultBlockState().setValue(LargePorcelainPlateBlock.PART,
                LargePorcelainPlateBlock.Part.CENTER);
        helper.getLevel().setBlockAndUpdate(center, centerState);
        block.setPlacedBy(helper.getLevel(), center, centerState, null,
                KHItems.LARGE_PORCELAIN_PLATE.getDefaultInstance());

        BlockPos west = center.west();
        HodgepodgeFeastBlockEntity westFeast = feast(helper, west);
        westFeast.setIngredients(List.of(new PlacedIngredient(PackingIngredients.RED_BERRY.getId(),
                15, 12, 8, 4, 2, 2)));
        double adjacentTop = helper.getLevel().getBlockState(center)
                .getCollisionShape(helper.getLevel(), center, CollisionContext.empty()).bounds().maxY;
        helper.assertValueEqual(adjacentTop, 14.0D / 16.0D,
                "A seam-crossing ingredient did not contribute to the adjacent part height map");
        helper.succeed();
    }

    @GameTest
    public void largePlateAllowsIngredientsToCrossPartSeams(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(TARGET);
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE;
        BlockState centerState = block.defaultBlockState().setValue(LargePorcelainPlateBlock.PART,
                LargePorcelainPlateBlock.Part.CENTER);
        helper.getLevel().setBlockAndUpdate(center, centerState);
        block.setPlacedBy(helper.getLevel(), center, centerState, null,
                KHItems.LARGE_PORCELAIN_PLATE.getDefaultInstance());

        BlockPos northWestPos = center.offset(-1, 0, -1);
        BlockPos northPos = center.north();
        HodgepodgeFeastBlockEntity northWest = feast(helper, northWestPos);
        HodgepodgeFeastBlockEntity north = feast(helper, northPos);
        var surface = block.placementIngredients(helper.getLevel(), northWestPos,
                helper.getLevel().getBlockState(northWestPos));
        PlacementSpace.Result placed = northWest.addAgainst(surface,
                PackingIngredients.FONDANT_SPIDER_EYE, 15, 10, 0,
                com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodData.EMPTY);
        helper.assertTrue(placed.success(), "A seam-crossing ingredient was rejected");

        var fromAdjacentPart = block.placementIngredients(helper.getLevel(), northPos,
                helper.getLevel().getBlockState(northPos));
        helper.assertValueEqual(fromAdjacentPart.size(), 1,
                "Adjacent part could not see the seam-crossing ingredient");
        PlacementSpace.Result overlap = north.addAgainst(fromAdjacentPart,
                PackingIngredients.FONDANT_SPIDER_EYE, 8, 10, 0,
                com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodData.EMPTY);
        helper.assertTrue(!overlap.success() && overlap.failure() == PlacementSpace.Failure.OVERLAP,
                "Adjacent part allowed an overlapping seam-crossing ingredient");
        helper.succeed();
    }

    @GameTest
    public void largePlatePreservesWaterloggedPartsAndCombinedDrop(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(TARGET);
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.getLevel().setBlockAndUpdate(center.offset(x, 0, z), Blocks.WATER.defaultBlockState());
            }
        }

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPlaceContext context = placementContext(helper, player, center, KHItems.LARGE_PORCELAIN_PLATE);
        BlockState centerState = block.getStateForPlacement(context);
        helper.assertTrue(centerState != null, "Large plate rejected a water-filled 3x3 area");
        assert centerState != null;
        helper.getLevel().setBlockAndUpdate(center, centerState);
        block.setPlacedBy(helper.getLevel(), center, centerState, player,
                KHItems.LARGE_PORCELAIN_PLATE.getDefaultInstance());

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos partPos = center.offset(x, 0, z);
                helper.assertTrue(helper.getLevel().getBlockState(partPos)
                                .getValue(BlockStateProperties.WATERLOGGED),
                        "Large plate part lost its waterlogged state at " + x + "," + z);
                helper.assertTrue(feast(helper, partPos).add(PackingIngredients.RED_BERRY, 8, 8).success(),
                        "Could not add an ingredient to a waterlogged part");
            }
        }

        BlockPos northWest = center.offset(-1, 0, -1);
        block.playerWillDestroy(helper.getLevel(), northWest,
                helper.getLevel().getBlockState(northWest), player);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos partPos = center.offset(x, 0, z);
                if (!partPos.equals(northWest)) {
                    helper.assertTrue(helper.getLevel().getBlockState(partPos).is(Blocks.WATER),
                            "Destroyed plate part did not restore water at " + x + "," + z);
                }
            }
        }
        ItemStack drop = helper.getLevel().getEntities(EntityType.ITEM,
                        new AABB(center).inflate(3.0), Entity::isAlive).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(KHItems.LARGE_PORCELAIN_PLATE))
                .findFirst().orElseThrow();
        CustomFeastData data = drop.get(KHDataComponents.CUSTOM_FEAST);
        helper.assertTrue(data != null && data.ingredients().size() == 9,
                "Waterlogged large plate did not preserve all part contents");
        helper.succeed();
    }

    @GameTest
    public void medianPlateStoresEightyAndPreservesBothPartsInDrop(GameTestHelper helper) {
        BlockPos leftPos = helper.absolutePos(TARGET);
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE;
        BlockState leftState = block.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                        Direction.NORTH)
                .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
        helper.getLevel().setBlockAndUpdate(leftPos, leftState);
        block.setPlacedBy(helper.getLevel(), leftPos, leftState, null,
                KHItems.MEDIAN_PORCELAIN_PLATE.getDefaultInstance());
        BlockPos rightPos = leftPos.east();

        HodgepodgeFeastBlockEntity left = feast(helper, leftPos);
        HodgepodgeFeastBlockEntity right = feast(helper, rightPos);
        fill(left, 40, helper);
        fill(right, 40, helper);
        helper.assertValueEqual(left.ingredients().size() + right.ingredients().size(), 80,
                "median plate capacity");

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        block.playerWillDestroy(helper.getLevel(), rightPos, helper.getLevel().getBlockState(rightPos), player);
        List<ItemEntity> drops = helper.getLevel().getEntities(EntityType.ITEM,
                new AABB(leftPos).inflate(3.0), Entity::isAlive);
        ItemStack drop = drops.stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(KHItems.MEDIAN_PORCELAIN_PLATE))
                .findFirst().orElseThrow();
        CustomFeastData data = drop.get(KHDataComponents.CUSTOM_FEAST);
        helper.assertTrue(data != null, "Median plate drop lost its feast snapshot");
        assert data != null;
        helper.assertValueEqual(data.ingredients().size(), 80, "median plate snapshot size");
        helper.assertTrue(helper.getLevel().getBlockState(leftPos).isAir(),
                "Breaking the right part left the left part behind");

        helper.getLevel().removeBlock(rightPos, false);
        helper.getLevel().setBlockAndUpdate(leftPos, leftState);
        block.setPlacedBy(helper.getLevel(), leftPos, leftState, null, drop);
        helper.assertValueEqual(feast(helper, leftPos).ingredients().size(), 40,
                "left snapshot partition");
        helper.assertValueEqual(feast(helper, rightPos).ingredients().size(), 40,
                "right snapshot partition");
        helper.succeed();
    }

    @GameTest
    public void medianPlateVoxelShapesMatchBothModelHalves(GameTestHelper helper) {
        BlockPos leftPos = helper.absolutePos(TARGET);
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE;
        BlockState leftState = block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
        helper.getLevel().setBlockAndUpdate(leftPos, leftState);
        block.setPlacedBy(helper.getLevel(), leftPos, leftState, null,
                KHItems.MEDIAN_PORCELAIN_PLATE.getDefaultInstance());

        AABB leftShape = block.getShape(leftState, helper.getLevel(), leftPos, CollisionContext.empty()).bounds();
        BlockPos rightPos = leftPos.east();
        BlockState rightState = helper.getLevel().getBlockState(rightPos);
        AABB rightShape = block.getShape(rightState, helper.getLevel(), rightPos, CollisionContext.empty()).bounds();
        helper.assertTrue(leftShape.maxX >= 1.0 - 1.0E-6 && rightShape.minX <= 1.0E-6,
                "Median plate voxel shapes are missing the part boundary seam");
        helper.assertTrue(leftShape.maxZ > 0.0 && rightShape.maxZ > 0.0,
                "Median plate voxel shapes are empty");
        helper.succeed();
    }

    @GameTest
    public void medianPlateRoundTripIsIndependentOfFacing(GameTestHelper helper) {
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE;
        PackingIngredients.Size size = PackingIngredients.RED_BERRY.getSize();
        List<PlacedIngredient> ingredients = List.of(
                new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 2, 2, 2,
                        size.x(), size.y(), size.z(), 0, IngredientFoodData.EMPTY),
                new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 15, 2, 8,
                        size.x(), size.y(), size.z(), 1, IngredientFoodData.EMPTY),
                new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 17, 2, 8,
                        size.x(), size.y(), size.z(), 2, IngredientFoodData.EMPTY),
                new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 29, 2, 13,
                        size.x(), size.y(), size.z(), 3, IngredientFoodData.EMPTY));
        ItemStack source = KHItems.MEDIAN_PORCELAIN_PLATE.getDefaultInstance();
        source.set(KHDataComponents.CUSTOM_FEAST, new CustomFeastData(
                CustomFeastData.ContainerKind.DISH, Direction.NORTH, ingredients));

        List<ItemStack> roundTrips = new java.util.ArrayList<>();
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int index = 0; index < facings.length; index++) {
            BlockPos leftPos = helper.absolutePos(TARGET.offset(index * 4, 0, 0));
            BlockState leftState = block.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facings[index])
                    .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
            helper.getLevel().setBlockAndUpdate(leftPos, leftState);
            block.setPlacedBy(helper.getLevel(), leftPos, leftState, null, source);

            int placedCount = block.placementIngredients(helper.getLevel(), leftPos, leftState).size();
            helper.assertValueEqual(placedCount, ingredients.size(),
                    facings[index] + " placement ingredient count");

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            block.playerWillDestroy(helper.getLevel(), leftPos, leftState, player);
            ItemStack roundTrip = helper.getLevel().getEntities(EntityType.ITEM,
                            new AABB(leftPos).inflate(2.0), Entity::isAlive).stream()
                    .map(ItemEntity::getItem)
                    .filter(stack -> stack.is(KHItems.MEDIAN_PORCELAIN_PLATE))
                    .findFirst().orElseThrow();
            CustomFeastData restored = roundTrip.get(KHDataComponents.CUSTOM_FEAST);
            helper.assertTrue(restored != null, facings[index] + " drop lost feast data");
            assert restored != null;
            helper.assertValueEqual(restored.ingredients(), ingredients,
                    facings[index] + " round-trip ingredients");
            roundTrips.add(roundTrip.copy());
            helper.getLevel().getEntities(EntityType.ITEM,
                    new AABB(leftPos).inflate(2.0), Entity::isAlive).forEach(Entity::discard);
            helper.getLevel().removeBlock(leftPos, false);
        }

        for (ItemStack roundTrip : roundTrips) {
            helper.assertTrue(ItemStack.isSameItemSameComponents(roundTrips.getFirst(), roundTrip),
                    "Identical median plates from different facings cannot stack");
        }
        helper.succeed();
    }

    @GameTest
    public void medianPlateAllowsPlacementAtPartBoundary(GameTestHelper helper) {
        BlockPos leftPos = helper.absolutePos(TARGET);
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE;
        BlockState leftState = block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
        helper.getLevel().setBlockAndUpdate(leftPos, leftState);
        block.setPlacedBy(helper.getLevel(), leftPos, leftState, null,
                KHItems.MEDIAN_PORCELAIN_PLATE.getDefaultInstance());

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        PackingBagService.set(bag, PackingBagContents.single(
                new BaggedIngredient(PackingIngredients.RED_BERRY.getId())));
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        BlockHitResult seamHit = new BlockHitResult(
                new Vec3(leftPos.getX() + 1.0, leftPos.getY() + 0.125, leftPos.getZ() + 0.5),
                Direction.EAST, leftPos, false);

        InteractionResult result = block.useItemOn(bag, leftState, helper.getLevel(), leftPos, player,
                InteractionHand.MAIN_HAND, seamHit);
        helper.assertTrue(result.consumesAction(), "Median plate rejected a seam-boundary placement");
        helper.assertValueEqual(feast(helper, leftPos).ingredients().size(), 1,
                "Median plate did not store a seam-boundary ingredient");
        helper.succeed();
    }

    @GameTest
    public void medianPlateStacksOnIngredientTopAcrossPartSeam(GameTestHelper helper) {
        BlockPos leftPos = helper.absolutePos(TARGET);
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE;
        BlockState leftState = block.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
        helper.getLevel().setBlockAndUpdate(leftPos, leftState);
        block.setPlacedBy(helper.getLevel(), leftPos, leftState, null,
                KHItems.MEDIAN_PORCELAIN_PLATE.getDefaultInstance());
        BlockPos rightPos = leftPos.east();
        HodgepodgeFeastBlockEntity left = feast(helper, leftPos);
        helper.assertTrue(feast(helper, rightPos).add(PackingIngredients.ICE_CUBE, 8, 8).success(),
                "Could not seed the neighbour part with an ice cube");

        List<PlacedIngredient> surface = block.placementIngredients(helper.getLevel(), leftPos, leftState);
        helper.assertValueEqual(surface.size(), 1, "Left part cannot see the neighbour ingredient");
        PlacedIngredient cube = surface.getFirst();
        helper.assertValueEqual(cube.x(), 24, "Neighbour ingredient x in left part coordinates");

        // 远距离、浅俯角时香草射线会先进入左半格的体素，命中方块被判定为左半格，
        // 而命中点落在右半格内（局部 x = 1.5）。此时仍必须叠放在冰块顶面。
        double top = leftPos.getY() + (cube.y() + cube.sizeY()) / 16.0;
        Vec3 eye = new Vec3(leftPos.getX() - 9.0, top + 1.6875, leftPos.getZ() + 0.5);
        BlockHitResult ghostHit = new BlockHitResult(
                new Vec3(leftPos.getX() + cube.x() / 16.0, top, leftPos.getZ() + cube.z() / 16.0),
                Direction.UP, leftPos, false);

        IngredientPlacementTarget.Pixel target = IngredientPlacementTarget.resolve(surface, leftPos, eye,
                ghostHit, PackingIngredients.RED_BERRY, 0, left.placementBounds(),
                block.allowsBoundaryPlacementProjection()).orElseThrow();
        helper.assertValueEqual(target.x(), 24, "Seam top-face placement x");
        helper.assertValueEqual(target.z(), 8, "Seam top-face placement z");

        PlacementSpace.Result result = left.addAgainst(surface, PackingIngredients.RED_BERRY,
                target.x(), target.z(), 0, IngredientFoodData.EMPTY);
        helper.assertTrue(result.success(), "Seam top-face placement was rejected");
        helper.assertValueEqual(result.placement().orElseThrow().y(), cube.y() + cube.sizeY(),
                "Seam top-face placement height");
        helper.succeed();
    }

    @GameTest
    public void nonPlayerDestructionDropsOnceAndCleansRemainingParts(GameTestHelper helper) {
        BlockPos leftPos = helper.absolutePos(TARGET);
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE;
        BlockState leftState = block.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                        Direction.NORTH)
                .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
        helper.getLevel().setBlockAndUpdate(leftPos, leftState);
        block.setPlacedBy(helper.getLevel(), leftPos, leftState, null,
                KHItems.MEDIAN_PORCELAIN_PLATE.getDefaultInstance());
        BlockPos rightPos = leftPos.east();
        helper.assertTrue(feast(helper, leftPos).add(PackingIngredients.RED_BERRY, 8, 8).success(),
                "Could not fill the left plate part");
        helper.assertTrue(feast(helper, rightPos).add(PackingIngredients.RED_BERRY, 8, 8).success(),
                "Could not fill the right plate part");

        helper.getLevel().destroyBlock(rightPos, true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(leftPos).isAir(),
                    "External destruction left an orphaned plate part");
            List<ItemStack> drops = helper.getLevel().getEntities(EntityType.ITEM,
                            new AABB(leftPos).inflate(3.0), Entity::isAlive).stream()
                    .map(ItemEntity::getItem)
                    .filter(stack -> stack.is(KHItems.MEDIAN_PORCELAIN_PLATE))
                    .toList();
            helper.assertValueEqual(drops.size(), 1, "external destruction plate drop count");
            CustomFeastData data = drops.getFirst().get(KHDataComponents.CUSTOM_FEAST);
            helper.assertTrue(data != null && data.ingredients().size() == 2,
                    "External destruction lost part contents");
            helper.succeed();
        });
    }

    private static BlockPlaceContext placementContext(GameTestHelper helper,
                                                       net.minecraft.world.entity.player.Player player,
                                                       BlockPos pos, net.minecraft.world.item.Item item) {
        ItemStack stack = item.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static HodgepodgeFeastBlockEntity feast(GameTestHelper helper, BlockPos pos) {
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(feast != null, "Expected feast block entity at " + pos);
        return feast;
    }

    private static void fill(HodgepodgeFeastBlockEntity feast, int count, GameTestHelper helper) {
        PlacementSpace.Bounds bounds = feast.placementBounds();
        int minX = Math.max(0, bounds.minX());
        int maxX = Math.min(16, bounds.maxX());
        int minZ = Math.max(0, bounds.minZ());
        int maxZ = Math.min(16, bounds.maxZ());
        int columns = Math.max(1, (maxX - minX) / 2);
        int rows = Math.max(1, (maxZ - minZ) / 2);
        for (int index = 0; index < count; index++) {
            int cell = index % (columns * rows);
            int x = minX + 1 + cell % columns * 2;
            int z = minZ + 1 + cell / columns * 2;
            helper.assertTrue(feast.add(PackingIngredients.RED_BERRY, x, z).success(),
                    "Ingredient " + index + " did not fit");
        }
        helper.assertTrue(!feast.add(PackingIngredients.RED_BERRY,
                        minX + 1, minZ + 1).success(),
                "Part exceeded porcelain capacity");
    }
}
