package com.eeeeeeruki.claudeupdater.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Checks the index, downloads new jars and schedules the swap for when the game exits. */
public final class UpdaterCore {
    private UpdaterCore() {
    }

    public static final String USER_AGENT = "ClaudeUpdater/" + UpdaterCore.class.getPackage().getImplementationVersion();

    private static Env env;
    private static UpdaterConfig config;
    private static HttpClient http;

    private static volatile boolean checked;
    private static volatile List<UpdateCandidate> found = List.of();
    private static volatile String checkError;

    public static Env env() {
        return env;
    }

    public static UpdaterConfig config() {
        return config;
    }

    /** True once the startup check has finished (successfully or not). */
    public static boolean checked() {
        return checked;
    }

    public static List<UpdateCandidate> found() {
        return found;
    }

    public static String checkError() {
        return checkError;
    }

    /**
     * Called once from the mod constructor.
     *
     * @param dedicatedServer when true and serverAutoUpdate is on, every update is downloaded without asking
     */
    private static List<InstalledMod> installedMods = List.of();
    private static Set<String> loadedIds = Set.of();

    public static List<InstalledMod> installed() {
        return installedMods;
    }

    /**
     * @param loadedIds every loaded mod ID (including ones not in the mods folder), so the install screen
     *                  does not offer a mod that is already there
     */
    public static void start(Env e, List<InstalledMod> installed, Set<String> loadedIds, boolean dedicatedServer) {
        env = e;
        installedMods = List.copyOf(installed);
        UpdaterCore.loadedIds = Set.copyOf(loadedIds);
        config = UpdaterConfig.load(e.configFile());
        http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL) // GitHub release downloads redirect to a CDN
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds))
                .build();
        applyLeftovers();
        registerShutdownHook();

        if (!config.checkOnStartup) {
            checked = true;
            return;
        }
        Thread t = new Thread(() -> {
            try {
                found = check(installed, dedicatedServer); // the startup screen leaves out hidden mods
                if (found.isEmpty()) {
                    Log.info("すべてのMODが最新です（確認したMOD: " + installed.size() + "個）");
                } else {
                    for (UpdateCandidate c : found) {
                        Log.info("新しい版があります: " + c.name + " " + c.installed.version() + " -> " + c.release.version);
                    }
                }
                if (dedicatedServer && config.serverAutoUpdate && !found.isEmpty()) {
                    downloadAll(found);
                    Log.info("ダウンロードした更新は、サーバーを停止したあとに入れ替わります。");
                }
            } catch (Exception ex) {
                checkError = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                Log.warn("更新の確認に失敗しました: " + checkError);
            } finally {
                checked = true;
            }
        }, "ClaudeUpdater-Check");
        t.setDaemon(true);
        t.start();
    }

    /** @param includeHidden true for the 設定 button (shows everything), false for the startup screen */
    public static List<UpdateCandidate> check(List<InstalledMod> installed, boolean includeHidden)
            throws IOException, InterruptedException {
        return findUpdates(fetchIndex(), installed, includeHidden);
    }

    /** Mods in the index that are not installed, for the hidden install screen. */
    public static List<UpdateCandidate> checkInstallable() throws IOException, InterruptedException {
        return findInstallable(fetchIndex());
    }

    public static List<UpdateCandidate> findInstallable(ModIndex index) {
        Set<String> pending = new HashSet<>();
        for (Apply.Op op : Apply.read(env.pendingFile())) pending.add(op.id());
        List<UpdateCandidate> out = new ArrayList<>();
        for (var e : index.mods.entrySet()) {
            String id = e.getKey();
            ModIndex.ModEntry entry = e.getValue();
            if (entry == null || entry.versions == null || Boolean.FALSE.equals(entry.installable)) continue;
            if (loadedIds.contains(id) || pending.contains(id)) continue;
            ModIndex.Release best = null;
            for (ModIndex.Release r : entry.versions) {
                if (!usable(r)) continue;
                if (best == null || Versions.compare(r.version, best.version) > 0) best = r;
            }
            if (best == null) continue;
            String name = entry.name != null && !entry.name.isBlank() ? entry.name : id;
            out.add(new UpdateCandidate(id, name, entry.summary, best));
        }
        out.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    private static ModIndex fetchIndex() throws IOException, InterruptedException {
        String url = config.indexUrl + (config.indexUrl.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis() / 60000;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(config.timeoutSeconds))
                .header("User-Agent", USER_AGENT)
                .header("Cache-Control", "no-cache")
                .GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() != 200) throw new IOException("index.json の取得に失敗しました (HTTP " + res.statusCode() + ")");
        ModIndex index = UpdaterConfig.parseIndex(res.body());
        if (index == null || index.mods == null) throw new IOException("index.json の形式が正しくありません");
        return index;
    }

    /** Pure matching step, separate so it can be tested without the network. */
    public static List<UpdateCandidate> findUpdates(ModIndex index, List<InstalledMod> installed, boolean includeHidden) {
        Set<String> alreadyPending = new HashSet<>();
        for (Apply.Op op : Apply.read(env.pendingFile())) alreadyPending.add(op.id());

        List<UpdateCandidate> out = new ArrayList<>();
        for (InstalledMod mod : installed) {
            ModIndex.ModEntry entry = index.mods.get(mod.id());
            if (entry == null || entry.versions == null) continue;
            if (alreadyPending.contains(mod.id())) continue; // downloaded already, waiting for the game to exit
            ModIndex.Release best = null;
            for (ModIndex.Release r : entry.versions) {
                if (!usable(r)) continue;
                if (best == null || Versions.compare(r.version, best.version) > 0) best = r;
            }
            if (best == null) continue;
            if (Versions.compare(best.version, mod.version()) <= 0) continue;
            if (!includeHidden && config.hidden.contains(mod.id())) continue;
            String name = entry.name != null && !entry.name.isBlank() ? entry.name : mod.name();
            out.add(new UpdateCandidate(mod, name, best));
        }
        return out;
    }

    private static boolean usable(ModIndex.Release r) {
        return r != null && r.version != null
                && env.mcVersion().equals(r.mc)
                && env.loader().equalsIgnoreCase(r.loader)
                && r.url != null && r.url.startsWith("https://")
                && r.sha256 != null && r.sha256.length() == 64;
    }

    public static void downloadAll(List<UpdateCandidate> list) {
        for (UpdateCandidate c : list) {
            if (!c.selected) continue;
            download(c);
        }
        scheduleSwap();
    }

    /** Downloads one jar into the staging folder, checks its hash and records the swap in pending.txt. */
    public static void download(UpdateCandidate c) {
        c.state = UpdateCandidate.State.DOWNLOADING;
        c.error = null;
        Path tmp = null;
        try {
            String fileName = safeFileName(c);
            Files.createDirectories(env.stagingDir());
            Path staged = env.stagingDir().resolve(fileName);
            tmp = env.stagingDir().resolve(fileName + ".part");

            HttpRequest req = HttpRequest.newBuilder(URI.create(c.release.url))
                    .timeout(Duration.ofMinutes(5))
                    .header("User-Agent", USER_AGENT)
                    .GET().build();
            HttpResponse<InputStream> res = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() != 200) {
                res.body().close();
                throw new IOException("HTTP " + res.statusCode());
            }
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            try (InputStream in = res.body(); var out = Files.newOutputStream(tmp)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    sha.update(buf, 0, n);
                    out.write(buf, 0, n);
                }
            }
            String got = hex(sha.digest());
            if (!got.equalsIgnoreCase(c.release.sha256)) {
                throw new IOException("ファイルが壊れています（SHA-256が一致しません）");
            }
            Files.move(tmp, staged, StandardCopyOption.REPLACE_EXISTING);
            Path target = env.modsDir().resolve(fileName);
            if (c.isNewInstall()) {
                // nothing to replace: a new jar can go straight into the mods folder, it loads on the next launch
                if (Files.exists(target)) {
                    Files.deleteIfExists(staged);
                    throw new IOException("mods フォルダに同じ名前のファイルがあります: " + fileName);
                }
                Files.move(staged, target);
            } else {
                Apply.put(env.pendingFile(), new Apply.Op(c.installed.id(),
                        c.installed.jar().toAbsolutePath(), staged.toAbsolutePath(), target.toAbsolutePath()));
            }
            c.state = UpdateCandidate.State.DONE;
            Log.info("ダウンロードしました: " + c.name + " " + c.release.version);
        } catch (Exception e) {
            c.error = e.getMessage() == null ? e.toString() : e.getMessage();
            c.state = UpdateCandidate.State.FAILED;
            Log.warn(c.name + " のダウンロードに失敗しました: " + c.error);
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /** The startup screen will not show these mods again, whatever version comes out. */
    public static void hide(List<UpdateCandidate> list) {
        for (UpdateCandidate c : list) config.hidden.add(c.id);
        config.save(env.configFile());
    }

    public static void unhide(List<UpdateCandidate> list) {
        for (UpdateCandidate c : list) config.hidden.remove(c.id);
        config.save(env.configFile());
    }

    public static boolean isHidden(UpdateCandidate c) {
        return config.hidden.contains(c.id);
    }

    private static String safeFileName(UpdateCandidate c) throws IOException {
        String name = c.release.fileName;
        if (name == null || name.isBlank()) {
            String path = URI.create(c.release.url).getPath();
            name = path.substring(path.lastIndexOf('/') + 1);
        }
        if (!name.toLowerCase(Locale.ROOT).endsWith(".jar") || name.contains("/") || name.contains("\\")
                || name.contains("..") || name.length() > 200) {
            throw new IOException("ファイル名が不正です: " + name);
        }
        return name;
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    // ---------------------------------------------------------------------------------------------
    // after the game exits

    /**
     * The swap is attempted two ways, so it still happens if one of them is cut off:
     * <ol>
     *   <li>a helper process started as soon as something is downloaded (and at startup when an earlier swap is
     *       still pending), which waits for this game process to end and then swaps;</li>
     *   <li>the game's own shutdown hook, which swaps in-process once the game has stopped (Java opens jars so
     *       that Windows still lets them be deleted).</li>
     * </ol>
     * Whichever runs first does the work; the other finds pending.txt empty.
     */
    private static final java.util.concurrent.atomic.AtomicBoolean helperStarted = new java.util.concurrent.atomic.AtomicBoolean();

    private static Path applyLog() {
        return env.workDir().resolve("apply.log");
    }

    /** Swaps left over from the last session: queue them for when this session ends. */
    private static void applyLeftovers() {
        List<Apply.Op> ops = Apply.read(env.pendingFile());
        if (ops.isEmpty()) return;
        Log.warn("前回入れ替えられなかった更新が " + ops.size() + " 件あります。ゲームを終了すると入れ替えます。");
        startHelper();
    }

    /** Called after downloads finish. */
    public static void scheduleSwap() {
        if (!Apply.read(env.pendingFile()).isEmpty()) startHelper();
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (Apply.read(env.pendingFile()).isEmpty()) return;
                if (Apply.applyNow(env.pendingFile(), applyLog()).isEmpty()) return;
                startHelper(); // still locked: let the helper retry after the process is gone
            } catch (Throwable t) {
                System.err.println("[ClaudeUpdater] 入れ替えに失敗しました: " + t);
            }
        }, "ClaudeUpdater-Apply"));
    }

    private static void startHelper() {
        if (!helperStarted.compareAndSet(false, true)) return;
        try {
            Files.createDirectories(env.workDir());
            // run the helper from a copy, so the updater can replace its own jar too
            Path helper = env.workDir().resolve("helper.jar");
            Files.copy(env.selfJar(), helper, StandardCopyOption.REPLACE_EXISTING);

            // relative paths on purpose: the helper starts inside the work folder, so a game folder with
            // non-ASCII characters (e.g. a Japanese instance name) never has to pass through the command line,
            // which Windows converts with the system code page
            ProcessBuilder pb = new ProcessBuilder(javaExecutable(), "-cp", "helper.jar",
                    Apply.class.getName(), Long.toString(ProcessHandle.current().pid()),
                    env.workDir().relativize(env.pendingFile()).toString());
            pb.directory(env.workDir().toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(applyLog().toFile()));
            pb.start();
            Log.info("入れ替え用のプロセスを起動しました（ゲームの終了を待っています）");
        } catch (Throwable t) {
            helperStarted.set(false);
            Apply.appendLog(applyLog(), java.time.LocalDateTime.now().withNano(0) + " 入れ替え用のプロセスを起動できませんでした: " + t + "\n");
            Log.warn("入れ替え用のプロセスを起動できませんでした: " + t);
        }
    }

    private static String javaExecutable() {
        String cmd = ProcessHandle.current().info().command().orElse(null);
        if (cmd != null) {
            Path p = Path.of(cmd);
            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
            if (n.equals("java.exe")) { // no console window on Windows
                Path w = p.resolveSibling("javaw.exe");
                if (Files.isRegularFile(w)) return w.toString();
            }
            if (n.startsWith("java")) return cmd;
        }
        boolean win = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        return Path.of(System.getProperty("java.home"), "bin", win ? "javaw.exe" : "java").toString();
    }
}
