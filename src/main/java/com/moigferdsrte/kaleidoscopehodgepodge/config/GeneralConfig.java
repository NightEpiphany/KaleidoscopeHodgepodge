package com.moigferdsrte.kaleidoscopehodgepodge.config;

import java.util.concurrent.atomic.AtomicReference;

public final class GeneralConfig {
    public record Snapshot(int woodenPlateCapacity, int porcelainCapacity, int soupCapacity,
                           int dishBaseHeight, int soupBaseHeight,
                           int woodenMaxModelHeight, int porcelainMaxModelHeight,
                           boolean debugLogging) {}

    private static final Snapshot DEFAULT = new Snapshot(20, 40, 40, 2, 4, 16, 32, false);
    private static final AtomicReference<Snapshot> CURRENT = new AtomicReference<>(DEFAULT);

    public static Snapshot snapshot() { return CURRENT.get(); }
    public static void replace(Snapshot snapshot) { CURRENT.set(snapshot); }
    public static void reset() { CURRENT.set(DEFAULT); }

    private GeneralConfig() {}
}
