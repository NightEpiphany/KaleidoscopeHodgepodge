package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.github.ysbbbbbb.kaleidoscopecookery.block.decoration.PlateBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteOneByTwoBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor.PlateBlockAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class FoodStructurePackingGameTests {
    private static final Identifier SOURCE = Identifier.fromNamespaceAndPath(
            KaleidoscopeCookery.MOD_ID, "braised_pork_ribs");
    private static final Identifier PLATE_SOURCE = Identifier.fromNamespaceAndPath(
            KaleidoscopeCookery.MOD_ID, "shengjian_mantou_plate");

    @GameTest
    public void eitherOneByTwoPartPacksWithoutDuplicatingDish(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.getValue(SOURCE);
        helper.assertTrue(block instanceof FoodBiteOneByTwoBlock,
                "Expected braised pork ribs to use the 1x2 food block");
        FoodBiteOneByTwoBlock food = (FoodBiteOneByTwoBlock) block;
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        for (int iteration = 0; iteration < 2; iteration++) {
            BlockPos rightPos = helper.absolutePos(new BlockPos(3 + iteration * 4, 1, 3));
            BlockState rightState = food.defaultBlockState()
                    .setValue(FoodBiteOneByTwoBlock.POSITION, FoodBiteOneByTwoBlock.RIGHT)
                    .setValue(FoodBiteOneByTwoBlock.FACING, Direction.SOUTH)
                    .setValue(BlockStateProperties.WATERLOGGED, false);
            helper.getLevel().setBlockAndUpdate(rightPos, rightState);
            food.setPlacedBy(helper.getLevel(), rightPos, rightState, player, food.asItem().getDefaultInstance());
            BlockPos leftPos = rightPos.relative(Direction.SOUTH.getClockWise());
            BlockPos clickedPos = iteration == 0 ? rightPos : leftPos;

            ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
            player.setItemInHand(InteractionHand.MAIN_HAND, bag);
            int bowlsBefore = player.getInventory().countItem(Items.BOWL);
            var result = ((WrappingBagItem) KHItems.WRAPPING_BAG).useOn(new UseOnContext(
                    helper.getLevel(), player, InteractionHand.MAIN_HAND, bag,
                    new BlockHitResult(Vec3.atCenterOf(clickedPos), Direction.UP, clickedPos, false)));

            helper.assertTrue(result.consumesAction(), "Packing did not consume the interaction");
            helper.assertTrue(!PackingBagService.get(player.getMainHandItem()).isEmpty(),
                    "Packing left the bag empty");
            helper.assertTrue(!helper.getLevel().getBlockState(leftPos).is(food)
                            && !helper.getLevel().getBlockState(rightPos).is(food),
                    "Packing left part of the dish structure behind");
            helper.assertValueEqual(player.getInventory().countItem(Items.BOWL), bowlsBefore + 1,
                    "Packing did not return exactly one bowl");
            long duplicateDrops = helper.getLevel().getEntities(EntityType.ITEM,
                            new AABB(rightPos).inflate(2.0), Entity::isAlive).stream()
                    .filter(entity -> entity.getItem().is(food.asItem()))
                    .count();
            helper.assertValueEqual(duplicateDrops, 0L,
                    "Packing the secondary part duplicated the source dish");
        }
        helper.succeed();
    }

    @GameTest
    public void plateBlockPacksOnlyWhenWhole(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.getValue(PLATE_SOURCE);
        helper.assertTrue(block instanceof PlateBlock,
                "Expected shengjian mantou plate to use PlateBlock");
        PlateBlock plate = (PlateBlock) block;
        var servings = ((PlateBlockAccessor) plate).kaleidoscopeHodgepodge$getServings();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult fullHit = hit(helper, new BlockPos(3, 1, 3));
        BlockPos fullPos = fullHit.getBlockPos();

        helper.getLevel().setBlockAndUpdate(fullPos, plate.defaultBlockState());
        ItemStack fullBag = KHItems.WRAPPING_BAG.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, fullBag);
        int bowlsBefore = player.getInventory().countItem(Items.BOWL);
        InteractionResult packed = plate.useItemOn(fullBag, helper.getLevel().getBlockState(fullPos),
                helper.getLevel(), fullPos, player, InteractionHand.MAIN_HAND, fullHit);

        var contents = PackingBagService.get(player.getMainHandItem());
        helper.assertTrue(packed.consumesAction(), "PlateBlock packing did not consume the interaction");
        helper.assertValueEqual(contents.ingredients().size(), 4, "packed plate ingredient count");
        helper.assertTrue(contents.ingredients().stream()
                        .allMatch(ingredient -> ingredient.id().equals(PackingIngredients.SHENGJIAN_MANTOU.getId())),
                "PlateBlock packed an unexpected ingredient");
        helper.assertTrue(!helper.getLevel().getBlockState(fullPos).is(plate),
                "Packed PlateBlock remained in the world");
        helper.assertValueEqual(player.getInventory().countItem(Items.BOWL), bowlsBefore + 1,
                "Packing PlateBlock did not return exactly one bowl");
        helper.assertTrue(helper.getLevel().getEntities(EntityType.ITEM,
                        new AABB(fullPos).inflate(2.0), Entity::isAlive).isEmpty(),
                "Packing PlateBlock produced an extra serving drop");

        BlockHitResult partialHit = hit(helper, new BlockPos(7, 1, 3));
        BlockPos partialPos = partialHit.getBlockPos();
        int partialCount = plate.getMaxCount() - 1;
        helper.getLevel().setBlockAndUpdate(partialPos,
                plate.defaultBlockState().setValue(servings, partialCount));
        ItemStack emptyBag = KHItems.WRAPPING_BAG.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, emptyBag);
        InteractionResult rejected = plate.useItemOn(emptyBag, helper.getLevel().getBlockState(partialPos),
                helper.getLevel(), partialPos, player, InteractionHand.MAIN_HAND, partialHit);

        helper.assertTrue(rejected == InteractionResult.FAIL, "Partial PlateBlock was packed");
        helper.assertTrue(PackingBagService.get(player.getMainHandItem()).isEmpty(),
                "Rejected PlateBlock packing filled the bag");
        helper.assertValueEqual(helper.getLevel().getBlockState(partialPos).getValue(servings), partialCount,
                "Rejected PlateBlock packing removed a serving");
        helper.succeed();
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        return new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
    }
}
