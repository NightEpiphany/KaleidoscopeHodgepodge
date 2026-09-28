package com.moigferdsrte.kaleidoscopehodgepodge.smoketest;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeRecipeBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.TeaTrayBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.client.screen.LunchBoxScreen;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientHodgepodgeRecipeTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.client.tooltip.ClientLunchBoxTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.core.*;
import com.moigferdsrte.kaleidoscopehodgepodge.init.*;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.HodgepodgeRecipeTooltip;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.LunchBoxTooltip;
import com.mojang.authlib.GameProfile;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class SmokeScreen extends Screen {
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final String[] PAGES = {"ingredients", "tooltips", "lunchbox", "blocks"};
    private final List<PackingIngredients> ingredients = PackingIngredientRegistry.all().values().stream()
            .sorted(java.util.Comparator.comparing(value -> value.getId().toString())).toList();
    private final UUID owner = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private final ItemStack bag = new ItemStack(KHItems.WRAPPING_BAG);
    private final ItemStack lunchBox = new ItemStack(KHItems.LUNCH_BOX);
    private final ItemStack dish;
    private final HodgepodgeRecipeData recipe;
    private final ClientHodgepodgeRecipeTooltip recipeTooltip;
    private final ClientLunchBoxTooltip lunchTooltip;
    private LunchBoxScreen lunchScreen;
    private int page;
    private int frames;
    private boolean advance;

    SmokeScreen() {
        super(Component.literal("Hodgepodge 1.21.1 visual smoke test"));
        try {
            String text = Files.readString(Path.of(System.getProperty("hodgepodge.testExample")));
            String code = text.substring(text.indexOf("KHP:")).strip();
            var decoded = FeastCodec.decode(code);
            dish = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(decoded.containerPath())));
            dish.set(KHDataComponents.CUSTOM_FEAST, decoded.feast());
            recipe = new HodgepodgeRecipeData(code, owner,
                    Optional.of(new GameProfile(owner, "SmokeTest")), Optional.of(Component.literal("Example / 54 ingredients")));
            recipeTooltip = new ClientHodgepodgeRecipeTooltip(new HodgepodgeRecipeTooltip(dish,
                    decoded.feast().ingredients(), owner, recipe.ownerProfile(), recipe.dishName()));
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot load example recipe", failure);
        }
        PackingBagService.set(bag, new PackingBagContents(List.of(new BaggedIngredient(PackingIngredients.MUTTON.getId()),
                new BaggedIngredient(PackingIngredients.RED_BERRY.getId()))));
        LunchBoxService.insert(lunchBox, ingredients.stream().limit(15)
                .map(value -> new BaggedIngredient(value.getId())).toList());
        LunchBoxService.select(lunchBox, 0);
        lunchBox.set(DataComponents.DYED_COLOR, new DyedItemColor(0xCF4261, false));
        lunchTooltip = new ClientLunchBoxTooltip(new LunchBoxTooltip(
                Optional.of(ingredients.getFirst().getId()), 15, 15));
    }

    @Override
    protected void init() {
        Inventory inventory = new Inventory(null);
        LunchBoxMenu menu = new LunchBoxMenu(1, inventory);
        for (int slot = 0; slot < 15; slot++) menu.getSlot(slot).set(
                IngredientModelService.createDisplay(ingredients.get(slot)).copyWithCount(slot + 1));
        menu.setClientSelectedSlot(4);
        lunchScreen = new LunchBoxScreen(menu, inventory, Component.translatable("item.kaleidoscope_hodgepodge.lunch_box"));
        lunchScreen.init(minecraft, width, height);
    }

    @Override
    public void tick() {
        if (!advance) return;
        advance = false;
        frames = 0;
        if (++page == PAGES.length) {
            KaleidoscopeHodgepodge.LOGGER.info("CLIENT_SMOKE: all four rendering pages captured successfully");
            minecraft.stop();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (page >= PAGES.length) return;
        graphics.fill(0, 0, width, height, 0xFF343A40);
        graphics.drawString(font, title.copy().append(" / " + PAGES[page]), 16, 12, 0xFFFFFFFF);
        switch (page) {
            case 0 -> renderIngredients(graphics);
            case 1 -> renderTooltips(graphics);
            case 2 -> lunchScreen.render(graphics, -1, -1, partialTick);
            case 3 -> renderBlocks(graphics);
            default -> throw new IllegalStateException();
        }
        graphics.flush();
        if (++frames == 40) {
            try {
                Path path = minecraft.gameDirectory.toPath().resolve("screenshots/" + PAGES[page] + ".png");
                Files.createDirectories(path.getParent());
                try (var image = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                    image.writeToFile(path);
                }
                KaleidoscopeHodgepodge.LOGGER.info("CLIENT_SMOKE: saved {}", path.toAbsolutePath());
                advance = true;
            } catch (Exception failure) {
                throw new IllegalStateException("Cannot capture smoke screenshot", failure);
            }
        }
    }

    private void renderIngredients(GuiGraphics graphics) {
        for (int index = 0; index < ingredients.size(); index++) {
            int x = 20 + index % 14 * 42;
            int y = 48 + index / 14 * 46;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(2, 2, 1);
            graphics.renderItem(IngredientModelService.createDisplay(ingredients.get(index)), 0, 0);
            graphics.pose().popPose();
            graphics.drawString(font, Integer.toString(index + 1), x, y + 32, 0xFFBBBBBB);
        }
    }

    private void renderTooltips(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(24, 40, 0);
        graphics.pose().scale(4, 4, 1);
        graphics.renderItem(bag, 0, 0);
        graphics.renderItem(lunchBox, 22, 0);
        graphics.renderItem(dish, 46, 0);
        graphics.pose().popPose();
        graphics.pose().pushPose();
        graphics.pose().translate(300, 72, 200);
        graphics.pose().scale(64, -64, 64);
        var openBox = ((FabricBakedModelManager) minecraft.getModelManager()).getModel(
                KaleidoscopeHodgepodge.id("item/lunch_box_open"));
        minecraft.getItemRenderer().render(lunchBox, ItemDisplayContext.GUI, false, graphics.pose(),
                graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, openBox);
        graphics.flush();
        graphics.pose().popPose();
        recipeTooltip.renderImage(font, 24, 140, graphics);
        lunchTooltip.renderImage(font, 370, 140, graphics);
    }

    private void renderBlocks(GuiGraphics graphics) {
        for (int column = 0; column < FACINGS.length; column++) {
            int x = 82 + column * 155;
            graphics.drawCenteredString(font, FACINGS[column].getName(), x, 40, 0xFFFFFFFF);
            var state = KHBlocks.TEA_TRAY.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, FACINGS[column]);
            var tray = new TeaTrayBlockEntity(BlockPos.ZERO, state);
            tray.insert(new ItemStack(ModItems.EMPTY_CUP), 0);
            for (int slot = 1; slot < 4; slot++) {
                String cup = List.of("barley_tea", "biluochun", "flower_tea").get(slot - 1);
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", cup));
                ClientSmoke.require(tray.insert(new ItemStack(item), slot), "Cannot insert cup: " + cup);
            }
            renderBlock(graphics, tray, x, 98, 50, 30);
            int row = 0;
            for (AttachFace face : AttachFace.values()) {
                var paperState = KHBlocks.HODGEPODGE_RECIPE.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, FACINGS[column])
                        .setValue(BlockStateProperties.ATTACH_FACE, face);
                var paper = new HodgepodgeRecipeBlockEntity(BlockPos.ZERO, paperState);
                paper.setRecipe(recipe);
                int y = 195 + row++ * 100;
                graphics.drawCenteredString(font, face.getSerializedName(), x, y - 40, 0xFFBBBBBB);
                renderBlock(graphics, paper, x, y, 62, face == AttachFace.CEILING ? -30 : 30);
            }
        }
    }

    private void renderBlock(GuiGraphics graphics, BlockEntity entity, int x, int y, float scale, float pitch) {
        graphics.flush();
        var poses = graphics.pose();
        poses.pushPose();
        poses.translate(x, y, 200);
        poses.scale(scale, -scale, scale);
        poses.mulPose(Axis.XP.rotationDegrees(pitch));
        poses.mulPose(Axis.YP.rotationDegrees(-35));
        poses.translate(-0.5, -0.5, -0.5);
        minecraft.getBlockRenderer().renderSingleBlock(entity.getBlockState(), poses,
                graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        minecraft.getBlockEntityRenderDispatcher().renderItem(entity, poses,
                graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        graphics.flush();
        poses.popPose();
    }
}
