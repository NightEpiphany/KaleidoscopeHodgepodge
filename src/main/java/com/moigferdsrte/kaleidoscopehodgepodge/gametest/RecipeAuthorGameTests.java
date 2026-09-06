package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.google.common.collect.ImmutableMultimap;
import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgeRecipeBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeRecipeBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.core.HodgepodgeRecipeData;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHDataComponents;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHItems;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import com.moigferdsrte.kaleidoscopehodgepodge.inventory.tooltip.HodgepodgeRecipeTooltip;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class RecipeAuthorGameTests {
    private static final BlockPos TARGET = new BlockPos(2, 1, 2);
    private static final Property TEXTURES = new Property("textures", "stored-skin-texture", "stored-skin-signature");

    @GameTest
    public void serverRecordsAuthorAndTransfersProfileWithoutOnlineCreator(GameTestHelper helper) {
        ItemStack recipe = record(helper);
        HodgepodgeRecipeData original = recipe.get(KHDataComponents.HODGEPODGE_RECIPE);
        helper.assertTrue(original != null && original.ownerProfile().isPresent(), "Server did not record author profile");
        helper.assertTrue(helper.getLevel().getServer().getPlayerList().getPlayer(original.owner()) == null,
                "Author must be absent from online player list for this test");
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var saved = ItemStack.CODEC.encodeStart(ops, recipe).getOrThrow();
        ItemStack restored = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        ItemStack received;
        try {
            ItemStack.STREAM_CODEC.encode(buffer, restored);
            received = ItemStack.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
        Player recipient = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!recipient.getUUID().equals(original.owner()), "Recipient must be a different player");
        recipient.setItemInHand(InteractionHand.MAIN_HAND, received);
        helper.assertValueEqual(received.get(KHDataComponents.HODGEPODGE_RECIPE), original, "transferred author data");
        HodgepodgeRecipeTooltip tooltip = (HodgepodgeRecipeTooltip) received.getItem().getTooltipImage(received).orElseThrow();
        helper.assertValueEqual(tooltip.owner(), original.owner(), "tooltip author UUID");
        GameProfile profile = tooltip.ownerProfile().orElseThrow();
        helper.assertValueEqual(profile.name(), "OriginalChef", "offline creator name");
        helper.assertValueEqual(profile.properties().get("textures").iterator().next(), TEXTURES,
                "offline creator texture and signature");
        helper.succeed();
    }

    @GameTest
    public void placedRecipeAndPlateLockRetainAuthorThroughReload(GameTestHelper helper) {
        ItemStack recipe = record(helper);
        HodgepodgeRecipeData original = recipe.get(KHDataComponents.HODGEPODGE_RECIPE);
        BlockPos recipePos = helper.absolutePos(TARGET.east(2));
        HodgepodgeRecipeBlock block = (HodgepodgeRecipeBlock) KHBlocks.HODGEPODGE_RECIPE;
        helper.getLevel().setBlockAndUpdate(recipePos, block.defaultBlockState());
        HodgepodgeRecipeBlockEntity entity = (HodgepodgeRecipeBlockEntity) helper.getLevel().getBlockEntity(recipePos);
        entity.setItem(recipe);
        var saved = entity.getUpdateTag(helper.getLevel().registryAccess());
        entity.setItem(ItemStack.EMPTY);
        entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(), saved));
        helper.assertValueEqual(entity.recipe(), original, "placed recipe author after reload");
        Player recipient = helper.makeMockPlayer(GameType.SURVIVAL);
        block.useItemOn(ItemStack.EMPTY, entity.getBlockState(), helper.getLevel(), recipePos, recipient,
                InteractionHand.MAIN_HAND, hit(recipePos));
        helper.assertValueEqual(recipient.getMainHandItem().get(KHDataComponents.HODGEPODGE_RECIPE), original,
                "author after another player retrieves recipe");

        BlockPos platePos = helper.absolutePos(TARGET.south(2));
        helper.getLevel().setBlockAndUpdate(platePos, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        recipient.getMainHandItem().getItem().useOn(new UseOnContext(recipient, InteractionHand.MAIN_HAND, hit(platePos)));
        HodgepodgeFeastBlockEntity plate = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(platePos);
        var savedPlate = plate.getUpdateTag(helper.getLevel().registryAccess());
        plate.clearLockedRecipe();
        plate.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(), savedPlate));
        helper.assertValueEqual(plate.lockedRecipe(), original, "locked recipe author after reload");
        recipient.getMainHandItem().getItem().useOn(new UseOnContext(recipient, InteractionHand.MAIN_HAND, hit(platePos)));
        helper.assertTrue(!plate.isRecipeLocked(), "Author profile broke recipe equality when unlocking");
        helper.succeed();
    }

    private static ItemStack record(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "OriginalChef",
                new PropertyMap(ImmutableMultimap.of("textures", TEXTURES)));
        Player author = new Player(helper.getLevel(), profile) {
            @Override
            public GameType gameMode() {
                return GameType.SURVIVAL;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
        BlockPos pos = helper.absolutePos(TARGET);
        helper.getLevel().setBlockAndUpdate(pos, KHBlocks.PORCELAIN_PLATE.defaultBlockState());
        HodgepodgeFeastBlockEntity feast = (HodgepodgeFeastBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(feast.add(PackingIngredients.RED_BERRY, 8, 8).success(), "Could not prepare recipe dish");
        ItemStack empty = new ItemStack(ModItems.RECIPE_ITEM);
        author.setItemInHand(InteractionHand.MAIN_HAND, empty);
        empty.getItem().useOn(new UseOnContext(author, InteractionHand.MAIN_HAND, hit(pos)));
        ItemStack recipe = author.getMainHandItem().copy();
        helper.assertTrue(recipe.is(KHItems.HODGEPODGE_RECIPE), "Recipe recording failed");
        return recipe;
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
