package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.IPot;
import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.IStockpot;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor.PotBlockEntityAccessor;
import com.moigferdsrte.kaleidoscopehodgepodge.mixin.accessor.StockpotBlockEntityAccessor;
import net.minecraft.gametest.framework.GameTest;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

import java.util.Collections;

public final class CookwarePackingGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void potPacksFinishedProductWithoutRecipeTypeDependency(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, ModBlocks.POT.defaultBlockState());
        PotBlockEntity pot = (PotBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(pot != null, "Expected pot block entity");
        assert pot != null;
        ItemStack product = blazeLambChopItem(helper);
        ((PotBlockEntityAccessor) pot).kaleidoscopeHodgepodge$setStatus(IPot.FINISHED);
        ((PotBlockEntityAccessor) pot).kaleidoscopeHodgepodge$setResult(product);
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);

        boolean packed = pot.takeOutProduct(helper.getLevel(), player, bag);

        helper.assertTrue(packed, "Finished pot product was not packed");
        helper.assertValueEqual(PackingBagService.get(bag).ingredients().size(), 8,
                "pot packed ingredient count");
        helper.assertValueEqual(pot.getStatus(), IPot.PUT_INGREDIENT, "pot did not reset");
        helper.assertTrue(pot.getResult().isEmpty(), "pot product remained after packing");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void stockpotPacksOneServingAtATime(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, ModBlocks.STOCKPOT.defaultBlockState());
        StockpotBlockEntity stockpot = (StockpotBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(stockpot != null, "Expected stockpot block entity");
        assert stockpot != null;
        StockpotBlockEntityAccessor accessor = (StockpotBlockEntityAccessor) stockpot;
        accessor.kaleidoscopeHodgepodge$setStatus(IStockpot.FINISHED);
        accessor.kaleidoscopeHodgepodge$setResult(blazeLambChopItem(helper));
        accessor.kaleidoscopeHodgepodge$setTakeoutCount(2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack firstBag = KHItems.WRAPPING_BAG.getDefaultInstance();
        helper.assertTrue(stockpot.takeOutProduct(helper.getLevel(), player, firstBag),
                "First stockpot serving was not packed");
        helper.assertValueEqual(stockpot.getTakeoutCount(), 1, "stockpot serving count after first bag");
        helper.assertValueEqual(stockpot.getStatus(), IStockpot.FINISHED,
                "stockpot reset before the final serving");

        ItemStack secondBag = KHItems.WRAPPING_BAG.getDefaultInstance();
        helper.assertTrue(stockpot.takeOutProduct(helper.getLevel(), player, secondBag),
                "Second stockpot serving was not packed");
        helper.assertValueEqual(PackingBagService.get(secondBag).ingredients().size(), 8,
                "stockpot packed ingredient count");
        helper.assertValueEqual(stockpot.getStatus(), IStockpot.PUT_SOUP_BASE,
                "stockpot did not reset after final serving");
        helper.assertTrue(stockpot.getResult().isEmpty(), "stockpot product remained after final serving");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void filledBagDoesNotConsumeFinishedPotProduct(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, ModBlocks.POT.defaultBlockState());
        PotBlockEntity pot = (PotBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(pot != null, "Expected pot block entity");
        assert pot != null;
        ((PotBlockEntityAccessor) pot).kaleidoscopeHodgepodge$setStatus(IPot.FINISHED);
        ((PotBlockEntityAccessor) pot).kaleidoscopeHodgepodge$setResult(blazeLambChopItem(helper));
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        PackingBagService.set(bag, new PackingBagContents(Collections.singletonList(
                new BaggedIngredient(PackingIngredients.RED_BERRY.getId()))));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);

        boolean handled = pot.takeOutProduct(helper.getLevel(), player, bag);

        helper.assertTrue(!handled, "Filled bag intercepted pot takeout");
        helper.assertValueEqual(pot.getStatus(), IPot.FINISHED, "filled bag reset the pot");
        helper.assertTrue(!pot.getResult().isEmpty(), "filled bag consumed the pot product");
        helper.succeed();
    }

    private static ItemStack blazeLambChopItem(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "blaze_lamb_chop");
        Block block = BuiltInRegistries.BLOCK.get(id);
        helper.assertTrue(block instanceof FoodBiteBlock, "Expected blaze lamb chop FoodBiteBlock");
        ItemStack stack = BuiltInRegistries.ITEM.get(id).getDefaultInstance();
        helper.assertTrue(!stack.isEmpty(), "Expected blaze lamb chop block item");
        return stack;
    }
}
