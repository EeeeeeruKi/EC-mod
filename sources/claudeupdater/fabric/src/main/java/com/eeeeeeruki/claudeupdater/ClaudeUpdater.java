package com.eeeeeeruki.claudeupdater;

import com.eeeeeeruki.claudeupdater.core.Env;
import com.eeeeeeruki.claudeupdater.core.InstalledMod;
import com.eeeeeeruki.claudeupdater.core.Log;
import com.eeeeeeruki.claudeupdater.core.UpdaterCore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Main entry point (both sides): starts the update check. Client screens and keys are in ClaudeUpdaterClient. */
public class ClaudeUpdater implements ModInitializer {
    public static final String MODID = "claudeupdater";
    public static final String MC_VERSION = "1.21.1";
    public static final String LOADER = "fabric";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        Log.info = LOGGER::info;
        Log.warn = LOGGER::warn;

        FabricLoader fl = FabricLoader.getInstance();
        Path game = fl.getGameDir().toAbsolutePath().normalize();
        Path mods = game.resolve("mods").normalize();
        Path self = fl.getModContainer(MODID)
                .flatMap(c -> c.getOrigin().getKind() == net.fabricmc.loader.api.metadata.ModOrigin.Kind.PATH
                        ? c.getOrigin().getPaths().stream().findFirst() : java.util.Optional.empty())
                .orElse(null);
        Env env = new Env(game, mods, MC_VERSION, LOADER, self);
        Set<String> loaded = new HashSet<>();
        for (ModContainer c : fl.getAllMods()) loaded.add(c.getMetadata().getId());
        UpdaterCore.start(env, installedMods(fl, mods), loaded, fl.getEnvironmentType() == EnvType.SERVER);
    }

    private static List<InstalledMod> installedMods(FabricLoader fl, Path mods) {
        List<InstalledMod> out = new ArrayList<>();
        for (ModContainer c : fl.getAllMods()) {
            try {
                if (c.getContainingMod().isPresent()) continue; // jar-in-jar library
                if (c.getOrigin().getKind() != net.fabricmc.loader.api.metadata.ModOrigin.Kind.PATH) continue;
                for (Path p : c.getOrigin().getPaths()) {
                    Path jar = p.toAbsolutePath().normalize();
                    // only jars sitting directly in the mods folder
                    if (!Files.isRegularFile(jar) || !mods.equals(jar.getParent())) continue;
                    out.add(new InstalledMod(c.getMetadata().getId(), c.getMetadata().getName(),
                            c.getMetadata().getVersion().getFriendlyString(), jar));
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }
}
