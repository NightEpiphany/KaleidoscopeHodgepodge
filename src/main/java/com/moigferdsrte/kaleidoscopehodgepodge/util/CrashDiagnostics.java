package com.moigferdsrte.kaleidoscopehodgepodge.util;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.config.ConfigManager;

import java.util.concurrent.atomic.AtomicReference;

public final class CrashDiagnostics {
    private static final AtomicReference<String> LAST_EVENT = new AtomicReference<>("none");
    private static volatile boolean installed;

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            KaleidoscopeHodgepodge.LOGGER.error(
                    "Uncaught exception diagnostics: thread={}, lastHodgepodgeEvent={}, config={}",
                    thread.getName(), LAST_EVENT.get(), ConfigManager.describe(), throwable);
            if (previous != null) previous.uncaughtException(thread, throwable);
        });
    }

    public static void record(String event) {
        LAST_EVENT.set(event);
    }

    private CrashDiagnostics() {}
}
