package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HodgepodgeRecipeDataTest {
    private static final UUID AUTHOR_ID = UUID.fromString("d2e107fc-a5e8-4f59-bab0-1f235eae4396");
    private static final Property TEXTURES = new Property("textures", "saved-texture-value", "saved-signature");
    private static final GameProfile AUTHOR = new GameProfile(AUTHOR_ID, "RecipeAuthor",
            new PropertyMap(ImmutableMultimap.of("textures", TEXTURES)));

    @Test
    void persistentCodecPreservesAuthorNameAndSignedTextures() {
        HodgepodgeRecipeData original = new HodgepodgeRecipeData("recipe-code", AUTHOR);
        var encoded = HodgepodgeRecipeData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        HodgepodgeRecipeData decoded = HodgepodgeRecipeData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(original, decoded);
        assertEquals(AUTHOR_ID, decoded.owner());
        assertEquals("RecipeAuthor", decoded.ownerProfile().orElseThrow().name());
        assertEquals(TEXTURES, decoded.ownerProfile().orElseThrow().properties().get("textures").iterator().next());
    }

    @Test
    void networkCodecPreservesAuthorNameAndSignedTextures() {
        HodgepodgeRecipeData original = new HodgepodgeRecipeData("recipe-code", AUTHOR);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            HodgepodgeRecipeData.STREAM_CODEC.encode(buffer, original);
            assertEquals(original, HodgepodgeRecipeData.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void legacyUuidOnlyRecipesRemainReadableAndNetworkSynchronized() {
        JsonObject legacy = new JsonObject();
        legacy.addProperty("feast_code", "legacy-recipe");
        legacy.addProperty("owner", AUTHOR_ID.toString());
        HodgepodgeRecipeData decoded = HodgepodgeRecipeData.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();
        assertEquals(new HodgepodgeRecipeData("legacy-recipe", AUTHOR_ID), decoded);
        assertEquals(Optional.empty(), decoded.ownerProfile());
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            HodgepodgeRecipeData.STREAM_CODEC.encode(buffer, decoded);
            assertEquals(decoded, HodgepodgeRecipeData.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void profilesWithoutTexturesStillPersistAuthorName() {
        HodgepodgeRecipeData original = new HodgepodgeRecipeData("recipe-code", new GameProfile(AUTHOR_ID, "OfflineAuthor"));
        var encoded = HodgepodgeRecipeData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        HodgepodgeRecipeData decoded = HodgepodgeRecipeData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals("OfflineAuthor", decoded.ownerProfile().orElseThrow().name());
        assertFalse(decoded.ownerProfile().orElseThrow().properties().containsKey("textures"));
    }

    @Test
    void namedRecipeCopiesStyledName() {
        var name = Component.literal("Special Dish").withStyle(ChatFormatting.GOLD);
        HodgepodgeRecipeData original = new HodgepodgeRecipeData("recipe-code", AUTHOR, Optional.of(name));
        name.append(" changed");
        assertEquals(Component.literal("Special Dish").withStyle(ChatFormatting.GOLD), original.dishName().orElseThrow());
    }

    @Test
    void missingAndBlankDishNamesRemainAbsent() {
        var legacy = HodgepodgeRecipeData.CODEC.encodeStart(JsonOps.INSTANCE,
                new HodgepodgeRecipeData("recipe-code", AUTHOR)).getOrThrow();
        assertFalse(legacy.getAsJsonObject().has("dish_name"));
        assertEquals(Optional.empty(), HodgepodgeRecipeData.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow().dishName());
        assertEquals(Optional.empty(), new HodgepodgeRecipeData("recipe-code", AUTHOR,
                Optional.of(Component.literal("  "))).dishName());
    }
}
