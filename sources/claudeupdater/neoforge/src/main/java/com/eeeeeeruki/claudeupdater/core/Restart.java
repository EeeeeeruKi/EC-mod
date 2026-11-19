package com.eeeeeeruki.claudeupdater.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Starts the game again after the swap ("適用して再起動").
 *
 * <p>The game works out how to start itself and writes it to restart.txt (UTF-8: the folder to start in on the
 * first line, then one argument per line). The helper process reads it after the swap and runs it. A file is used
 * instead of command-line arguments so that non-ASCII paths (e.g. a Japanese instance name) never pass through
 * the Windows code page.
 *
 * <p>Like {@link Apply}, this class also runs in the helper process, so it may use nothing but the JDK.
 */
public final class Restart {
    private Restart() {
    }

    public record Plan(Path dir, List<String> command, String how) {
    }

    /** How to start this game again, or empty when it cannot be worked out. Runs inside the game. */
    public static Optional<Plan> plan(Path gameDir) {
        Optional<Plan> p = viaLauncher(gameDir);
        if (p.isPresent()) return p;
        return sameCommand(gameDir);
    }

    /**
     * Prism Launcher / MultiMC: the game is a child of the launcher, and the launcher puts INST_ID in the game's
     * environment. "launcher --launch id" starts the instance again (and hands over to a launcher that is open).
     */
    private static Optional<Plan> viaLauncher(Path gameDir) {
        String id = System.getenv("INST_ID");
        if (id == null || id.isBlank()) return Optional.empty();
        Path exe = null;
        Optional<ProcessHandle> parent = ProcessHandle.current().parent();
        if (parent.isPresent()) {
            String cmd = parent.get().info().command().orElse(null);
            if (cmd != null && isLauncher(Path.of(cmd))) exe = Path.of(cmd);
        }
        if (exe == null) {
            // portable installs keep the launcher next to the instances folder
            String instDir = System.getenv("INST_DIR");
            List<Path> roots = new ArrayList<>();
            if (instDir != null && Path.of(instDir).getParent() != null && Path.of(instDir).getParent().getParent() != null)
                roots.add(Path.of(instDir).getParent().getParent());
            String local = System.getenv("LOCALAPPDATA");
            if (local != null) roots.add(Path.of(local, "Programs", "PrismLauncher"));
            for (Path r : roots) {
                for (String n : new String[]{"prismlauncher.exe", "PrismLauncher.exe", "prismlauncher", "MultiMC.exe", "MultiMC"}) {
                    Path c = r.resolve(n);
                    if (Files.isRegularFile(c)) {
                        exe = c;
                        break;
                    }
                }
                if (exe != null) break;
            }
        }
        if (exe == null) return Optional.empty();
        String flag = exe.getFileName().toString().toLowerCase(Locale.ROOT).startsWith("multimc") ? "-l" : "--launch";
        Path dir = exe.getParent() != null ? exe.getParent() : gameDir;
        return Optional.of(new Plan(dir, List.of(exe.toString(), flag, id), "launcher"));
    }

    private static boolean isLauncher(Path exe) {
        String n = exe.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.startsWith("prismlauncher") || n.startsWith("multimc") || n.startsWith("polymc");
    }

    /** Any other launcher: run the same Java command again, when the OS tells us what it was. */
    private static Optional<Plan> sameCommand(Path gameDir) {
        ProcessHandle.Info info = ProcessHandle.current().info();
        Optional<String> cmd = info.command();
        Optional<String[]> args = info.arguments();
        if (cmd.isEmpty() || args.isEmpty() || args.get().length == 0) return Optional.empty();
        List<String> c = new ArrayList<>();
        c.add(cmd.get());
        c.addAll(List.of(args.get()));
        Path dir = Path.of(System.getProperty("user.dir", gameDir.toString()));
        return Optional.of(new Plan(dir, c, "command"));
    }

    public static void write(Path file, Plan plan) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(plan.dir().toString());
        lines.addAll(plan.command());
        Files.createDirectories(file.getParent());
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /** Runs the plan in restart.txt, if there is one. Runs in the helper process after the swap. */
    static void runIfRequested(Path file) {
        if (!Files.isRegularFile(file)) return;
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log("restart.txt を読めませんでした: " + e);
            return;
        }
        if (lines.size() < 2) return;
        try {
            Thread.sleep(2000); // let the launcher notice that the game has closed
            ProcessBuilder pb = new ProcessBuilder(lines.subList(1, lines.size()));
            pb.directory(Path.of(lines.get(0)).toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.start();
            log("ゲームをもう一度起動しました: " + lines.get(1));
        } catch (Exception e) {
            log("ゲームをもう一度起動できませんでした: " + e);
        }
    }

    private static void log(String s) {
        System.out.println(LocalDateTime.now().withNano(0) + " " + s);
    }
}
