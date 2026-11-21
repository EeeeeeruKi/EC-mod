package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.client.ModSafetyScreen;
import com.eeeeeeruki.minecrafttweak.feature.client.ModSafetyFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * modの安全チェック（Modgrade API）。
 *
 * - 起動したら、mods フォルダの jar の SHA-1 を数え、前の起動のときの一覧（config/minecrafttweak/mod_safety.json）とくらべる。
 * - 初めて起動したとき・modが増えた／変わった／消えたとき・前の起動のあとにクラッシュの記録が増えていたときだけ、Modgrade に問い合わせる。
 * - 結果はタイトル画面で知らせる。マルウェアや注意が見つかったら、結果の画面を自動で開く。
 *
 * Modgrade API の決まり: 1回に100個まで・1分に60回まで、User-Agent にプログラム名と連絡先、
 * 結果を出すところには Modgrade のページへのリンクを出す。
 */
public final class ModSafety {
    private static final String API = "https://modgrade.com/api/v1/projects?sha1=";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum Status { MALWARE, WARNING, PARTLY, NOT_CHECKED, PASSED, UNKNOWN }

    /** 1つのファイルの結果。 */
    public record Entry(String fileName, String sha1, Status status, String title, String url, List<String> reasons) {
    }

    public enum State { IDLE, CHECKING, DONE, FAILED }

    private static volatile State state = State.IDLE;
    private static volatile List<Entry> results = List.of();
    private static volatile List<String> triggers = List.of();
    private static volatile String error = "";
    private static boolean shownScreen;
    private static boolean registered;

    private ModSafety() {
    }

