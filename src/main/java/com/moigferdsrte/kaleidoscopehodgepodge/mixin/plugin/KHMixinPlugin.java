package com.moigferdsrte.kaleidoscopehodgepodge.mixin.plugin;

import com.moigferdsrte.kaleidoscopehodgepodge.compat.Compat;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

public class KHMixinPlugin implements IMixinConfigPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("com.moigferdsrte.kaleidoscopehodgepodge.compat." + Compat.KCH))
            return FabricLoader.getInstance().isModLoaded(Compat.KCH);
        return true;
    }
}
