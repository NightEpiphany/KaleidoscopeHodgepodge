package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeFeastBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.HodgepodgeRecipeBlockEntity;
import com.moigferdsrte.kaleidoscopehodgepodge.blockentity.TeaTrayBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KHBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KaleidoscopeHodgepodge.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeaTrayBlockEntity>> TEA_TRAY =
            BLOCK_ENTITIES.register("tea_tray", () -> BlockEntityType.Builder.of(
                    TeaTrayBlockEntity::new, KHBlocks.TEA_TRAY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HodgepodgeRecipeBlockEntity>> RECIPE =
            BLOCK_ENTITIES.register("hodgepodge_recipe", () -> BlockEntityType.Builder.of(
                    HodgepodgeRecipeBlockEntity::new, KHBlocks.HODGEPODGE_RECIPE.get()).build(null));

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
