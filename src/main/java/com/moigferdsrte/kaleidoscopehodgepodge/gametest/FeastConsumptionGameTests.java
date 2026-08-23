package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.github.ysbbbbbb.kaleidoscopecookery.KaleidoscopeCookery;
import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import com.moigferdsrte.kaleidoscopehodgepodge.item.WrappingBagItem;
import net.minecraft.gametest.framework.GameTest;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class FeastConsumptionGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void packingCapturesPerBiteFoodData(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        FoodBiteBlock food = blazeLambChop(helper);
        helper.getLevel().setBlockAndUpdate(target, food.defaultBlockState());
        ItemStack bag = KHItems.WRAPPING_BAG.getDefaultInstance();

        ((WrappingBagItem) KHItems.WRAPPING_BAG).useOn(new UseOnContext(helper.getLevel(), null,
                InteractionHand.MAIN_HAND, bag,
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false)));

        PackingBagContents contents = PackingBagService.get(bag);
        helper.assertValueEqual(contents.ingredients().size(), 8, "packed ingredient count");
        IngredientFoodData snapshot = contents.first().orElseThrow().food();
        helper.assertValueEqual(snapshot.nutrition(), 2, "per-bite nutrition");
        helper.assertTrue(Math.abs(snapshot.saturation() - 1.6F) < 0.001F, "per-bite saturation mismatch");
        helper.assertValueEqual(snapshot.effects().size(), 2, "potion effect group count");
        helper.assertValueEqual(snapshot.effects().getFirst().effects().size(), 1, "potion effect count");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void customFeastItemStacksFoodAndReturnsContainer(GameTestHelper helper) {
        GeneralConfig.Snapshot originalConfig = GeneralConfig.snapshot();
        GeneralConfig.replace(originalConfig.withHandheldDishEating(true));
        try {
            verifyHandheldFeastEating(helper);
        } finally {
            GeneralConfig.replace(originalConfig);
        }
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void customFeastItemDoesNotStartEatingByDefault(GameTestHelper helper) {
        ItemStack feastStack = KHItems.WOODEN_PLATE.getDefaultInstance();
        feastStack.set(KHDataComponents.CUSTOM_FEAST, new CustomFeastData(
                CustomFeastData.ContainerKind.DISH, Direction.NORTH,
                List.of(placed(PackingIngredients.RED_BERRY, 8, IngredientFoodData.EMPTY))));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, feastStack);

        ((CustomFeastBlockItem) KHItems.WOODEN_PLATE)
                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(!player.isUsingItem(), "custom feast unexpectedly started handheld eating");
        helper.succeed();
    }

    private static void verifyHandheldFeastEating(GameTestHelper helper) {
        IngredientFoodData food = IngredientFoodService.capture(blazeLambChop(helper));
        PlacedIngredient first = placed(PackingIngredients.RED_BERRY, 5, food);
        PlacedIngredient second = placed(PackingIngredients.ARDENT_CORE, 11, food);
        ItemStack feastStack = KHItems.WOODEN_PLATE.getDefaultInstance();
        feastStack.set(KHDataComponents.CUSTOM_FEAST,
                new CustomFeastData(CustomFeastData.ContainerKind.DISH, Direction.NORTH, List.of(first, second)));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0.0F);
        CustomFeastBlockItem item = (CustomFeastBlockItem) KHItems.WOODEN_PLATE;

        player.setItemInHand(InteractionHand.MAIN_HAND, feastStack);
        InteractionResult useResult = item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult();
        helper.assertTrue(useResult.consumesAction() && player.isUsingItem(), "item did not begin long eating");
        player.stopUsingItem();
        helper.assertValueEqual(item.getUseDuration(feastStack, player), 32, "item eat duration");
        helper.assertValueEqual(item.getUseAnimation(feastStack), UseAnim.EAT, "item eat animation");
        ItemStack remainder = item.finishUsingItem(feastStack, helper.getLevel(), player);

        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 4, "stacked nutrition");
        helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - 3.2F) < 0.001F,
                "stacked saturation mismatch");
        assertEffectDuration(helper, player.getEffect(MobEffects.FIRE_RESISTANCE), 3200, "fire resistance");
        assertEffectDuration(helper, player.getEffect(MobEffects.DAMAGE_RESISTANCE), 4000, "resistance");
        helper.assertTrue(remainder.is(KHItems.WOODEN_PLATE), "eating did not return the wooden plate");
        helper.assertTrue(!remainder.has(KHDataComponents.CUSTOM_FEAST), "returned plate still contains feast data");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void customFeastNamesReflectContentsAndKind(GameTestHelper helper) {
        ItemStack emptyPlate = KHItems.WOODEN_PLATE.getDefaultInstance();
        helper.assertValueEqual(emptyPlate.getItem().getName(emptyPlate),
                Component.translatable("block.kaleidoscope_hodgepodge.wooden_plate"),
                "empty plate name");

        ItemStack dish = KHItems.PORCELAIN_PLATE.getDefaultInstance();
        dish.set(KHDataComponents.CUSTOM_FEAST, new CustomFeastData(
                CustomFeastData.ContainerKind.DISH, Direction.NORTH,
                List.of(placed(PackingIngredients.RED_BERRY, 8, IngredientFoodData.EMPTY))));
        helper.assertValueEqual(dish.getItem().getName(dish),
                Component.translatable("item.kaleidoscope_hodgepodge.custom_dish"),
                "filled plate name");

        ItemStack soup = KHItems.PORCELAIN_SOUP_BOWL.getDefaultInstance();
        soup.set(KHDataComponents.CUSTOM_FEAST, new CustomFeastData(
                CustomFeastData.ContainerKind.SOUP, Direction.NORTH,
                List.of(placed(PackingIngredients.RED_BERRY, 8, IngredientFoodData.EMPTY))));
        helper.assertValueEqual(soup.getItem().getName(soup),
                Component.translatable("item.kaleidoscope_hodgepodge.custom_soup"),
                "filled soup name");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void customFeastItemIgnoresNonNutritionalIngredients(GameTestHelper helper) {
        GeneralConfig.Snapshot originalConfig = GeneralConfig.snapshot();
        GeneralConfig.replace(originalConfig.withHandheldDishEating(true));
        try {
            verifyNonNutritionalItemIngredient(helper);
        } finally {
            GeneralConfig.replace(originalConfig);
        }
    }

    private static void verifyNonNutritionalItemIngredient(GameTestHelper helper) {
        IngredientFoodData food = IngredientFoodService.capture(blazeLambChop(helper));
        ItemStack feastStack = KHItems.WOODEN_PLATE.getDefaultInstance();
        feastStack.set(KHDataComponents.CUSTOM_FEAST, new CustomFeastData(
                CustomFeastData.ContainerKind.DISH, Direction.NORTH,
                List.of(placed(PackingIngredients.RED_BERRY, 5, food),
                        placed(PackingIngredients.SPRUCE_DECO, 11, food))));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0.0F);

        ((CustomFeastBlockItem) KHItems.WOODEN_PLATE)
                .finishUsingItem(feastStack, helper.getLevel(), player);

        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 2,
                "non-nutritional item ingredient added nutrition");
        helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - 1.6F) < 0.001F,
                "non-nutritional item ingredient added saturation");
        assertEffectDuration(helper, player.getEffect(MobEffects.FIRE_RESISTANCE), 1600, "fire resistance");
        assertEffectDuration(helper, player.getEffect(MobEffects.DAMAGE_RESISTANCE), 2000, "resistance");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void modelStackMultipliesNutritionWhenEaten(GameTestHelper helper) {
        GeneralConfig.Snapshot originalConfig = GeneralConfig.snapshot();
        GeneralConfig.replace(originalConfig.withHandheldDishEating(true));
        try {
            verifyModelStackNutrition(helper);
        } finally {
            GeneralConfig.replace(originalConfig);
        }
    }

    private static void verifyModelStackNutrition(GameTestHelper helper) {
        ItemStack feastStack = KHItems.WOODEN_PLATE.getDefaultInstance();
        feastStack.set(KHDataComponents.CUSTOM_FEAST, new CustomFeastData(
                CustomFeastData.ContainerKind.DISH, Direction.NORTH,
                List.of(placed(PackingIngredients.CAKE, 8,
                        new IngredientFoodData(2, 0.1F, List.of())))));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0.0F);

        ((CustomFeastBlockItem) KHItems.WOODEN_PLATE)
                .finishUsingItem(feastStack, helper.getLevel(), player);

        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 14,
                "Cake modelStack did not multiply nutrition");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void emptyHandEatsHighestIngredientFirstAndReturnsContainer(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        assert feast != null;
        IngredientFoodData food = IngredientFoodService.capture(blazeLambChop(helper));
        feast.add(PackingIngredients.RED_BERRY, 8, 8, 0, food);
        feast.add(PackingIngredients.ARDENT_CORE, 8, 8, 0, food);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0.0F);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);

        ItemInteractionResult route = helper.getLevel().getBlockState(target).useItemOn(ItemStack.EMPTY,
                helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(route == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                "empty hand was not routed to useWithoutItem");
        helper.getLevel().getBlockState(target).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertValueEqual(feast.ingredients().size(), 1, "first bite ingredient count");
        helper.assertValueEqual(feast.ingredients().getFirst().id(), PackingIngredients.RED_BERRY.getId(),
                "highest ingredient was not eaten first");
        helper.assertTrue(helper.getLevel().getBlockState(target).is(KHBlocks.PORCELAIN_PLATE),
                "container vanished before the final bite");
        helper.getLevel().getBlockState(target).useWithoutItem(helper.getLevel(), player, hit);

        helper.assertTrue(helper.getLevel().getBlockState(target).isAir(), "empty feast block remained");
        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 4, "two block bites nutrition");
        helper.assertTrue(player.getInventory().contains(KHItems.PORCELAIN_PLATE.getDefaultInstance()),
                "final bite did not return the porcelain plate");
        helper.succeed();
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void emptyHandGetsNoNutritionFromNonNutritionalIngredient(GameTestHelper helper) {
        BlockPos target = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(target, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(target);
        helper.assertTrue(feast != null, "Expected custom feast block entity");
        assert feast != null;
        IngredientFoodData food = IngredientFoodService.capture(blazeLambChop(helper));
        feast.add(PackingIngredients.SPRUCE_DECO, 8, 8, 0, food);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0.0F);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);

        helper.getLevel().getBlockState(target).useWithoutItem(helper.getLevel(), player, hit);

        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 0,
                "non-nutritional block ingredient added nutrition");
        helper.assertTrue(player.getFoodData().getSaturationLevel() == 0.0F,
                "non-nutritional block ingredient added saturation");
        helper.assertTrue(player.getEffect(MobEffects.FIRE_RESISTANCE) == null
                        && player.getEffect(MobEffects.DAMAGE_RESISTANCE) == null,
                "non-nutritional block ingredient applied an effect");
        helper.succeed();
    }

    private static FoodBiteBlock blazeLambChop(GameTestHelper helper) {
        Block block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(KaleidoscopeCookery.MOD_ID, "blaze_lamb_chop"));
        helper.assertTrue(block instanceof FoodBiteBlock, "Expected blaze lamb chop FoodBiteBlock");
        return (FoodBiteBlock) block;
    }

    private static PlacedIngredient placed(PackingIngredients ingredient, int x, IngredientFoodData food) {
        PackingIngredients.Size size = ingredient.getSize();
        return new PlacedIngredient(ingredient.getId(), x, 2, 8, size.x(), size.y(), size.z(), 0, food);
    }

    private static void assertEffectDuration(GameTestHelper helper, MobEffectInstance effect,
                                             int expected, String name) {
        helper.assertTrue(effect != null, "Missing " + name);
        assert effect != null;
        helper.assertValueEqual(effect.getDuration(), expected, name + " duration");
    }
}
