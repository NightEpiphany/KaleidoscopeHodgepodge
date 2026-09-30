package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeRecipeBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.TeaTrayBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.*;
import com.moigferdsrte.kaleidoscopehodgepodge.init.*;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.crafting.LunchBoxDyeRecipe;
import com.moigferdsrte.kaleidoscopehodgepodge.crafting.HodgepodgeRecipeResetRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder("kaleidoscope_hodgepodge")
@PrefixGameTestTemplate(false)
public final class BackportGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    @SuppressWarnings("all")
    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void exampleCommandsImportDishRecipeAndExport(GameTestHelper helper) throws Exception {
        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(TARGET)));
        String text = Files.readString(Path.of(System.getProperty("hodgepodge.testExample")));
        String code = text.substring(text.indexOf("KHP:")).strip();
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        helper.assertValueEqual(dispatcher.execute("hodgepodge import dish @s " + code, source), 54, "dish import result");
        ItemStack dish = player.getInventory().getItem(0);
        helper.assertTrue(dish.is(KHItems.MEDIAN_PORCELAIN_PLATE.get()), "Imported the wrong container");
        helper.assertValueEqual(dish.get(KHDataComponents.CUSTOM_FEAST.get()).ingredients().size(), 54, "imported dish models");
        helper.assertValueEqual(dispatcher.execute("hodgepodge export", source), 54, "dish export result");
        helper.assertValueEqual(dispatcher.execute("hodgepodge import recipe @s " + code, source), 54, "recipe import result");
        var recipe = player.getInventory().getItem(1).get(KHDataComponents.HODGEPODGE_RECIPE.get());
        helper.assertTrue(recipe != null, "Recipe import did not create a recipe");
        helper.assertValueEqual(recipe.feastCode(), code, "imported recipe code");
        helper.assertValueEqual(recipe.owner(), player.getUUID(), "imported recipe owner");
        boolean denied = false;
        try { dispatcher.execute("hodgepodge import dish @s " + code, source.withPermission(0)); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { denied = true; }
        helper.assertTrue(denied, "Import bypassed operator permission");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void mediumRecipeGuidesAcrossPartsAndAllFacings(GameTestHelper helper) {
        var block = (com.moigferdsrte.kaleidoscopehodgepodge.block.MedianPorcelainPlateBlock) KHBlocks.MEDIAN_PORCELAIN_PLATE.get();
        Player player = GameTestPlayers.create(helper, GameType.SURVIVAL);
        var targets = List.of(
                new PlacedIngredient(PackingIngredients.MUTTON.getId(), 16, 2, 8, 6, 2, 10, 1),
                new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 4, 2, 4, 2, 2, 2),
                new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 28, 2, 12, 2, 2, 2),
                new PlacedIngredient(PackingIngredients.RED_BERRY.getId(), 16, 4, 8, 2, 2, 2));
        String code = FeastCodec.encode("kaleidoscope_hodgepodge:median_porcelain_plate",
                new CustomFeastData(CustomFeastData.ContainerKind.DISH, Direction.NORTH, targets));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            // Reuse the test's loaded area to keep the test independent of neighbouring structures.
            BlockPos first = helper.absolutePos(new BlockPos(3, 1, 3));
            var state = block.defaultBlockState().setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing);
            helper.getLevel().setBlockAndUpdate(first, state);
            block.setPlacedBy(helper.getLevel(), first, state, null, new ItemStack(block));
            BlockPos second = first.relative(facing.getClockWise());
            var feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(first);
            var other = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(second);
            other.setLockedRecipe(new HodgepodgeRecipeData(code, player.getUUID()));
            for (int index = 0; index < targets.size(); index++) {
                var next = feast.nextRecipePlacement().orElseThrow();
                var firstPreview = block.recipePlacementTarget(first, state, next);
                var secondPreview = block.recipePlacementTarget(second, helper.getLevel().getBlockState(second), next);
                helper.assertValueEqual(firstPreview.translated(first.getX() * 16, first.getZ() * 16),
                        secondPreview.translated(second.getX() * 16, second.getZ() * 16), "preview world coordinates");
                ItemStack box = new ItemStack(KHItems.LUNCH_BOX.get());
                LunchBoxService.insert(box, List.of(new BaggedIngredient(next.id(), (next.rotation() + 1) % 4)));
                LunchBoxService.select(box, 0);
                LunchBoxService.setMode(box, PackingBagMode.PLACEMENT);
                player.setItemInHand(InteractionHand.MAIN_HAND, box);
                BlockPos clicked = index % 2 == 0 ? first : second;
                helper.assertTrue(helper.getLevel().getBlockState(clicked).useItemOn(box, helper.getLevel(), player,
                        InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false))
                        .consumesAction(), "Guided multiblock placement failed for " + facing);
                helper.assertTrue(LunchBoxService.get(box).isEmpty(), "Recipe did not consume selected unit");
            }
            helper.assertTrue(!feast.isRecipeLocked() && !other.isRecipeLocked(), "Stale multiblock lock");
            var expected = targets.stream().sorted(java.util.Comparator.comparingInt(PlacedIngredient::x)
                    .thenComparingInt(PlacedIngredient::y)).toList();
            var actual = feast.recipeSnapshot().ingredients().stream().map(value -> value.withFood(IngredientFoodData.EMPTY))
                    .sorted(java.util.Comparator.comparingInt(PlacedIngredient::x).thenComparingInt(PlacedIngredient::y)).toList();
            helper.assertValueEqual(actual, expected, "completed multiblock recipe");
            helper.getLevel().setBlockAndUpdate(second, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(first, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void lunchBoxPreservesInventoryDragging(GameTestHelper helper) {
        Player player = GameTestPlayers.create(helper, GameType.SURVIVAL);
        ItemStack box = new ItemStack(KHItems.LUNCH_BOX.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, box);
        LunchBoxMenu menu = new LunchBoxMenu(3, player.getInventory(), box, InteractionHand.MAIN_HAND);
        menu.setCarried(new ItemStack(Items.APPLE, 8));
        menu.clicked(-999, 0, ClickType.QUICK_CRAFT, player);
        menu.clicked(15, 1, ClickType.QUICK_CRAFT, player);
        menu.clicked(16, 1, ClickType.QUICK_CRAFT, player);
        menu.clicked(-999, 2, ClickType.QUICK_CRAFT, player);
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 4, "first dragged stack");
        helper.assertValueEqual(player.getInventory().getItem(10).getCount(), 4, "second dragged stack");
        helper.assertTrue(menu.getCarried().isEmpty(), "Inventory drag left items on cursor");
        helper.assertTrue(player.getMainHandItem() == box, "Dragging moved the open box");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void lunchBoxIngredientDraggingConservesItems(GameTestHelper helper) {
        Player player = GameTestPlayers.create(helper, GameType.SURVIVAL);
        ItemStack box = new ItemStack(KHItems.LUNCH_BOX.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, box);
        LunchBoxMenu menu = new LunchBoxMenu(4, player.getInventory(), box, InteractionHand.MAIN_HAND);
        menu.setCarried(IngredientModelService.createDisplay(PackingIngredients.RED_BERRY).copyWithCount(6));
        menu.clicked(-999, 0, ClickType.QUICK_CRAFT, player);
        menu.clicked(0, 1, ClickType.QUICK_CRAFT, player);
        menu.clicked(1, 1, ClickType.QUICK_CRAFT, player);
        menu.clicked(-999, 2, ClickType.QUICK_CRAFT, player);
        helper.assertValueEqual(LunchBoxService.get(box).unitCount(), 6, "dragged ingredient units");
        helper.assertTrue(menu.getCarried().isEmpty(), "Ingredient drag duplicated cursor contents");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void lunchBoxShiftTransferPreservesCursor(GameTestHelper helper) {
        Player player = GameTestPlayers.create(helper, GameType.SURVIVAL);
        ItemStack box = new ItemStack(KHItems.LUNCH_BOX.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, box);
        player.getInventory().setItem(9,
                IngredientModelService.createDisplay(PackingIngredients.RED_BERRY).copyWithCount(6));
        LunchBoxMenu menu = new LunchBoxMenu(5, player.getInventory(), box, InteractionHand.MAIN_HAND);
        ItemStack carried = new ItemStack(Items.DIAMOND, 3);
        menu.setCarried(carried);
        menu.clicked(15, 0, ClickType.QUICK_MOVE, player);
        helper.assertValueEqual(LunchBoxService.get(box).unitCount(), 6, "shift-transferred units");
        helper.assertTrue(menu.getCarried() == carried && carried.getCount() == 3, "Shift transfer lost cursor contents");
        helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "Shift transfer duplicated ingredients");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void lightingNeverLoadsStructureChunks(GameTestHelper helper) {
        var unreadable = (net.minecraft.world.level.BlockGetter) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{net.minecraft.world.level.BlockGetter.class},
                (proxy, method, args) -> { throw new AssertionError("Light query accessed world: " + method.getName()); });
        for (var block : List.of(KHBlocks.WOODEN_PLATE.get(), KHBlocks.PORCELAIN_PLATE.get(),
                KHBlocks.MEDIAN_PORCELAIN_PLATE.get(), KHBlocks.LARGE_PORCELAIN_PLATE.get(), KHBlocks.PORCELAIN_SOUP_BOWL.get())) {
            var state = block.defaultBlockState();
            helper.assertTrue(state.propagatesSkylightDown(unreadable, BlockPos.ZERO), "Dry plate blocked skylight");
            helper.assertValueEqual(state.getLightBlock(unreadable, BlockPos.ZERO), 0, "dry opacity");
            state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true);
            helper.assertValueEqual(state.getLightBlock(unreadable, BlockPos.ZERO), 1, "water opacity");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void partialBagInsertionKeepsRejectedIngredients(GameTestHelper helper) {
        Player player = GameTestPlayers.create(helper, GameType.SURVIVAL);
        ItemStack box = new ItemStack(KHItems.LUNCH_BOX.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, box);
        BaggedIngredient berry = new BaggedIngredient(PackingIngredients.RED_BERRY.getId());
        BaggedIngredient mutton = new BaggedIngredient(PackingIngredients.MUTTON.getId());
        LunchBoxService.set(box, LunchBoxContents.EMPTY.insert(Collections.nCopies(16, berry)).contents());
        LunchBoxMenu menu = new LunchBoxMenu(1, player.getInventory(), box, InteractionHand.MAIN_HAND);
        ItemStack bag = new ItemStack(KHItems.WRAPPING_BAG.get());
        PackingBagService.set(bag, new PackingBagContents(List.of(berry, mutton)));
        menu.setCarried(bag);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertValueEqual(PackingBagService.get(bag).ingredients(), List.of(berry), "rejected bag contents");
        helper.assertValueEqual(LunchBoxService.get(box).unitCount(), 17, "accepted units");
        helper.assertValueEqual(LunchBoxService.get(box).first(1).orElseThrow(), mutton, "accepted model");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void lunchBoxProtectsOwnerAndPreservesSelection(GameTestHelper helper) {
        Player player = GameTestPlayers.create(helper, GameType.SURVIVAL);
        ItemStack box = new ItemStack(KHItems.LUNCH_BOX.get());
        player.setItemInHand(InteractionHand.OFF_HAND, box);
        LunchBoxService.insert(box, List.of(new BaggedIngredient(PackingIngredients.RED_BERRY.getId())));
        LunchBoxMenu menu = new LunchBoxMenu(2, player.getInventory(), box, InteractionHand.OFF_HAND);
        menu.selectFromNetwork(0, player);
        menu.clicked(15, 40, ClickType.SWAP, player);
        helper.assertTrue(player.getOffhandItem() == box, "Offhand box moved while open");
        helper.assertValueEqual(LunchBoxService.selectedSlot(box), 0, "selection");
        menu.clickMenuButton(player, 0);
        helper.assertValueEqual(LunchBoxService.getMode(box), PackingBagMode.PLACEMENT, "mode");
        menu.selectFromNetwork(Integer.MAX_VALUE, player);
        helper.assertValueEqual(LunchBoxService.selectedSlot(box), -1, "invalid selection");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void legacyLunchBoxMigrationPreservesOverflowAndShells(GameTestHelper helper) {
        Player player = GameTestPlayers.create(helper, GameType.SURVIVAL);
        ItemStack box = new ItemStack(KHItems.LUNCH_BOX.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, box);
        BaggedIngredient berry = new BaggedIngredient(PackingIngredients.RED_BERRY.getId());
        ItemStack bag = new ItemStack(KHItems.WRAPPING_BAG.get());
        PackingBagService.set(bag, new PackingBagContents(Collections.nCopies(9, berry)));
        var slots = net.minecraft.core.NonNullList.withSize(256, ItemStack.EMPTY);
        slots.set(0, bag);
        slots.set(1, bag.copy());
        slots.set(2, IngredientModelService.createDisplay(berry).copyWithCount(2));
        slots.set(255, new ItemStack(Items.DIAMOND, 3));
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        LunchBoxService.migrateIfNeeded(box, player);
        helper.assertValueEqual(LunchBoxService.get(box).unitCount(), 16, "migrated units");
        helper.assertTrue(!box.has(DataComponents.CONTAINER), "Legacy component remained");
        int remainder = 0, shells = 0, diamonds = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(KHItems.WRAPPING_BAG.get())) {
                shells += stack.getCount();
                remainder += PackingBagService.get(stack).ingredients().size();
            }
            if (stack.is(KHItems.INGREDIENT_DISPLAY.get())) remainder += stack.getCount();
            if (stack.is(Items.DIAMOND)) diamonds += stack.getCount();
        }
        helper.assertValueEqual(remainder, 4, "overflow ingredients");
        helper.assertValueEqual(shells, 2, "bag shells");
        helper.assertValueEqual(diamonds, 3, "last legacy slot");
        LunchBoxService.migrateIfNeeded(box, player);
        helper.assertValueEqual(LunchBoxService.get(box).unitCount(), 16, "idempotent migration");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void dyeRecipeKeepsContentsAndName(GameTestHelper helper) {
        ItemStack box = new ItemStack(KHItems.LUNCH_BOX.get());
        LunchBoxService.insert(box, List.of(new BaggedIngredient(PackingIngredients.RED_BERRY.getId())));
        LunchBoxService.select(box, 0);
        box.set(DataComponents.CUSTOM_NAME, Component.literal("Lunch"));
        CraftingInput input = CraftingInput.of(2, 1, List.of(box, new ItemStack(Items.RED_DYE)));
        LunchBoxDyeRecipe recipe = new LunchBoxDyeRecipe(CraftingBookCategory.EQUIPMENT);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Dye recipe did not match");
        ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertValueEqual(LunchBoxService.get(result), LunchBoxService.get(box), "dyed contents");
        helper.assertValueEqual(LunchBoxService.selectedSlot(result), 0, "dyed selection");
        helper.assertValueEqual(result.getHoverName().getString(), "Lunch", "dyed name");
        helper.assertTrue(result.has(DataComponents.DYED_COLOR), "Missing color component");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void recipeRoundTripPreservesAuthorAndDishName(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(pos, KHBlocks.PORCELAIN_PLATE.get().defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(pos);
        feast.add(PackingIngredients.RED_BERRY, 8, 8);
        feast.setDishName(Optional.of(Component.literal("Berry dish")));
        Player player = GameTestPlayers.create(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RECIPE_ITEM.get(), 2));
        var context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                player.getMainHandItem(), new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        helper.assertTrue(ModItems.RECIPE_ITEM.get().useOn(context).consumesAction(), "Recording failed");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "unused recipe paper");
        ItemStack recipe = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(KHItems.HODGEPODGE_RECIPE.get()))
                recipe = player.getInventory().getItem(slot);
        }
        helper.assertTrue(!recipe.isEmpty(), "Recorded recipe missing");
        var data = recipe.get(KHDataComponents.HODGEPODGE_RECIPE.get());
        helper.assertValueEqual(data.owner(), player.getUUID(), "recipe author");
        helper.assertValueEqual(data.dishName().orElseThrow().getString(), "Berry dish", "recorded name");
        var entity = new HodgepodgeRecipeBlockEntity(pos, KHBlocks.HODGEPODGE_RECIPE.get().defaultBlockState());
        entity.setItem(recipe);
        var restored = new HodgepodgeRecipeBlockEntity(pos, entity.getBlockState());
        restored.loadWithComponents(entity.saveWithFullMetadata(helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        helper.assertValueEqual(restored.recipe(), data, "recipe NBT round trip");
        var reset = new HodgepodgeRecipeResetRecipe(CraftingBookCategory.MISC);
        var input = CraftingInput.of(1, 1, List.of(recipe));
        helper.assertTrue(reset.matches(input, helper.getLevel()), "Recorded recipe cannot be reset");
        helper.assertTrue(reset.assemble(input, helper.getLevel().registryAccess()).is(ModItems.RECIPE_ITEM.get()),
                "Wrong reset output");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void lockedRecipeUsesItsOwnRotationAndFinishes(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(pos, KHBlocks.PORCELAIN_PLATE.get().defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(pos);
        var target = PlacementSpace.place(List.of(), PackingIngredients.MUTTON, 8, 8, 40, 2,
                feast.placementBounds(), 1).placement().orElseThrow();
        String code = FeastCodec.encode("kaleidoscope_hodgepodge:porcelain_plate",
                new CustomFeastData(CustomFeastData.ContainerKind.DISH, Direction.NORTH, List.of(target)));
        feast.setLockedRecipe(new HodgepodgeRecipeData(code, UUID.randomUUID(), Optional.empty(),
                Optional.of(Component.literal("Named recipe"))));
        Player player = GameTestPlayers.create(helper, GameType.SURVIVAL);
        ItemStack box = new ItemStack(KHItems.LUNCH_BOX.get());
        LunchBoxService.insert(box, List.of(new BaggedIngredient(PackingIngredients.MUTTON.getId())));
        LunchBoxService.select(box, 0);
        LunchBoxService.setMode(box, PackingBagMode.PLACEMENT);
        player.setItemInHand(InteractionHand.MAIN_HAND, box);
        var state = helper.getLevel().getBlockState(pos);
        helper.assertTrue(state.useItemOn(box, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)).consumesAction(), "Guided placement failed");
        helper.assertValueEqual(feast.ingredients().getFirst().rotation(), 1, "guided rotation");
        helper.assertTrue(!feast.isRecipeLocked(), "Completed recipe remained locked");
        helper.assertTrue(LunchBoxService.get(box).isEmpty(), "Placed ingredient remained in box");
        helper.assertValueEqual(feast.dishName().orElseThrow().getString(), "Named recipe", "completed recipe name");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void teaTrayPreservesFourIndependentSlots(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(pos, KHBlocks.TEA_TRAY.get().defaultBlockState());
        TeaTrayBlockEntity tray = (TeaTrayBlockEntity) helper.getLevel().getBlockEntity(pos);
        ItemStack cup = new ItemStack(ModItems.EMPTY_CUP.get());
        for (int slot = 0; slot < 4; slot++) helper.assertTrue(tray.insert(cup, slot), "Cup insertion failed");
        helper.assertTrue(!tray.insert(cup, 0), "Tray accepted duplicate slot");
        helper.assertValueEqual(tray.removeAt(2).getCount(), 1, "removed cup");
        var saved = tray.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored = new TeaTrayBlockEntity(pos, tray.getBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.hasCup(0) && restored.hasCup(1) && !restored.hasCup(2) && restored.hasCup(3),
                "Cup slots changed on reload");
        helper.assertTrue(!restored.insert(new ItemStack(Items.STONE), 2), "Tray accepted a non-cup");
        helper.assertTrue(restored.insert(cup, 2), "Freed cup slot was not reusable");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void exampleCodeRoundTripsWithEffects(GameTestHelper helper) throws Exception {
        String text = Files.readString(Path.of(System.getProperty("hodgepodge.testExample")));
        String code = text.substring(text.indexOf("KHP:")).strip();
        var decoded = FeastCodec.decode(code);
        helper.assertValueEqual(decoded.feast().ingredients().size(), 54, "example model count");
        var roundTrip = FeastCodec.decode(FeastCodec.encode(decoded.containerPath(), decoded.feast()));
        helper.assertValueEqual(roundTrip, decoded, "example round trip");
        helper.assertTrue(decoded.feast().ingredients().stream().anyMatch(value -> !value.food().effects().isEmpty()),
                "Example effects were lost");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft")
    public void malformedCodesAreRejectedWithoutAllocatingUnboundedLists(GameTestHelper helper) {
        String header = "KHP:1|kaleidoscope_hodgepodge:porcelain_plate|d|n|1|";
        String ingredient = "kaleidoscope_hodgepodge:red_berry;8;2;8;2;2;2;0;1;";
        for (String code : List.of(header + ingredient + "NaN;",
                header + ingredient + "1;1!2147483647:minecraft:speed@1@0@false@true@true;",
                header + ingredient + "1;1!-1:;",
                "KHP:1|minecraft:air|d|n|2147483647")) {
            boolean rejected = false;
            try { FeastCodec.decode(code); }
            catch (FeastCodec.FormatException expected) { rejected = true; }
            helper.assertTrue(rejected, "Malformed code was accepted");
        }
        helper.succeed();
    }
}
