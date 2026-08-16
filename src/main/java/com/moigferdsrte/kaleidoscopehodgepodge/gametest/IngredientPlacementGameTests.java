package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgePlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgeSoupBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientPlacementTarget;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class IngredientPlacementGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
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
                InteractionHand.MAIN_HAND, hit).result();
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(result.consumesAction(), "Placement did not consume the action");
        helper.assertTrue(PackingBagService.get(bag).isEmpty(), "Bag contents were not cleared");
        assert feast != null;
        helper.assertValueEqual(feast.ingredients().size(), 1, "ingredient count");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
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
                InteractionHand.MAIN_HAND, hit).result();

        helper.assertTrue(result.consumesAction(), "Retrieval did not consume the action");
        helper.assertValueEqual(PackingBagService.get(bag).first().orElseThrow().id(),
                PackingIngredients.RED_BERRY.getId(), "retrieved ingredient");
        helper.assertTrue(feast.ingredients().isEmpty(), "Retrieved ingredient remained in the feast");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
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

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void soupRejectsDishOnlyIngredient(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_SOUP_BOWL.defaultBlockState());
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();
        bag.set(KHDataComponents.PACKING_BAG_INGREDIENT, PackingIngredients.MUTTON.getId().toString());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InteractionResult result = ((HodgepodgeSoupBlock) KHBlocks.PORCELAIN_SOUP_BOWL).useItemOn(bag,
                helper.getLevel().getBlockState(target), helper.getLevel(), target, player,
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(target).add(0, 0.5, 0),
                        Direction.UP, target, false)).result();
        helper.assertTrue(result == InteractionResult.FAIL, "Soup should reject dish-only ingredient");
        helper.assertTrue(!PackingBagService.get(bag).isEmpty(), "Rejected placement consumed the bag");
        InteractionResult bagResult = ((WrappingBagItem) KHItems.WRAPPING_BAG).useOn(new UseOnContext(
                helper.getLevel(), player, InteractionHand.MAIN_HAND, bag,
                new BlockHitResult(Vec3.atCenterOf(target).add(0, 0.5, 0), Direction.UP, target, false)));
        helper.assertTrue(bagResult == InteractionResult.PASS,
                "Filled bag must not claim interactions with non-source blocks");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
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
}
