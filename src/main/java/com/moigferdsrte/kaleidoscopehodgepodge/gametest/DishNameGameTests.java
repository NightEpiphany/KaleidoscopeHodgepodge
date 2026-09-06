package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.moigferdsrte.kaleidoscopehodgepodge.block.AbstractHodgepodgeFeastBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.LargePorcelainPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.MedianPorcelainPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.BaggedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagContents;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagMode;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PackingBagService;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.HodgepodgeRecipeTooltip;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class DishNameGameTests {
    private static final BlockPos TARGET = new BlockPos(3, 1, 3);
    private static final Component NAME = Component.literal("Chef's Special");

    @GameTest
    public void namedTagsRequireFoodAndPersistAcrossSingleBlockDrops(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        List<Block> containers = List.of(KHBlocks.WOODEN_PLATE, KHBlocks.PORCELAIN_PLATE, KHBlocks.PORCELAIN_SOUP_BOWL);
        for (int index = 0; index < containers.size(); index++) {
            AbstractHodgepodgeFeastBlock block = (AbstractHodgepodgeFeastBlock) containers.get(index);
            BlockPos pos = helper.absolutePos(TARGET.offset(index * 3, 0, 0));
            place(helper, block, pos, block.defaultBlockState(), new ItemStack(block));
            ItemStack tag = namedTag(NAME);
            helper.assertValueEqual(use(helper, player, pos, tag), InteractionResult.PASS, "empty dish naming");
            helper.assertValueEqual(tag.getCount(), 1, "empty dish tag consumption");
            helper.assertTrue(feast(helper, pos).add(PackingIngredients.RED_BERRY, 8, 8).success(), "food placement");
            ItemStack blank = new ItemStack(Items.NAME_TAG);
            helper.assertValueEqual(use(helper, player, pos, blank), InteractionResult.PASS, "unnamed tag");
            helper.assertValueEqual(use(helper, player, pos, namedTag(Component.literal("  "))),
                    InteractionResult.PASS, "blank tag");
            helper.assertTrue(use(helper, player, pos, tag).consumesAction(), "named tag rejected");
            helper.assertTrue(tag.isEmpty(), "named tag not consumed");
            reload(helper, pos);
            helper.assertValueEqual(feast(helper, pos).dishName(), Optional.of(NAME), "saved name");
            ItemStack drop = drop(helper, pos);
            helper.assertValueEqual(drop.get(KHDataComponents.DISH_NAME), NAME, "custom name component");
            helper.assertValueEqual(drop.getHoverName(), NAME, "dish item name");
            BlockPos restored = pos.south(3);
            place(helper, block, restored, block.defaultBlockState(), drop);
            helper.assertValueEqual(feast(helper, restored).dishName(), Optional.of(NAME), "replaced name");
        }
        helper.succeed();
    }

    @GameTest
    public void anvilNamesAndRemovalSynchronizeDishComponentAndRecipeTooltip(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AbstractHodgepodgeFeastBlock block = (AbstractHodgepodgeFeastBlock) KHBlocks.PORCELAIN_PLATE;
        BlockPos source = helper.absolutePos(TARGET);
        place(helper, block, source, block.defaultBlockState(), new ItemStack(block));
        feast(helper, source).add(PackingIngredients.RED_BERRY, 8, 8);
        ItemStack unnamedRecipe = record(helper, player, source);
        helper.assertTrue(tooltip(unnamedRecipe).dishName().isEmpty(), "Unnamed recipe has name row");
        ItemStack named = anvil(player, drop(helper, source), NAME.getString());
        helper.assertValueEqual(named.get(KHDataComponents.DISH_NAME), NAME, "anvil dish component");
        BlockPos target = source.south(3);
        place(helper, block, target, block.defaultBlockState(), named);
        ItemStack recipe = record(helper, player, target);
        helper.assertValueEqual(recipe.get(KHDataComponents.HODGEPODGE_RECIPE).dishName(), Optional.of(NAME), "recorded anvil name");
        helper.assertValueEqual(tooltip(recipe).dishName(), Optional.of(NAME), "recipe name row");
        helper.assertValueEqual(tooltip(recipe).preview().get(KHDataComponents.DISH_NAME), NAME, "preview name");
        ItemStack renamed = anvil(player, named, "New Dish");
        helper.assertValueEqual(renamed.get(KHDataComponents.DISH_NAME), Component.literal("New Dish"), "anvil rename");
        ItemStack cleared = anvil(player, renamed, "");
        helper.assertTrue(!cleared.isEmpty() && !cleared.has(KHDataComponents.DISH_NAME)
                && !cleared.has(DataComponents.CUSTOM_NAME), "Anvil removal left stale name");
        helper.succeed();
    }

    @GameTest
    public void multiBlockRecipeNamesRestoreOnCancelAndPersistOnCompletion(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        MedianPorcelainPlateBlock block = (MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE;
        BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(MedianPorcelainPlateBlock.PART, MedianPorcelainPlateBlock.Part.LEFT);
        BlockPos source = helper.absolutePos(TARGET);
        place(helper, block, source, state, new ItemStack(block));
        feast(helper, source.east()).add(PackingIngredients.RED_BERRY, 8, 8);
        ItemStack tag = namedTag(NAME);
        use(helper, player, source, tag);
        helper.assertTrue(tag.isEmpty(), "Empty half did not name whole dish");
        ItemStack recipe = record(helper, player, source.east());
        helper.assertValueEqual(tooltip(recipe).dishName(), Optional.of(NAME), "recorded tag name");
        helper.assertValueEqual(drop(helper, source.east()).get(KHDataComponents.DISH_NAME), NAME, "multi-block drop name");
        BlockPos target = source.south(3);
        ItemStack independentlyNamed = new ItemStack(block);
        independentlyNamed.set(DataComponents.CUSTOM_NAME, Component.literal("My Plate"));
        place(helper, block, target, state, independentlyNamed);
        useRecipe(player, target.east(), recipe);
        helper.assertValueEqual(feast(helper, target).dishName(), Optional.of(NAME), "inherited name");
        ItemStack forbiddenTag = namedTag(Component.literal("Override"));
        helper.assertValueEqual(use(helper, player, target.east(), forbiddenTag), InteractionResult.FAIL, "renaming locked dish");
        helper.assertValueEqual(forbiddenTag.getCount(), 1, "locked tag consumption");
        reload(helper, target);
        useRecipe(player, target, recipe);
        helper.assertValueEqual(feast(helper, target.east()).dishName(), Optional.of(Component.literal("My Plate")),
                "cancel should restore independent name");
        feast(helper, target).setDishName(Optional.empty());
        useRecipe(player, target, recipe);
        useRecipe(player, target.east(), recipe);
        helper.assertTrue(feast(helper, target).dishName().isEmpty(), "cancel left inherited name");
        useRecipe(player, target, recipe);
        ItemStack bag = new ItemStack(KHItems.WRAPPING_BAG);
        PackingBagService.set(bag, PackingBagContents.single(new BaggedIngredient(PackingIngredients.RED_BERRY.getId())));
        PackingBagService.setMode(bag, PackingBagMode.PLACEMENT);
        helper.assertTrue(use(helper, player, target, bag).consumesAction(), "recipe placement failed");
        helper.assertTrue(!feast(helper, target).isRecipeLocked(), "recipe did not complete");
        reload(helper, target);
        helper.assertValueEqual(feast(helper, target.east()).dishName(), Optional.of(NAME), "completed recipe name");
        helper.assertValueEqual(drop(helper, target.east()).get(KHDataComponents.DISH_NAME), NAME, "completed dish drop name");
        helper.succeed();
    }

    @GameTest
    public void largePlateSharesNameAcrossNineParts(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        LargePorcelainPlateBlock block = (LargePorcelainPlateBlock) KHBlocks.LARGE_PORCELAIN_PLATE;
        BlockPos center = helper.absolutePos(TARGET);
        place(helper, block, center, block.defaultBlockState(), new ItemStack(block));
        feast(helper, center).add(PackingIngredients.RED_BERRY, 8, 8);
        use(helper, player, center.south().east(), namedTag(NAME));
        for (LargePorcelainPlateBlock.Part part : LargePorcelainPlateBlock.Part.values()) {
            BlockPos pos = center.offset(part.x(), 0, part.z());
            helper.assertValueEqual(feast(helper, pos).dishName(), Optional.of(NAME), "large plate name " + part);
            helper.assertValueEqual(drop(helper, pos).get(KHDataComponents.DISH_NAME), NAME, "large plate drop " + part);
        }
        helper.succeed();
    }

    @GameTest
    public void namedDishAndRecipePreserveStyledNamesInStorageAndNetwork(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(TARGET);
        AbstractHodgepodgeFeastBlock block = (AbstractHodgepodgeFeastBlock) KHBlocks.PORCELAIN_PLATE;
        place(helper, block, pos, block.defaultBlockState(), new ItemStack(block));
        feast(helper, pos).add(PackingIngredients.RED_BERRY, 8, 8);
        Component styledName = Component.literal("Special Dish").withStyle(ChatFormatting.GOLD);
        use(helper, player, pos, namedTag(styledName));
        ItemStack recipe = record(helper, player, pos);
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for (ItemStack original : List.of(drop(helper, pos), recipe)) {
            var saved = ItemStack.CODEC.encodeStart(ops, original).getOrThrow();
            ItemStack restored = ItemStack.CODEC.parse(ops, saved).getOrThrow();
            helper.assertTrue(ItemStack.isSameItemSameComponents(original, restored), "stored name components");
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(buffer, restored);
                ItemStack received = ItemStack.STREAM_CODEC.decode(buffer);
                helper.assertTrue(ItemStack.isSameItemSameComponents(original, received), "network name components");
                Component receivedName = received.is(KHItems.HODGEPODGE_RECIPE)
                        ? tooltip(received).dishName().orElseThrow() : received.get(KHDataComponents.DISH_NAME);
                helper.assertValueEqual(receivedName, styledName, "styled dish name");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    private static ItemStack anvil(Player player, ItemStack input, String name) {
        AnvilMenu menu = new AnvilMenu(1, player.getInventory());
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(input.copy());
        menu.setItemName(name);
        return menu.getSlot(AnvilMenu.RESULT_SLOT).getItem().copy();
    }

    private static ItemStack namedTag(Component name) {
        ItemStack stack = new ItemStack(Items.NAME_TAG);
        stack.set(DataComponents.CUSTOM_NAME, name.copy());
        return stack;
    }

    private static void place(GameTestHelper helper, AbstractHodgepodgeFeastBlock block, BlockPos pos,
                              BlockState state, ItemStack stack) {
        helper.getLevel().setBlockAndUpdate(pos, state);
        block.setPlacedBy(helper.getLevel(), pos, state, null, stack);
    }

    private static ItemStack drop(GameTestHelper helper, BlockPos pos) {
        return Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, feast(helper, pos)).getFirst();
    }

    private static void reload(GameTestHelper helper, BlockPos pos) {
        HodgepodgeFeastBlockEntity entity = feast(helper, pos);
        var saved = entity.getUpdateTag(helper.getLevel().registryAccess());
        entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
    }

    private static ItemStack record(GameTestHelper helper, Player player, BlockPos pos) {
        ItemStack empty = new ItemStack(ModItems.RECIPE_ITEM);
        player.setItemInHand(InteractionHand.MAIN_HAND, empty);
        empty.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
        helper.assertTrue(player.getMainHandItem().is(KHItems.HODGEPODGE_RECIPE), "record failed");
        return player.getMainHandItem().copy();
    }

    private static HodgepodgeRecipeTooltip tooltip(ItemStack recipe) {
        return (HodgepodgeRecipeTooltip) recipe.getItem().getTooltipImage(recipe).orElseThrow();
    }

    private static void useRecipe(Player player, BlockPos pos, ItemStack recipe) {
        player.setItemInHand(InteractionHand.MAIN_HAND, recipe.copy());
        recipe.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos)));
    }

    private static InteractionResult use(GameTestHelper helper, Player player, BlockPos pos, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockState state = helper.getLevel().getBlockState(pos);
        return ((AbstractHodgepodgeFeastBlock) state.getBlock()).useItemOn(stack, state, helper.getLevel(), pos,
                player, InteractionHand.MAIN_HAND, hit(pos));
    }

    private static HodgepodgeFeastBlockEntity feast(GameTestHelper helper, BlockPos pos) {
        return (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
