package com.eeeeeeruki.claudeupdater.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Which launcher started the game, and finding missing required mods on Modrinth so they can be
 * downloaded right away (CurseForge needs an API key, so it only gets a search page).
 */
public final class ModSites {
    private ModSites() {
    }

    public enum Launcher {
        CURSEFORGE("CurseForge", "CurseForge アプリ"),
        MODRINTH("Modrinth", "Modrinth App"),
        OTHER(null, null);

        /** The site name, null when the launcher is not one of the two (or not certain). */
        public final String site;
        public final String app;

        Launcher(String site, String app) {
            this.site = site;
            this.app = app;
        }
    }

    private static Launcher launcher;

    /**
     * Only answers CURSEFORGE or MODRINTH when it is sure: the CurseForge app keeps minecraftinstance.json in
     * every instance folder, and the Modrinth App names itself in minecraft.launcher.brand and keeps its
     * instances in a "profiles" folder. Anything else (Prism, the official launcher, unknown) is OTHER.
     */
    public static synchronized Launcher launcher(Path gameDir) {
        if (launcher != null) return launcher;
        launcher = detect(gameDir);
        Log.info("ランチャー: " + launcher + "（minecraft.launcher.brand=" + System.getProperty("minecraft.launcher.brand") + "）");
        return launcher;
    }

    private static Launcher detect(Path gameDir) {
        try {
            Path dir = gameDir.toAbsolutePath().normalize();
            Path parent = dir.getParent();
            // Prism / MultiMC: instance.cfg next to the game folder
            if (parent != null && Files.isRegularFile(parent.resolve("instance.cfg"))) return Launcher.OTHER;
            if (Files.isRegularFile(dir.resolve("minecraftinstance.json"))) return Launcher.CURSEFORGE;

            String brand = System.getProperty("minecraft.launcher.brand", "").toLowerCase(Locale.ROOT);
            boolean modrinthBrand = brand.contains("modrinth") || brand.contains("theseus");
            boolean inProfiles = parent != null && parent.getFileName() != null
                    && parent.getFileName().toString().equals("profiles");
            Path app = inProfiles ? parent.getParent() : null;
            boolean modrinthFolder = app != null && (Files.isRegularFile(app.resolve("app.db"))
                    || app.getFileName() != null && app.getFileName().toString().toLowerCase(Locale.ROOT).contains("modrinth"));
            if (modrinthBrand && inProfiles || modrinthFolder) return Launcher.MODRINTH;
        } catch (Exception ignored) {
        }
        return Launcher.OTHER;
    }

    /** A mod found on Modrinth with a file for this Minecraft version and loader. */
    public record Hit(String slug, String title, String summary, ModIndex.Release release) {
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Looks a required mod up on Modrinth (official public API, no key needed) and picks its newest file for
     * this Minecraft version and loader (a release when there is one). Network: call it off the render thread.
     *
     * @return null when Modrinth has no such mod or no file for this version
     */
    public static Hit modrinth(String id, String name, String mc, String loader) throws Exception {
        JsonObject project = modrinthProject(id, name, mc, loader);
        if (project == null) return null;
        String slug = project.get("slug").getAsString();
        String lf = "[\"" + loader.toLowerCase(Locale.ROOT) + "\"]";
        String gv = "[\"" + mc + "\"]";
        int[] st = new int[1];
        String body = get("https://api.modrinth.com/v2/project/" + URLEncoder.encode(slug, StandardCharsets.UTF_8)
                + "/version?loaders=" + URLEncoder.encode(lf, StandardCharsets.UTF_8)
                + "&game_versions=" + URLEncoder.encode(gv, StandardCharsets.UTF_8), st);
        if (st[0] != 200) throw new java.io.IOException("HTTP " + st[0]);
        JsonArray versions = JsonParser.parseString(body).getAsJsonArray();
        JsonObject pick = null;
        for (JsonElement v : versions) { // newest first
            JsonObject o = v.getAsJsonObject();
            if (pick == null) pick = o;
            if ("release".equals(str(o, "version_type"))) {
                pick = o;
                break;
            }
        }
        if (pick == null) return null;
        JsonObject file = null;
        for (JsonElement f : pick.getAsJsonArray("files")) {
            JsonObject fo = f.getAsJsonObject();
            if (file == null || fo.has("primary") && fo.get("primary").getAsBoolean()) file = fo;
            if (fo.has("primary") && fo.get("primary").getAsBoolean()) break;
        }
        if (file == null || !file.has("hashes")) return null;
        ModIndex.Release r = new ModIndex.Release();
        r.version = str(pick, "version_number");
        r.mc = mc;
        r.loader = loader;
        r.url = str(file, "url");
        r.fileName = str(file, "filename");
        r.sha512 = str(file.getAsJsonObject("hashes"), "sha512");
        r.changelog = null;
        r.released = str(pick, "date_published");
        if (r.released != null && r.released.length() >= 10) r.released = r.released.substring(0, 10);
        if (r.url == null || !r.url.startsWith("https://") || r.fileName == null || r.sha512 == null) return null;
        String title = str(project, "title");
        return new Hit(slug, title != null ? title : name, str(project, "description"), r);
    }

    private static String str(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    public static String loaderName(String loader) {
        switch (loader.toLowerCase(Locale.ROOT)) {
            case "neoforge": return "NeoForge";
            case "forge": return "Forge";
            case "fabric": return "Fabric";
            default: return loader;
        }
    }

    private static String get(String url, int[] status) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "EeeeeeruKi/ClaudeUpdater (github.com/EeeeeeruKi/EC-mod)")
                .GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        status[0] = res.statusCode();
        return res.body();
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    // ---- Modrinth (official public API, no key needed) ------------------------------------------

    /** The Modrinth search hit for a mod with this ID or name that has a file for this version and loader. */
    private static JsonObject modrinthProject(String id, String name, String mc, String loader) throws Exception {
        String facets = "[[\"categories:" + loader.toLowerCase(Locale.ROOT) + "\"],[\"versions:" + mc + "\"],[\"project_type:mod\"]]";
        Set<String> queries = new LinkedHashSet<>();
        queries.add(name);
        queries.add(id);
        for (String q : queries) {
            int[] st = new int[1];
            String body = get("https://api.modrinth.com/v2/search?limit=10&query=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                    + "&facets=" + URLEncoder.encode(facets, StandardCharsets.UTF_8), st);
            if (st[0] != 200) throw new java.io.IOException("HTTP " + st[0]);
            JsonArray hits = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("hits");
            for (JsonElement h : hits) {
                JsonObject o = h.getAsJsonObject();
                String slug = o.has("slug") ? o.get("slug").getAsString() : null;
                String title = o.has("title") ? o.get("title").getAsString() : null;
                if (slug == null) continue;
                if (norm(slug).equals(norm(id)) || norm(title).equals(norm(name)) || norm(title).equals(norm(id))) return o;
            }
        }
        return null;
    }

    // ---- CurseForge (its API needs a key, so only a search page is offered) ----------------------------

    public static String curseSearchUrl(String name, String mc) {
        return "https://www.curseforge.com/minecraft/search?class=mc-mods&gameVersion=" + mc + "&search="
                + URLEncoder.encode(name, StandardCharsets.UTF_8);
    }
}
