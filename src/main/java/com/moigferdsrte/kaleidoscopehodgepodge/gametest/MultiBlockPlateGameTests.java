package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.block.LargePorcelainPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.MedianPorcelainPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
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

@GameTestHolder(KaleidoscopeHodgepodge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultiBlockPlateGameTests {
    private static final BlockPos TARGET = new BlockPos(3, 1, 3);

    @GameTest(template = "empty")
    public void largePlateRequiresClearAreaAndProvidesNineContainers(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(TARGET);
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE.get();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPlaceContext context = placementContext(helper, player, center, KHItems.LARGE_PORCELAIN_PLATE.get());

        helper.getLevel().setBlockAndUpdate(center.east(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(block.getStateForPlacement(context) == null,
                "Large plate ignored an obstructing block");
        helper.getLevel().setBlockAndUpdate(center.east(), Blocks.AIR.defaultBlockState());

        BlockState centerState = block.getStateForPlacement(context);
        helper.assertTrue(centerState != null, "Large plate rejected a clear 3x3 area");
        assert centerState != null;
        helper.getLevel().setBlockAndUpdate(center, centerState);
        block.setPlacedBy(helper.getLevel(), center, centerState, player,
                KHItems.LARGE_PORCELAIN_PLATE.get().getDefaultInstance());

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

    @GameTest(template = "empty")
    public void largePlateMatchesFortyPixelModelAndFortyTwoPixelPlacementArea(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(TARGET);
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE.get();
        BlockState centerState = block.defaultBlockState().setValue(LargePorcelainPlateBlock.PART,
                LargePorcelainPlateBlock.Part.CENTER);
        helper.getLevel().setBlockAndUpdate(center, centerState);
        block.setPlacedBy(helper.getLevel(), center, centerState, null,
                KHItems.LARGE_PORCELAIN_PLATE.get().getDefaultInstance());

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

    @GameTest(template = "empty")
    public void largePlateAllowsIngredientsToCrossPartSeams(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(TARGET);
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE.get();
        BlockState centerState = block.defaultBlockState().setValue(LargePorcelainPlateBlock.PART,
                LargePorcelainPlateBlock.Part.CENTER);
        helper.getLevel().setBlockAndUpdate(center, centerState);
        block.setPlacedBy(helper.getLevel(), center, centerState, null,
                KHItems.LARGE_PORCELAIN_PLATE.get().getDefaultInstance());

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

    @GameTest(template = "empty")
    public void largePlatePreservesWaterloggedPartsAndCombinedDrop(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(TARGET);
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE.get();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.getLevel().setBlockAndUpdate(center.offset(x, 0, z), Blocks.WATER.defaultBlockState());
            }
        }

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPlaceContext context = placementContext(helper, player, center, KHItems.LARGE_PORCELAIN_PLATE.get());
        BlockState centerState = block.getStateForPlacement(context);
        helper.assertTrue(centerState != null, "Large plate rejected a water-filled 3x3 area");
        assert centerState != null;
        helper.getLevel().setBlockAndUpdate(center, centerState);
        block.setPlacedBy(helper.getLevel(), center, centerState, player,
                KHItems.LARGE_PORCELAIN_PLATE.get().getDefaultInstance());

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
                .filter(stack -> stack.is(KHItems.LARGE_PORCELAIN_PLATE.get()))
                .findFirst().orElseThrow();
        CustomFeastData data = drop.get(KHDataComponents.CUSTOM_FEAST.get());
        helper.assertTrue(data != null && data.ingredients().size() == 9,
                "Waterlogged large plate did not preserve all part contents");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void medianPlateStoresEightyAndPreservesBothPartsInDrop(GameTestHelper helper) {
        BlockPos leftPos = helper.absolutePos(TARGET);
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE.get();
        BlockState leftState = block.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                        Direction.NORTH)
                .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
        helper.getLevel().setBlockAndUpdate(leftPos, leftState);
        block.setPlacedBy(helper.getLevel(), leftPos, leftState, null,
                KHItems.MEDIAN_PORCELAIN_PLATE.get().getDefaultInstance());
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
                .filter(stack -> stack.is(KHItems.MEDIAN_PORCELAIN_PLATE.get()))
                .findFirst().orElseThrow();
        CustomFeastData data = drop.get(KHDataComponents.CUSTOM_FEAST.get());
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

    @GameTest(template = "empty")
    public void medianPlateRoundTripIsIndependentOfFacing(GameTestHelper helper) {
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE.get();
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
        ItemStack source = KHItems.MEDIAN_PORCELAIN_PLATE.get().getDefaultInstance();
        source.set(KHDataComponents.CUSTOM_FEAST.get(), new CustomFeastData(
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
                    .filter(stack -> stack.is(KHItems.MEDIAN_PORCELAIN_PLATE.get()))
                    .findFirst().orElseThrow();
            CustomFeastData restored = roundTrip.get(KHDataComponents.CUSTOM_FEAST.get());
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

    @GameTest(template = "empty")
    public void medianPlateDirectPlacementDropIsIndependentOfFacing(GameTestHelper helper) {
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE.get();
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        List<PlacedIngredient> droppedIngredients = new java.util.ArrayList<>();

        for (int index = 0; index < facings.length; index++) {
            BlockPos leftPos = helper.absolutePos(TARGET.offset(index * 4, 0, 0));
            BlockState leftState = block.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facings[index])
                    .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
            helper.getLevel().setBlockAndUpdate(leftPos, leftState);
            block.setPlacedBy(helper.getLevel(), leftPos, leftState, null,
                    KHItems.MEDIAN_PORCELAIN_PLATE.get().getDefaultInstance());
            helper.assertTrue(feast(helper, leftPos).add(PackingIngredients.RED_BERRY, 8, 8).success(),
                    facings[index] + " direct ingredient placement failed");

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            block.playerWillDestroy(helper.getLevel(), leftPos, leftState, player);
            ItemStack drop = helper.getLevel().getEntities(EntityType.ITEM,
                            new AABB(leftPos).inflate(2.0), Entity::isAlive).stream()
                    .map(ItemEntity::getItem)
                    .filter(stack -> stack.is(KHItems.MEDIAN_PORCELAIN_PLATE.get()))
                    .findFirst().orElseThrow();
            CustomFeastData data = drop.get(KHDataComponents.CUSTOM_FEAST.get());
            helper.assertTrue(data != null && data.ingredients().size() == 1,
                    facings[index] + " direct placement drop lost its ingredient");
            assert data != null;
            droppedIngredients.add(data.ingredients().getFirst());

            helper.getLevel().getEntities(EntityType.ITEM,
                    new AABB(leftPos).inflate(2.0), Entity::isAlive).forEach(Entity::discard);
            helper.getLevel().removeBlock(leftPos, false);
        }

        PlacedIngredient expected = droppedIngredients.getFirst();
        for (PlacedIngredient ingredient : droppedIngredients) {
            helper.assertValueEqual(ingredient.x(), expected.x(),
                    "Directly filled median plate changed item X with facing");
            helper.assertValueEqual(ingredient.y(), expected.y(),
                    "Directly filled median plate changed item Y with facing");
            helper.assertValueEqual(ingredient.z(), expected.z(),
                    "Directly filled median plate changed item Z with facing");
            helper.assertValueEqual(ingredient.sizeX(), expected.sizeX(),
                    "Directly filled median plate changed item width with facing");
            helper.assertValueEqual(ingredient.sizeZ(), expected.sizeZ(),
                    "Directly filled median plate changed item depth with facing");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void nonPlayerDestructionDropsOnceAndCleansRemainingParts(GameTestHelper helper) {
        BlockPos leftPos = helper.absolutePos(TARGET);
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE.get();
        BlockState leftState = block.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                        Direction.NORTH)
                .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
        helper.getLevel().setBlockAndUpdate(leftPos, leftState);
        block.setPlacedBy(helper.getLevel(), leftPos, leftState, null,
                KHItems.MEDIAN_PORCELAIN_PLATE.get().getDefaultInstance());
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
                    .filter(stack -> stack.is(KHItems.MEDIAN_PORCELAIN_PLATE.get()))
                    .toList();
            helper.assertValueEqual(drops.size(), 1, "external destruction plate drop count");
            CustomFeastData data = drops.getFirst().get(KHDataComponents.CUSTOM_FEAST.get());
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
