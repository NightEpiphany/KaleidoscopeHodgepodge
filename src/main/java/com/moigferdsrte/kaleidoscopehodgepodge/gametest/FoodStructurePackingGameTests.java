package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteOneByTwoBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
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

    @GameTest
    public void eitherOneByTwoPartPacksWithoutDuplicatingDish(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.getValue(SOURCE);
        helper.assertTrue(block instanceof FoodBiteOneByTwoBlock,
                "Expected braised pork ribs to use the 1x2 food block");
        assert block instanceof FoodBiteOneByTwoBlock;
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
}
