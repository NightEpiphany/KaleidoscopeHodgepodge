package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KHBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KaleidoscopeHodgepodge.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HodgepodgeFeastBlockEntity>> FEAST =
            BLOCK_ENTITIES.register("feast", () -> BlockEntityType.Builder.of(HodgepodgeFeastBlockEntity::new,
                    KHBlocks.WOODEN_PLATE.get(), KHBlocks.PORCELAIN_PLATE.get(), KHBlocks.BAMBOO_DISPLAY_TRAY.get(),
                    KHBlocks.PORCELAIN_SOUP_BOWL.get(), KHBlocks.MEDIAN_PORCELAIN_PLATE.get(),
                    KHBlocks.LARGE_PORCELAIN_PLATE.get()).build(null));

    public static void init(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }

    private KHBlockEntities() {}
}
