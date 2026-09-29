package com.moigferdsrte.kaleidoscopehodgepodge.api;

import net.neoforged.api.distmarker.Dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface Service {
    UsedFor usedFor();

    Dist env() default Dist.DEDICATED_SERVER;

    enum UsedFor {
        BLOCK,
        ITEM,
        ENTITY,
        BLOCK_ENTITY
    }
}
