package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeRecipeBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class KHBlockEntities {
    public static final BlockEntityType<HodgepodgeRecipeBlockEntity> RECIPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            KaleidoscopeHodgepodge.id("hodgepodge_recipe"), FabricBlockEntityTypeBuilder.create(HodgepodgeRecipeBlockEntity::new, KHBlocks.HODGEPODGE_RECIPE).build());
    public static final BlockEntityType<HodgepodgeFeastBlockEntity> FEAST = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            KaleidoscopeHodgepodge.id("feast"),
            FabricBlockEntityTypeBuilder.create(HodgepodgeFeastBlockEntity::new,
                    KHBlocks.WOODEN_PLATE, KHBlocks.BAMBOO_DISPLAY_TRAY, KHBlocks.PORCELAIN_PLATE, KHBlocks.PORCELAIN_SOUP_BOWL,
                    KHBlocks.MEDIAN_PORCELAIN_PLATE, KHBlocks.LARGE_PORCELAIN_PLATE).build());

    public static void init() {}

    private KHBlockEntities() {}
}
