package com.eeeeeeruki.claudeupdater.core;

import java.nio.file.Path;

/** A mod that is loaded right now from its own jar directly inside the mods folder. */
public record InstalledMod(String id, String name, String version, Path jar) {
}
