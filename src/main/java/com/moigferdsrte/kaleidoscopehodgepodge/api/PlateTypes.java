package com.moigferdsrte.kaleidoscopehodgepodge.api;

import org.jetbrains.annotations.Nullable;

public enum PlateTypes {
    NORMAL(""),
    WOODEN("wooden"),
    MEDIAN("median"),
    LARGE("large");

    private final String desc;

    PlateTypes(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
