package com.moigferdsrte.kaleidoscopehodgepodge.mixin.client;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.api.PlateTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {
    @Unique
    private static final Function<String, ModelResourceLocation> CACHE_MODEL = Util.memoize((shape) -> {
        if (shape.isBlank()) return ModelResourceLocation.inventory(KaleidoscopeHodgepodge.id("porcelain_plate_empty"));
        return ModelResourceLocation.inventory(KaleidoscopeHodgepodge.id(shape.contains("wooden") ? "wooden_plate_empty" : shape + "_porcelain_plate_empty"));
    });

    @Shadow
    protected abstract void loadSpecialItemModelAndDependencies(ModelResourceLocation var);

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/ModelBakery;loadSpecialItemModelAndDependencies(Lnet/minecraft/client/resources/model/ModelResourceLocation;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 1
            )
    )
    private void loadSpecialItemModelAndDependencies(BlockColors blockColors, ProfilerFiller profilerFiller, Map<ResourceLocation, BlockModel> modelResources, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> blockStateResources, CallbackInfo ci) {
        for (var i : PlateTypes.values())
            this.loadSpecialItemModelAndDependencies(CACHE_MODEL.apply(i.getDesc()));
        this.loadSpecialItemModelAndDependencies(ModelResourceLocation.inventory(KaleidoscopeHodgepodge.id("porcelain_soup_bowl_empty_with_soup")));
        this.loadSpecialItemModelAndDependencies(ModelResourceLocation.inventory(KaleidoscopeHodgepodge.id("porcelain_soup_bowl_empty_without_soup")));
    }
}