    public static void register(IEventBus modBus) {
        if (registered) {
            return;
        }
        registered = true;
        modBus.addListener(ModSafety::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(ModSafety::onTitleInit);
        MinecraftForge.EVENT_BUS.addListener(ModSafety::onRender);
        MinecraftForge.EVENT_BUS.addListener(ModSafety::onClick);
    }

    public static State state() {
        return state;
    }

    public static List<Entry> results() {
        return results;
    }

    public static List<String> triggers() {
        return triggers;
    }

    public static String error() {
        return error;
    }

    private static boolean enabled() {
        return ModSafetyFeature.INSTANCE != null && ModSafetyFeature.INSTANCE.isEnabled();
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        if (!enabled()) {
            return;
        }
        Thread t = new Thread(() -> run(false), "minEcraft twEEk mod safety check");
        t.setDaemon(true);
        t.start();
    }

    /** 設定画面などから「今すぐ調べる」とき。 */
    public static void checkNow() {
        if (state == State.CHECKING) {
            return;
        }
        Thread t = new Thread(() -> run(true), "minEcraft twEEk mod safety check");
        t.setDaemon(true);
        t.start();
    }

    // ===== 起動時の判断 =====

    private static Path stateFile() {
        return FMLPaths.CONFIGDIR.get().resolve(MinecraftTweak.MODID).resolve("mod_safety.json");
    }

    private static void run(boolean force) {
        try {
            Path modsDir = FMLPaths.MODSDIR.get();
            Map<String, String> now = hashMods(modsDir);
            JsonObject saved = readState();
            List<String> why = new ArrayList<>();
            long lastRun = 0;
            if (force) {
                why.add("manual");
            }
            if (saved == null) {
                why.add("first_run");
            } else {
                lastRun = saved.has("lastRun") ? saved.get("lastRun").getAsLong() : 0;
                Map<String, String> before = new LinkedHashMap<>();
                if (saved.has("files")) {
                    for (Map.Entry<String, JsonElement> e : saved.getAsJsonObject("files").entrySet()) {
                        before.put(e.getKey(), e.getValue().getAsString());
                    }
                }
                boolean added = false;
                boolean updated = false;
                for (Map.Entry<String, String> e : now.entrySet()) {
                    String old = before.get(e.getKey());
                    if (old == null) {
                        added = true;
                    } else if (!old.equals(e.getValue())) {
                        updated = true;
                    }
                }
                boolean removed = before.keySet().stream().anyMatch(k -> !now.containsKey(k));
                if (added) {
                    why.add("added");
                }
                if (updated) {
                    why.add("updated");
                }
                if (removed) {
                    why.add("removed");
                }
                if (crashedSince(lastRun)) {
                    why.add("crash");
                }
            }
            writeState(now, System.currentTimeMillis());
            if (why.isEmpty()) {
                return; // 変わったことがないので調べない
            }
            triggers = List.copyOf(why);
            state = State.CHECKING;
            shownScreen = false;
            results = query(now);
            state = State.DONE;
            MinecraftTweak.LOGGER.info("minEcraft twEEk: mod safety check done ({} files, reason {})", now.size(), why);
        } catch (Throwable t) {
            error = String.valueOf(t.getMessage());
            state = State.FAILED;
            MinecraftTweak.LOGGER.warn("minEcraft twEEk: mod safety check failed", t);
        }
    }

    private static Map<String, String> hashMods(Path dir) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        if (!Files.isDirectory(dir)) {
            return map;
        }
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar")) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    files.add(p);
                }
            }
        }
        files.sort(null);
        for (Path p : files) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(p)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    md.update(buf, 0, n);
                }
            }
            map.put(p.getFileName().toString(), HexFormat.of().formatHex(md.digest()));
        }
        return map;
    }

    /** 前の起動のあとに、クラッシュの記録（crash-reports か hs_err_pid）が増えたか。 */
    private static boolean crashedSince(long lastRun) {
        if (lastRun <= 0) {
            return false;
        }
        Path game = FMLPaths.GAMEDIR.get();
        try {
            Path reports = game.resolve("crash-reports");
            if (Files.isDirectory(reports)) {
                try (DirectoryStream<Path> s = Files.newDirectoryStream(reports, "crash-*.txt")) {
                    for (Path p : s) {
                        if (Files.getLastModifiedTime(p).toMillis() > lastRun) {
                            return true;
                        }
                    }
                }
            }
            try (DirectoryStream<Path> s = Files.newDirectoryStream(game, "hs_err_pid*.log")) {
                for (Path p : s) {
                    if (Files.getLastModifiedTime(p).toMillis() > lastRun) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
            // 読めなければクラッシュは無かったことにする
        }
        return false;
    }

    private static JsonObject readState() {
        try {
            Path f = stateFile();
            if (!Files.isRegularFile(f)) {
                return null;
            }
            return JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeState(Map<String, String> files, long time) {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("lastRun", time);
            JsonObject f = new JsonObject();
            files.forEach(f::addProperty);
            o.add("files", f);
            Path path = stateFile();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception e) {
            MinecraftTweak.LOGGER.warn("minEcraft twEEk: could not save the mod safety state", e);
        }
    }

    // ===== Modgrade への問い合わせ =====

    private static List<Entry> query(Map<String, String> files) throws Exception {
        String version = ModList.get().getModContainerById(MinecraftTweak.MODID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("?");
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        List<Map.Entry<String, String>> list = new ArrayList<>(files.entrySet());
        Map<String, JsonObject> bySha = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i += 100) {
            List<Map.Entry<String, String>> part = list.subList(i, Math.min(list.size(), i + 100));
            StringBuilder q = new StringBuilder();
            for (Map.Entry<String, String> e : part) {
                if (q.length() > 0) {
                    q.append(',');
                }
                q.append(e.getValue());
            }
            HttpRequest req = HttpRequest.newBuilder(URI.create(API + q))
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "minEcraft-twEEk/" + version + " (Minecraft mod; https://github.com/EeeeeeruKi/EC-mod)")
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 429 || res.statusCode() == 503) {
                long wait = res.headers().firstValueAsLong("Retry-After").orElse(30);
                Thread.sleep(Math.min(120, Math.max(1, wait)) * 1000L);
                i -= 100; // もう一度
                continue;
            }
            if (res.statusCode() != 200) {
                throw new IllegalStateException("HTTP " + res.statusCode());
            }
            JsonObject body = JsonParser.parseString(res.body()).getAsJsonObject();
            JsonArray arr = body.getAsJsonArray("results");
            if (arr != null) {
                // query は「sha1:<ハッシュ>」の形で返ってくる。結果は頼んだ順に並ぶので、順番でも合わせる
                for (int k = 0; k < arr.size(); k++) {
                    if (!arr.get(k).isJsonObject()) {
                        continue;
                    }
                    JsonObject r = arr.get(k).getAsJsonObject();
                    String key = null;
                    if (r.has("query") && r.get("query").isJsonPrimitive()) {
                        key = stripPrefix(r.get("query").getAsString());
                    }
                    if ((key == null || key.length() != 40) && r.has("file") && r.get("file").isJsonObject()
                            && r.getAsJsonObject("file").has("sha1") && r.getAsJsonObject("file").get("sha1").isJsonPrimitive()) {
                        key = stripPrefix(r.getAsJsonObject("file").get("sha1").getAsString());
                    }
                    if ((key == null || key.length() != 40) && k < part.size()) {
                        key = part.get(k).getValue();
                    }
                    if (key != null) {
                        bySha.put(key, r);
                    }
                }
            }
            if (i + 100 < list.size()) {
                Thread.sleep(1100); // 1分に60回までなので、間をあける
            }
        }
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<String, String> e : list) {
            out.add(toEntry(e.getKey(), e.getValue(), bySha.get(e.getValue())));
        }
        out.sort((a, b) -> a.status().compareTo(b.status()) != 0
                ? a.status().compareTo(b.status()) : a.fileName().compareToIgnoreCase(b.fileName()));
        return List.copyOf(out);
    }

    private static String stripPrefix(String value) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        int colon = v.indexOf(':');
        return colon >= 0 ? v.substring(colon + 1) : v;
    }

    private static Status toStatus(String value) {
        return switch (value == null ? "" : value.trim().toLowerCase(Locale.ROOT)) {
            case "malware" -> Status.MALWARE;
            case "warning" -> Status.WARNING;
            case "partly_checked", "partly", "partial" -> Status.PARTLY;
            case "passed", "safe", "clean" -> Status.PASSED;
            default -> Status.NOT_CHECKED;
        };
    }

    private static Entry toEntry(String fileName, String sha1, JsonObject r) {
        if (r == null || !r.has("found") || !r.get("found").isJsonPrimitive() || !r.get("found").getAsBoolean()
                || !r.has("project") || !r.get("project").isJsonObject()) {
            return new Entry(fileName, sha1, Status.UNKNOWN, "", "", List.of());
        }
        JsonObject p = r.getAsJsonObject("project");
        String security = p.has("security") && p.get("security").isJsonPrimitive() ? p.get("security").getAsString() : "";
        boolean malwareHere = r.has("malware_on_this_platform") && !r.get("malware_on_this_platform").isJsonNull()
                && r.get("malware_on_this_platform").getAsBoolean();
        Status status = toStatus(security);
        // そのファイルそのものの結果（file.result）も見て、悪いほうにする
        if (r.has("file") && r.get("file").isJsonObject()) {
            JsonObject f = r.getAsJsonObject("file");
            if (f.has("result") && f.get("result").isJsonPrimitive()) {
                Status fileStatus = toStatus(f.get("result").getAsString());
                if (fileStatus.ordinal() < status.ordinal()) {
                    status = fileStatus;
                }
            }
        }
        if (malwareHere || (p.has("malware_confirmed") && p.get("malware_confirmed").isJsonPrimitive()
                && p.get("malware_confirmed").getAsBoolean())) {
            status = Status.MALWARE;
        }
        List<String> reasons = new ArrayList<>();
        if (p.has("reasons") && p.get("reasons").isJsonArray()) {
            for (JsonElement el : p.getAsJsonArray("reasons")) {
                reasons.add(el.isJsonPrimitive() ? el.getAsString() : el.toString());
            }
        }
        String title = p.has("title") && !p.get("title").isJsonNull() ? p.get("title").getAsString() : fileName;
        String url = p.has("url") && !p.get("url").isJsonNull() ? p.get("url").getAsString() : "";
        return new Entry(fileName, sha1, status, title, url, List.copyOf(reasons));
    }

    // ===== 知らせる =====

    public static int count(Status s) {
        int n = 0;
        for (Entry e : results) {
            if (e.status() == s) {
                n++;
            }
        }
        return n;
    }

    private static void onTitleInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen) || state != State.DONE || shownScreen) {
            return;
        }
        if (count(Status.MALWARE) + count(Status.WARNING) > 0) {
            shownScreen = true;
            Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new ModSafetyScreen(event.getScreen())));
        }
    }

    /** タイトル画面の左上に、チェックのようすを小さく出す。 */
    public static Component statusLine() {
        return switch (state) {
            case CHECKING -> Component.translatable("minecrafttweak.mod_safety.checking").withStyle(ChatFormatting.GRAY);
            case FAILED -> Component.translatable("minecrafttweak.mod_safety.failed", error).withStyle(ChatFormatting.RED);
            case DONE -> {
                int bad = count(Status.MALWARE);
                int warn = count(Status.WARNING);
                if (bad > 0) {
                    yield Component.translatable("minecrafttweak.mod_safety.line.malware", bad).withStyle(ChatFormatting.RED);
                }
                if (warn > 0) {
                    yield Component.translatable("minecrafttweak.mod_safety.line.warning", warn).withStyle(ChatFormatting.GOLD);
                }
                yield Component.translatable("minecrafttweak.mod_safety.line.ok", count(Status.PASSED), results.size())
                        .withStyle(ChatFormatting.GREEN);
            }
            default -> Component.translatable("minecrafttweak.mod_safety.line.idle").withStyle(ChatFormatting.GRAY);
        };
    }

    private static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen) || !enabled()) {
            return;
        }
        Component line = statusLine();
        if (line != null) {
            Component text = Component.translatable("minecrafttweak.mod_safety.prefix").append(line);
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, text, 2, LINE_Y, 0xFFFFFF);
            lineWidth = Minecraft.getInstance().font.width(text);
        } else {
            lineWidth = 0;
        }
    }

    private static final int LINE_Y = 12;
    private static int lineWidth;

    /** タイトル画面の文字を押したら、結果の画面を開く。 */
    private static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof TitleScreen) || lineWidth <= 0 || state == State.CHECKING) {
            return;
        }
        if (event.getMouseX() >= 0 && event.getMouseX() <= lineWidth + 2
                && event.getMouseY() >= LINE_Y - 1 && event.getMouseY() <= LINE_Y + 9) {
            Minecraft.getInstance().setScreen(new ModSafetyScreen(event.getScreen()));
            event.setCanceled(true);
        }
    }
}
