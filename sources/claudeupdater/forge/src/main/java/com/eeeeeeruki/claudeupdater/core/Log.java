package com.eeeeeeruki.claudeupdater.core;

import java.util.function.Consumer;

/** Tiny logging bridge so the core does not depend on the loader's logger. */
public final class Log {
    private Log() {
    }

    public static volatile Consumer<String> info = s -> System.out.println("[ClaudeUpdater] " + s);
    public static volatile Consumer<String> warn = s -> System.err.println("[ClaudeUpdater] " + s);

    public static void info(String s) {
        info.accept(s);
    }

    public static void warn(String s) {
        warn.accept(s);
    }
}
