package com.eeeeeeruki.claudeupdater.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads which other mods a jar needs, straight from its own metadata (mods.toml, neoforge.mods.toml or
 * fabric.mod.json), so the index does not have to list dependencies.
 */
public final class Deps {
    private Deps() {
    }

    /** The game, the loader and Java: always there, never offered. */
    private static final Set<String> PLATFORM = Set.of(
            "minecraft", "forge", "neoforge", "fml", "javafml", "lowcodefml",
            "fabricloader", "fabric-loader", "java", "quilt_loader");

    /** What one jar says about itself. */
    public static final class Info {
        /** Mod IDs this jar (and the jars bundled inside it) provides. */
        public final Set<String> provides = new LinkedHashSet<>();
        /** Mod IDs it requires, minus the ones it provides itself. */
        public final Set<String> requires = new LinkedHashSet<>();
    }

    /**
     * @param client true on the game client: dependencies only needed on the server side are left out
     */
    public static Info read(Path jar, boolean client) {
        Info info = new Info();
        try (InputStream in = Files.newInputStream(jar)) {
            readZip(in, info, client, 0);
        } catch (Exception e) {
            Log.warn("必要なMODを確認できませんでした: " + jar.getFileName() + " " + e);
        }
        info.requires.removeAll(info.provides);
        info.requires.removeAll(PLATFORM);
        return info;
    }

    private static void readZip(InputStream raw, Info info, boolean client, int depth) throws IOException {
        ZipInputStream zip = new ZipInputStream(raw);
        ZipEntry e;
        while ((e = zip.getNextEntry()) != null) {
            String name = e.getName();
            if (name.equals("META-INF/mods.toml") || name.equals("META-INF/neoforge.mods.toml")) {
                readToml(new String(zip.readAllBytes(), StandardCharsets.UTF_8), info, client, depth == 0);
            } else if (name.equals("fabric.mod.json")) {
                readFabric(new String(zip.readAllBytes(), StandardCharsets.UTF_8), info, depth == 0);
            } else if (depth == 0 && name.startsWith("META-INF/") && name.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                // jar-in-jar libraries (META-INF/jarjar, META-INF/jars): what they provide counts as provided
                Info nested = new Info();
                readZip(new ByteArrayInputStream(zip.readAllBytes()), nested, client, depth + 1);
                info.provides.addAll(nested.provides);
            }
        }
    }

    // ---- Forge / NeoForge ------------------------------------------------------------------------

    static void readToml(String text, Info info, boolean client, boolean withDeps) {
        String section = "";
        Map<String, String> block = new java.util.HashMap<>();
        for (String rawLine : (text + "\n[[end]]").split("\\R")) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[")) {
                finishBlock(section, block, info, client, withDeps);
                section = line;
                block = new java.util.HashMap<>();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (value.length() >= 2 && (value.startsWith("\"") || value.startsWith("'"))) {
                char q = value.charAt(0);
                int end = value.indexOf(q, 1);
                value = end > 0 ? value.substring(1, end) : value.substring(1);
            }
            block.put(key, value);
        }
    }

    private static void finishBlock(String section, Map<String, String> b, Info info, boolean client, boolean withDeps) {
        String id = b.get("modId");
        if (id == null || id.isBlank()) return;
        if (section.equals("[[mods]]")) {
            info.provides.add(id);
            return;
        }
        if (!withDeps || !section.startsWith("[[dependencies.")) return;
        String type = b.get("type");      // NeoForge: required / optional / incompatible / discouraged
        String mandatory = b.get("mandatory"); // Forge 1.20.1: true / false
        boolean required;
        if (type != null) required = type.equalsIgnoreCase("required");
        else if (mandatory != null) required = mandatory.equalsIgnoreCase("true");
        else required = true; // NeoForge's default type
        String side = b.getOrDefault("side", "BOTH");
        if (client && side.equalsIgnoreCase("SERVER")) required = false;
        if (!client && side.equalsIgnoreCase("CLIENT")) required = false;
        if (required) info.requires.add(id);
    }

    private static String stripComment(String line) {
        boolean inQuote = false;
        char q = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == q) inQuote = false;
            } else if (c == '"' || c == '\'') {
                inQuote = true;
                q = c;
            } else if (c == '#') {
                return line.substring(0, i);
            }
        }
        return line;
    }

    // ---- Fabric ----------------------------------------------------------------------------------

    static void readFabric(String text, Info info, boolean withDeps) {
        JsonObject o;
        try {
            o = JsonParser.parseString(text).getAsJsonObject();
        } catch (Exception e) {
            return;
        }
        if (o.has("id")) info.provides.add(o.get("id").getAsString());
        if (o.has("provides") && o.get("provides").isJsonArray()) {
            for (JsonElement p : o.getAsJsonArray("provides")) info.provides.add(p.getAsString());
        }
        if (withDeps && o.has("depends") && o.get("depends").isJsonObject()) {
            info.requires.addAll(o.getAsJsonObject("depends").keySet());
        }
    }

    // ---- names and links for mods that are not in the library ---------------------------------------

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("fabric-api", "Fabric API"), Map.entry("fabric", "Fabric API"),
            Map.entry("curios", "Curios API"), Map.entry("hyperdaimc", "Hyperlink"), Map.entry("ponder", "Ponder"), Map.entry("create", "Create"),
            Map.entry("mekanism", "Mekanism"), Map.entry("projecte", "ProjectE"), Map.entry("jei", "JEI"),
            Map.entry("geckolib", "GeckoLib"), Map.entry("architectury", "Architectury API"),
            Map.entry("cloth_config", "Cloth Config API"), Map.entry("cloth-config", "Cloth Config API"),
            Map.entry("kotlinforforge", "Kotlin for Forge"), Map.entry("aether", "The Aether"),
            Map.entry("modmenu", "Mod Menu"), Map.entry("balm", "Balm"), Map.entry("cobalt", "Cobalt"),
            Map.entry("flywheel", "Flywheel"), Map.entry("terrablender", "TerraBlender"),
            Map.entry("citadel", "Citadel"), Map.entry("patchouli", "Patchouli"),
            Map.entry("cold_sweat", "Cold Sweat"), Map.entry("corpse", "Corpse"),
            Map.entry("cosmeticarmorreworked", "Cosmetic Armor Reworked"));

    public static String displayName(String id) {
        return NAMES.getOrDefault(id, id);
    }

    /** A Modrinth search for the mod, narrowed to this Minecraft version and loader. */
    public static String searchUrl(String id, String mcVersion, String loader) {
        String q = java.net.URLEncoder.encode(displayName(id), StandardCharsets.UTF_8);
        return "https://modrinth.com/mods?q=" + q + "&g=categories:" + loader.toLowerCase(Locale.ROOT) + "&v=" + mcVersion;
    }
}
