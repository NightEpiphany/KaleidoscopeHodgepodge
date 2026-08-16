package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgePlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgeSoupBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.LargePorcelainPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.block.MedianPorcelainPlateBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public final class KHBlocks {

    public static final Block WOODEN_PLATE = commonReg("wooden_plate", HodgepodgePlateBlock::new, BlockBehaviour.Properties.of().instabreak().ignitedByLava().noOcclusion().sound(SoundType.WOOD));

    public static final Block PORCELAIN_PLATE = commonReg("porcelain_plate", HodgepodgePlateBlock::new, BlockBehaviour.Properties.of().instabreak().noOcclusion().sound(SoundType.DECORATED_POT));

    public static final Block MEDIAN_PORCELAIN_PLATE = commonReg("median_porcelain_plate",
            MedianPorcelainPlateBlock::new,
            BlockBehaviour.Properties.of().instabreak().noOcclusion().sound(SoundType.DECORATED_POT));

    public static final Block LARGE_PORCELAIN_PLATE = commonReg("large_porcelain_plate",
            LargePorcelainPlateBlock::new,
            BlockBehaviour.Properties.of().instabreak().noOcclusion().sound(SoundType.DECORATED_POT));

    public static final Block PORCELAIN_SOUP_BOWL = commonReg("porcelain_soup_bowl", HodgepodgeSoupBlock::new, BlockBehaviour.Properties.of().instabreak().noOcclusion().sound(SoundType.DECORATED_POT));

    public static Block register(ResourceLocation id, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    private static Block commonReg(String string, Function<BlockBehaviour.Properties, Block> function, BlockBehaviour.Properties properties) {
        return register(KaleidoscopeHodgepodge.id(string), function.apply(properties));
    }

    public static void init() {

    }
}
