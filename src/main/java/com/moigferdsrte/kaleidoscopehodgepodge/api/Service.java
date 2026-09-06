package com.moigferdsrte.kaleidoscopehodgepodge.api;

import net.fabricmc.api.EnvType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface Service {
    UsedFor usedFor();

    EnvType env() default EnvType.SERVER;

    enum UsedFor {
        BLOCK,
        ITEM,
        ENTITY,
        BLOCK_ENTITY
    }
}
