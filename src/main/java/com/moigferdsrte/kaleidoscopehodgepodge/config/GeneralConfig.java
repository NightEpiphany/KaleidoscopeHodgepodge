package com.moigferdsrte.kaleidoscopehodgepodge.config;

import java.util.concurrent.atomic.AtomicReference;

public final class GeneralConfig {
    public record Snapshot(int dishCapacity, int soupCapacity, int dishBaseHeight,
                           int soupBaseHeight, int maxModelHeight, boolean debugLogging) {}

    private static final Snapshot DEFAULT = new Snapshot(12, 9, 2, 4, 16, false);
    private static final AtomicReference<Snapshot> CURRENT = new AtomicReference<>(DEFAULT);

    public static Snapshot snapshot() { return CURRENT.get(); }
    public static void replace(Snapshot snapshot) { CURRENT.set(snapshot); }
    public static void reset() { CURRENT.set(DEFAULT); }

    private GeneralConfig() {}
}
