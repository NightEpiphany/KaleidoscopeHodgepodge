package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgePlateBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public final class KHBlocks {
    public static final Block WOODEN_PLATE = commonReg("wooden_plate", HodgepodgePlateBlock::new, BlockBehaviour.Properties.of().instabreak().ignitedByLava().noOcclusion().sound(SoundType.WOOD));
    public static final Block PORCELAIN_PLATE = commonReg("porcelain_plate", HodgepodgePlateBlock::new, BlockBehaviour.Properties.of().instabreak().noOcclusion().sound(SoundType.DECORATED_POT));
    public static Block register(ResourceKey<Block> resourceKey, Function<BlockBehaviour.Properties, Block> function, BlockBehaviour.Properties properties) {
        Block block = function.apply(properties.setId(resourceKey));
        return Registry.register(BuiltInRegistries.BLOCK, resourceKey, block);
    }

    private static Block commonReg(String string, Function<BlockBehaviour.Properties, Block> function, BlockBehaviour.Properties properties) {
        return register(ResourceKey.create(Registries.BLOCK, KaleidoscopeHodgepodge.id(string)), function, properties);
    }

    public static void init() {

    }
}
