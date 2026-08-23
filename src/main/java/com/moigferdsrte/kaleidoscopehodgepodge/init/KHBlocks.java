package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.block.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KHBlocks {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(KaleidoscopeHodgepodge.MOD_ID);

    public static final DeferredBlock<HodgepodgePlateBlock> WOODEN_PLATE = BLOCKS.registerBlock(
            "wooden_plate", HodgepodgePlateBlock::new,
            BlockBehaviour.Properties.of().instabreak().ignitedByLava().noOcclusion().sound(SoundType.WOOD));

    public static final DeferredBlock<HodgepodgeDisplayTrayBlock> BAMBOO_DISPLAY_TRAY = BLOCKS.registerBlock(
            "bamboo_display_tray", HodgepodgeDisplayTrayBlock::new,
            BlockBehaviour.Properties.of().instabreak().ignitedByLava().noOcclusion().sound(SoundType.BAMBOO));

    public static final DeferredBlock<HodgepodgePlateBlock> PORCELAIN_PLATE = BLOCKS.registerBlock(
            "porcelain_plate", HodgepodgePlateBlock::new,
            BlockBehaviour.Properties.of().instabreak().noOcclusion().sound(SoundType.DECORATED_POT));

    public static final DeferredBlock<MedianPorcelainPlateBlock> MEDIAN_PORCELAIN_PLATE = BLOCKS.registerBlock(
            "median_porcelain_plate", MedianPorcelainPlateBlock::new,
            BlockBehaviour.Properties.of().instabreak().noOcclusion().sound(SoundType.DECORATED_POT));

    public static final DeferredBlock<LargePorcelainPlateBlock> LARGE_PORCELAIN_PLATE = BLOCKS.registerBlock(
            "large_porcelain_plate", LargePorcelainPlateBlock::new,
            BlockBehaviour.Properties.of().instabreak().noOcclusion().sound(SoundType.DECORATED_POT));

    public static final DeferredBlock<HodgepodgeSoupBlock> PORCELAIN_SOUP_BOWL = BLOCKS.registerBlock(
            "porcelain_soup_bowl", HodgepodgeSoupBlock::new,
            BlockBehaviour.Properties.of().instabreak().noOcclusion().sound(SoundType.DECORATED_POT));

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private KHBlocks() {}
}
