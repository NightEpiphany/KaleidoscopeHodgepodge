package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.moigferdsrte.kaleidoscopehodgepodge.block.AbstractHodgepodgeFeastBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.AbstractMultiBlockPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.LargePorcelainPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.MedianPorcelainPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.LunchBoxService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class MultiBlockRecipeGameTests {
    private static final BlockPos TARGET = new BlockPos(3, 1, 3);

    @GameTest
    public void mediumRecipeRecordsWholePlateAndCopiesSeamAcrossAllFacings(GameTestHelper helper) {
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE;
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        List<PlacedIngredient> expected = List.of(model(PackingIngredients.MUTTON, 16, 8, 1),
                model(PackingIngredients.RED_BERRY, 4, 4, 0), model(PackingIngredients.RED_BERRY, 28, 12, 0),
                new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 16, 4, 8, 2, 2, 2));
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int index = 0; index < facings.length; index++) {
            BlockPos source = helper.absolutePos(TARGET.offset(index * 8, 0, 0));
            BlockState sourceState = mediumState(block, facings[index]);
            place(helper, block, source, sourceState, expected);
            BlockPos sourceOther = source.relative(facings[index].getClockWise());
            ItemStack recipe = record(helper, player, sourceOther);
            ItemStack otherRecipe = record(helper, player, source);
            helper.assertValueEqual(recipe.get(KHDataComponents.HODGEPODGE_RECIPE),
                    otherRecipe.get(KHDataComponents.HODGEPODGE_RECIPE), "recording from either part");

            BlockPos target = source.south(5);
            Direction targetFacing = facings[(index + 1) % facings.length];
            BlockState targetState = mediumState(block, targetFacing);
            place(helper, block, target, targetState, List.of());
            BlockPos targetOther = target.relative(targetFacing.getClockWise());
            helper.assertTrue(useRecipe(helper, player, targetOther, recipe).consumesAction(), "Recipe did not bind");
            helper.assertTrue(feast(helper, target).nextRecipePlacement().isPresent(), "No recipe preview target");
            assertPreview(helper, block, target, targetOther);
            for (int ingredientIndex = 0; ingredientIndex < expected.size(); ingredientIndex++) {
                PlacedIngredient next = feast(helper, target).nextRecipePlacement().orElseThrow();
                placeNext(helper, block, player, ingredientIndex % 2 == 0 ? target : targetOther,
                        next, ingredientIndex % 2 != 0);
            }
            assertModels(helper, feast(helper, source).recipeSnapshot(), feast(helper, target).recipeSnapshot());
            helper.assertTrue(!feast(helper, target).isRecipeLocked() && !feast(helper, targetOther).isRecipeLocked(),
                    "Completed recipe left a locked part");
        }
        helper.succeed();
    }

    @GameTest
    public void largeRecipeLocksAllNinePartsAndSharesProgressAfterReload(GameTestHelper helper) {
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE;
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos source = helper.absolutePos(TARGET);
        List<PlacedIngredient> expected = List.of(model(PackingIngredients.RED_BERRY, -8, -8, 0),
                model(PackingIngredients.MUTTON, 0, 0, 1), model(PackingIngredients.RED_BERRY, 24, 24, 0));
        place(helper, block, source, block.defaultBlockState(), expected);
        ItemStack recipe = record(helper, player, source.east());
        BlockPos target = source.south(5);
        place(helper, block, target, block.defaultBlockState(), List.of());
        helper.assertTrue(useRecipe(helper, player, target.south().east(), recipe).consumesAction(), "Recipe did not bind");
        for (LargePorcelainPlateBlock.Part part : LargePorcelainPlateBlock.Part.values()) {
            BlockPos partPos = target.offset(part.x(), 0, part.z());
            HodgepodgeFeastBlockEntity entity = feast(helper, partPos);
            helper.assertTrue(entity.isRecipeLocked(), "Unlocked large plate part: " + part);
            helper.assertTrue(!entity.add(PackingIngredients.MUTTON, 8, 8).success(), "Wrong ingredient bypassed lock");
            ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
            PackingBagService.setMode(bag, PackingBagMode.STORAGE);
            helper.assertValueEqual(useContainer(helper, block, player, partPos, bag), InteractionResult.FAIL,
                    "storage lock for " + part);
            ItemStack lunchBox = KHItems.LUNCH_BOX.getDefaultInstance();
            LunchBoxService.setMode(lunchBox, PackingBagMode.STORAGE);
            helper.assertValueEqual(useContainer(helper, block, player, partPos, lunchBox), InteractionResult.FAIL,
                    "lunch box storage lock for " + part);
            helper.assertValueEqual(block.useWithoutItem(helper.getLevel().getBlockState(partPos),
                    helper.getLevel(), partPos, player, hit(partPos)), InteractionResult.FAIL, "eating lock for " + part);
            assertPreview(helper, block, target, partPos);
        }
        placeNext(helper, block, player, target.east(), feast(helper, target).nextRecipePlacement().orElseThrow(), false);
        PlacedIngredient next = feast(helper, target).nextRecipePlacement().orElseThrow();
        for (LargePorcelainPlateBlock.Part part : LargePorcelainPlateBlock.Part.values()) {
            HodgepodgeFeastBlockEntity entity = feast(helper, target.offset(part.x(), 0, part.z()));
            var saved = entity.saveWithoutMetadata(helper.getLevel().registryAccess());
            entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                    helper.getLevel().registryAccess(), saved));
        }
        helper.assertValueEqual(feast(helper, target.south()).nextRecipePlacement().orElseThrow(), next,
                "recipe progress after reload");
        assertPreview(helper, block, target, target.north().west());
        placeNext(helper, block, player, target.north().west(), next, true);
        placeNext(helper, block, player, target.north(), feast(helper, target).nextRecipePlacement().orElseThrow(), false);
        assertModels(helper, feast(helper, source).recipeSnapshot(), feast(helper, target).recipeSnapshot());
        for (LargePorcelainPlateBlock.Part part : LargePorcelainPlateBlock.Part.values()) {
            helper.assertTrue(!feast(helper, target.offset(part.x(), 0, part.z())).isRecipeLocked(), "Stale lock after completion");
        }
        helper.succeed();
    }

    @GameTest
    public void mediumRecipeChecksWholePlateEmptinessAndUnlocksFromEitherPart(GameTestHelper helper) {
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE;
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos source = helper.absolutePos(TARGET);
        place(helper, block, source, mediumState(block, Direction.NORTH),
                List.of(model(PackingIngredients.MUTTON, 16, 8, 0)));
        ItemStack recipe = record(helper, player, source);
        BlockPos target = source.south(4);
        place(helper, block, target, mediumState(block, Direction.NORTH),
                List.of(model(PackingIngredients.RED_BERRY, 24, 8, 0)));
        helper.assertValueEqual(useRecipe(helper, player, target, recipe), InteractionResult.FAIL,
                "binding through an empty part of a nonempty plate");
        feast(helper, target.east()).setIngredients(List.of());
        useRecipe(helper, player, target.east(), recipe);
        helper.assertTrue(feast(helper, target).isRecipeLocked(), "Lock on right did not reach left");
        useRecipe(helper, player, target, recipe);
        helper.assertTrue(!feast(helper, target.east()).isRecipeLocked(), "Unlock on left did not reach right");
        useRecipe(helper, player, target, recipe);
        useRecipe(helper, player, target.east(), recipe);
        helper.assertTrue(!feast(helper, target).isRecipeLocked(), "Unlock on right did not reach left");
        helper.succeed();
    }

    @GameTest
    public void singleBlockPlateAndSoupRecipesStillRecordAndComplete(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        List<AbstractHodgepodgeFeastBlock> blocks = List.of(
                (AbstractHodgepodgeFeastBlock) KHBlocks.WOODEN_PLATE,
                (AbstractHodgepodgeFeastBlock) KHBlocks.PORCELAIN_PLATE,
                (AbstractHodgepodgeFeastBlock) KHBlocks.PORCELAIN_SOUP_BOWL);
        for (int index = 0; index < blocks.size(); index++) {
            AbstractHodgepodgeFeastBlock block = blocks.get(index);
            BlockPos source = helper.absolutePos(TARGET.offset(index * 3, 0, 0));
            BlockPos target = source.south(3);
            helper.getLevel().setBlockAndUpdate(source, block.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(target, block.defaultBlockState());
            helper.assertTrue(feast(helper, source).add(PackingIngredients.RED_BERRY, 8, 8, 2).success(),
                    "Single block source placement failed");
            ItemStack recipe = record(helper, player, source);
            helper.assertTrue(useRecipe(helper, player, target, recipe).consumesAction(), "Single block recipe did not bind");
            placeNext(helper, block, player, target, feast(helper, target).nextRecipePlacement().orElseThrow(), false);
            assertModels(helper, feast(helper, source).recipeSnapshot(), feast(helper, target).recipeSnapshot());
            helper.assertTrue(!feast(helper, target).isRecipeLocked(), "Single block recipe did not complete");
        }
        helper.succeed();
    }

    private static PlacedIngredient model(PackingIngredients ingredient, int pixelX, int pixelZ, int rotation) {
        PackingIngredients.Size size = ingredient.getSize();
        boolean swap = rotation % 2 != 0;
        return new PlacedIngredient(ingredient.getId(), pixelX, 2, pixelZ,
                swap ? size.z() : size.x(), size.y(), swap ? size.x() : size.z(), rotation);
    }

    private static BlockState mediumState(MedianPorcelainPlateBlock block, Direction facing) {
        return block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
    }

    private static void place(GameTestHelper helper, AbstractMultiBlockPlateBlock block, BlockPos pos,
                              BlockState state, List<PlacedIngredient> ingredients) {
        ItemStack stack = new ItemStack(block);
        stack.set(KHDataComponents.CUSTOM_FEAST,
                new CustomFeastData(CustomFeastData.ContainerKind.DISH, Direction.NORTH, ingredients));
        helper.getLevel().setBlockAndUpdate(pos, state);
        block.setPlacedBy(helper.getLevel(), pos, state, null, stack);
    }

    private static ItemStack record(GameTestHelper helper, Player player, BlockPos pos) {
        ItemStack empty = new ItemStack(ModItems.RECIPE_ITEM);
        player.setItemInHand(InteractionHand.MAIN_HAND, empty);
        empty.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
        ItemStack recipe = player.getMainHandItem().copy();
        helper.assertTrue(recipe.is(KHItems.HODGEPODGE_RECIPE), "Empty KC recipe did not record entire plate");
        return recipe;
    }

    private static InteractionResult useRecipe(GameTestHelper helper, Player player, BlockPos pos, ItemStack recipe) {
        player.setItemInHand(InteractionHand.MAIN_HAND, recipe.copy());
        return recipe.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
    }

    private static void placeNext(GameTestHelper helper, AbstractHodgepodgeFeastBlock block, Player player,
                                  BlockPos pos, PlacedIngredient next, boolean useLunchBox) {
        ItemStack stack = new ItemStack(useLunchBox ? KHItems.LUNCH_BOX : KHItems.WRAPPING_BAG);
        BaggedIngredient ingredient = new BaggedIngredient(next.id(), (next.rotation() + 1) % 4, next.food());
        if (useLunchBox) {
            LunchBoxService.insert(stack, List.of(ingredient));
            LunchBoxService.select(stack, 0);
            LunchBoxService.setMode(stack, PackingBagMode.PLACEMENT);
        } else {
            PackingBagService.set(stack, PackingBagContents.single(ingredient));
            PackingBagService.setMode(stack, PackingBagMode.PLACEMENT);
        }
        helper.assertTrue(useContainer(helper, block, player, pos, stack).consumesAction(), "Recipe placement failed");
        helper.assertTrue(useLunchBox ? LunchBoxService.get(stack).isEmpty() : PackingBagService.get(stack).isEmpty(),
                "Recipe placement did not consume ingredient");
    }

    private static InteractionResult useContainer(GameTestHelper helper, AbstractHodgepodgeFeastBlock block,
                                                  Player player, BlockPos pos, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return block.useItemOn(stack, helper.getLevel().getBlockState(pos), helper.getLevel(), pos, player,
                InteractionHand.MAIN_HAND, hit(pos));
    }

    private static void assertPreview(GameTestHelper helper, AbstractMultiBlockPlateBlock block,
                                       BlockPos first, BlockPos second) {
        PlacedIngredient target = feast(helper, first).nextRecipePlacement().orElseThrow();
        helper.assertValueEqual(feast(helper, second).nextRecipePlacement().orElseThrow(), target, "shared recipe target");
        PlacedIngredient firstLocal = block.recipePlacementTarget(first, helper.getLevel().getBlockState(first), target);
        PlacedIngredient secondLocal = block.recipePlacementTarget(second, helper.getLevel().getBlockState(second), target);
        helper.assertValueEqual(firstLocal.translated(first.getX() * 16, first.getZ() * 16),
                secondLocal.translated(second.getX() * 16, second.getZ() * 16), "world-space preview across parts");
    }

    private static void assertModels(GameTestHelper helper, CustomFeastData expected, CustomFeastData actual) {
        helper.assertValueEqual(actual.ingredients().stream().map(value -> value.withFood(IngredientFoodData.EMPTY)).toList(),
                expected.ingredients().stream().map(value -> value.withFood(IngredientFoodData.EMPTY)).toList(), "copied models");
    }

    private static HodgepodgeFeastBlockEntity feast(GameTestHelper helper, BlockPos pos) {
        return (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
