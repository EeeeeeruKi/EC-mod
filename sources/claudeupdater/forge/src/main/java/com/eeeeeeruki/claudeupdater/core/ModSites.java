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
 * Which launcher started the game, and whether a mod can be found on that launcher's own site
 * (CurseForge or Modrinth), so the dependency screen can say where to get a missing mod.
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

    /** What was found for one mod: a sentence to show and the page "開く" should open. */
    public record Result(String sentence, String url) {
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Looks the mod up on the launcher's site. Runs network requests: call it off the render thread. */
    public static Result lookup(Launcher l, String id, String name, String mc, String loader) {
        String ver = mc + " " + loaderName(loader);
        try {
            Boolean found;
            String page;
            if (l == Launcher.MODRINTH) {
                String slug = modrinthSlug(id, name, mc, loader);
                found = slug != null;
                page = slug != null ? "https://modrinth.com/mod/" + slug : Deps.searchUrl(id, mc, loader);
            } else {
                Object[] cf = curseForge(id, name, mc, loader); // {Boolean found-with-version or null, url}
                found = (Boolean) cf[0];
                page = (String) cf[1];
                if (found == null) {
                    return new Result("§7" + l.site + " にあるかは確かめられませんでした。「開く」で " + l.site + " の検索ページを開けます。", page);
                }
            }
            if (found) {
                return new Result("§a" + l.site + " に " + ver + " 版があります。" + l.app + " で「" + name + "」を検索すると入れられます。", page);
            }
            return new Result("§6" + l.site + " では " + ver + " 版を見つけられませんでした（名前が違うだけかもしれません）。", page);
        } catch (Exception e) {
            Log.warn(l.site + " で " + id + " を探せませんでした: " + e);
            String page = l == Launcher.MODRINTH ? Deps.searchUrl(id, mc, loader) : curseSearchUrl(name, mc);
            return new Result("§7" + l.site + " に接続できなかったため、あるか確かめられませんでした。", page);
        }
    }

    private static String loaderName(String loader) {
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

    /** The slug of a Modrinth mod with this ID or name that has a file for this version and loader. */
    private static String modrinthSlug(String id, String name, String mc, String loader) throws Exception {
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
                if (norm(slug).equals(norm(id)) || norm(title).equals(norm(name)) || norm(title).equals(norm(id))) return slug;
            }
        }
        return null;
    }

    // ---- CurseForge (the official API needs a key, so the public CFWidget mirror is used) ------------

    private static String curseSearchUrl(String name, String mc) {
        return "https://www.curseforge.com/minecraft/search?class=mc-mods&gameVersion=" + mc + "&search="
                + URLEncoder.encode(name, StandardCharsets.UTF_8);
    }

    /** @return {TRUE/FALSE when known, null when it could not be checked; the page to open} */
    private static Object[] curseForge(String id, String name, String mc, String loader) throws Exception {
        Set<String> slugs = new LinkedHashSet<>();
        slugs.add(id.toLowerCase(Locale.ROOT).replace('_', '-'));
        slugs.add(name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""));
        boolean unsure = false;
        for (String slug : slugs) {
            if (slug.isEmpty()) continue;
            int[] st = new int[1];
            String body = get("https://api.cfwidget.com/minecraft/mc-mods/" + slug, st);
            if (st[0] == 404) continue;
            if (st[0] != 200) { // 202 = not looked up yet, try again later
                unsure = true;
                continue;
            }
            JsonObject o = JsonParser.parseString(body).getAsJsonObject();
            String page = "https://www.curseforge.com/minecraft/mc-mods/" + slug;
            if (o.has("urls") && o.getAsJsonObject("urls").has("curseforge")) {
                page = o.getAsJsonObject("urls").get("curseforge").getAsString();
            }
            boolean match = false;
            if (o.has("files") && o.get("files").isJsonArray()) {
                for (JsonElement f : o.getAsJsonArray("files")) {
                    JsonObject fo = f.getAsJsonObject();
                    if (!fo.has("versions") || !fo.get("versions").isJsonArray()) continue;
                    boolean v = false;
                    boolean ld = false;
                    for (JsonElement x : fo.getAsJsonArray("versions")) {
                        String s = x.getAsString();
                        if (s.equals(mc)) v = true;
                        if (s.equalsIgnoreCase(loader)) ld = true;
                    }
                    if (v && ld) {
                        match = true;
                        break;
                    }
                }
            }
            return new Object[]{match, page};
        }
        return new Object[]{unsure ? null : Boolean.FALSE, curseSearchUrl(name, mc)};
    }
}
