package com.moigferdsrte.kaleidoscopehodgepodge.client;

import com.moigferdsrte.kaleidoscopehodgepodge.client.render.item.CustomFeastItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.Objects;
import java.util.function.Supplier;

public final class ClientItemExtensions {
    public static IClientItemExtensions renderer(Supplier<? extends CustomFeastItemRenderer> factory) {
        return new DynamicRendererExtension(factory);
    }

    private static final class DynamicRendererExtension implements IClientItemExtensions {
        private final Supplier<? extends CustomFeastItemRenderer> factory;
        private volatile BlockEntityWithoutLevelRenderer renderer;

        private DynamicRendererExtension(Supplier<? extends CustomFeastItemRenderer> factory) {
            this.factory = Objects.requireNonNull(factory);
        }

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            BlockEntityWithoutLevelRenderer current = renderer;
            if (current != null) return current;
            synchronized (this) {
                if (renderer == null) renderer = factory.get();
                return renderer;
            }
        }
    }

    private ClientItemExtensions() {}
}
