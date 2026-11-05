package com.eeeeeeruki.claudeupdater.core;

import java.nio.file.Path;

/**
 * Everything the core needs to know about the running game, filled in by the loader-specific entry point.
 *
 * @param gameDir   the instance folder (.minecraft or the server folder)
 * @param modsDir   the mods folder that jars are replaced in
 * @param mcVersion the Minecraft version this jar was built for, matched against "mc" in index.json
 * @param loader    "forge" or "neoforge", matched against "loader" in index.json
 * @param selfJar   this updater's own jar, copied out to run the replace helper after the game exits
 */
public record Env(Path gameDir, Path modsDir, String mcVersion, String loader, Path selfJar) {

    public Path workDir() {
        return gameDir.resolve("claudeupdater");
    }

    public Path stagingDir() {
        return workDir().resolve("staging");
    }

    public Path pendingFile() {
        return workDir().resolve("pending.txt");
    }

    public Path configFile() {
        return gameDir.resolve("config").resolve("claudeupdater.json");
    }
}
