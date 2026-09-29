package com.moigferdsrte.kaleidoscopehodgepodge.mixin.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves the newer assets' single-axis rotations without changing vanilla model parsing. */
@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockElement$Deserializer")
public abstract class BlockElementDeserializerMixin {
    @Inject(method = "getRotation", at = @At("HEAD"), cancellable = true)
    private void hodgepodge$extendedRotation(JsonObject element,
                                             CallbackInfoReturnable<BlockElementRotation> callback) {
        if (!element.has("kaleidoscope_hodgepodge:rotation")) return;
        JsonObject rotation = GsonHelper.getAsJsonObject(element, "kaleidoscope_hodgepodge:rotation");
        var origin = GsonHelper.getAsJsonArray(rotation, "origin");
        if (origin.size() != 3) throw new JsonParseException("Rotation origin must contain three numbers");
        Vector3f pivot = new Vector3f(origin.get(0).getAsFloat(), origin.get(1).getAsFloat(),
                origin.get(2).getAsFloat()).div(16);
        Direction.Axis axis = Direction.Axis.Y;
        float angle = 0;
        for (Direction.Axis candidate : Direction.Axis.values()) {
            float value = GsonHelper.getAsFloat(rotation, candidate.getName(), 0);
            if (!Float.isFinite(value)) throw new JsonParseException("Non-finite rotation");
            if (value != 0) {
                if (angle != 0) throw new JsonParseException("Only single-axis extended rotations are supported");
                axis = candidate;
                angle = value;
            }
        }
        if (!pivot.isFinite()) throw new JsonParseException("Non-finite rotation origin");
        callback.setReturnValue(new BlockElementRotation(pivot, axis, angle, false));
    }
}
