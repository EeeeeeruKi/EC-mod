package com.eeeeeeruki.claudeupdater.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Swaps downloaded jars into the mods folder.
 *
 * <p>The game keeps its mod jars open while it runs, and on Windows an open file cannot be deleted. So the
 * game only records what to swap in pending.txt; when it exits, a second small Java process is started with
 * {@link #main} from a copy of this jar. It waits for the game process to end, then deletes each old jar and
 * moves the new one in. The new versions load on the next launch.
 *
 * <p>This class runs in that helper process with nothing but the JDK on the classpath, so it must not use
 * Gson, the loader, Minecraft or any other class from this mod that does.
 *
 * <p>pending.txt has one tab-separated line per mod: {@code MOD id oldJar stagedJar targetJar}.
 */
public final class Apply {
    private Apply() {
    }

    public record Op(String id, Path oldJar, Path staged, Path target) {
        String line() {
            return String.join("\t", "MOD", id, oldJar.toString(), staged.toString(), target.toString());
        }
    }

    public static synchronized List<Op> read(Path pending) {
        List<Op> ops = new ArrayList<>();
        if (!Files.isRegularFile(pending)) return ops;
        try {
            for (String line : Files.readAllLines(pending, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t");
                if (p.length == 5 && p[0].equals("MOD")) {
                    ops.add(new Op(p[1], Paths.get(p[2]), Paths.get(p[3]), Paths.get(p[4])));
                }
            }
        } catch (IOException e) {
            log("pending.txt を読めませんでした: " + e);
        }
        return ops;
    }

    public static synchronized void write(Path pending, List<Op> ops) throws IOException {
        if (ops.isEmpty()) {
            Files.deleteIfExists(pending);
            return;
        }
        Files.createDirectories(pending.getParent());
        List<String> lines = new ArrayList<>();
        for (Op op : ops) lines.add(op.line());
        Path tmp = pending.resolveSibling(pending.getFileName() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        Files.move(tmp, pending, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Adds or replaces the entry for one mod (a newer download replaces an older, not yet applied one). */
    public static synchronized void put(Path pending, Op op) throws IOException {
        List<Op> ops = read(pending);
        ops.removeIf(o -> o.id().equals(op.id()));
        ops.add(op);
        write(pending, ops);
    }

    /** Helper entry point: {@code java -cp helper.jar ...Apply <game pid> <pending.txt>}. */
    public static void main(String[] args) throws Exception {
        // apply.log is always UTF-8, whatever the system code page is
        System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, StandardCharsets.UTF_8));
        if (args.length < 2) {
            log("引数が足りません");
            return;
        }
        long pid = Long.parseLong(args[0]);
        Path pending = Paths.get(args[1]);
        log("ゲームの終了を待っています (pid " + pid + ")");
        Optional<ProcessHandle> game = ProcessHandle.of(pid);
        if (game.isPresent()) {
            try {
                game.get().onExit().get(); // started while the game runs, so wait as long as it takes
            } catch (Exception e) {
                log("ゲームの終了を待てませんでした: " + e);
                return;
            }
        }
        Thread.sleep(1000);
        List<Op> left = new ArrayList<>();
        for (Op op : read(pending)) {
            if (!applyOne(op)) left.add(op);
        }
        write(pending, left);
        log(left.isEmpty() ? "すべての入れ替えが完了しました" : left.size() + "件は次回の終了時にもう一度試します");
    }

    /**
     * Tries the swap from inside the running game. The loaders open mod jars with Java NIO, which on Windows
     * shares the file for deletion, so the old jar can usually be deleted even while it is loaded. Ops that
     * cannot be done here are returned and left for the helper process.
     */
    public static synchronized List<Op> applyNow(Path pending, Path logFile) {
        List<Op> left = new ArrayList<>();
        StringBuilder log = new StringBuilder();
        for (Op op : read(pending)) {
            if (!Files.isRegularFile(op.staged())) {
                log.append(stamp()).append(op.id()).append(": ダウンロードしたファイルが見つからないため取り消します\n");
                continue;
            }
            try {
                Files.deleteIfExists(op.oldJar());
            } catch (IOException e) {
                log.append(stamp()).append(op.id()).append(": ゲーム中は古いjarを消せませんでした（終了後に入れ替えます） ")
                        .append(e).append("\n");
                left.add(op);
                continue;
            }
            try {
                Files.move(op.staged(), op.target(), StandardCopyOption.REPLACE_EXISTING);
                log.append(stamp()).append(op.id()).append(": 入れ替えました -> ").append(op.target().getFileName()).append("\n");
            } catch (IOException e) {
                // old jar is gone already: put the new one back in place later, never leave the mod missing
                log.append(stamp()).append(op.id()).append(": 新しいjarを置けませんでした（終了後にもう一度試します） ")
                        .append(e).append("\n");
                left.add(op);
            }
        }
        try {
            write(pending, left);
        } catch (IOException e) {
            log.append(stamp()).append("pending.txt を書けませんでした: ").append(e).append("\n");
        }
        appendLog(logFile, log.toString());
        return left;
    }

    static void appendLog(Path logFile, String text) {
        if (text.isEmpty()) return;
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, text, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private static String stamp() {
        return LocalDateTime.now().withNano(0) + " ";
    }

    private static boolean applyOne(Op op) {
        if (!Files.isRegularFile(op.staged())) {
            log(op.id() + ": ダウンロードしたファイルが見つからないため取り消します");
            return true; // nothing left to do for this entry
        }
        // 1) the old jar must be gone first, otherwise the next launch would load two copies of the mod
        boolean gone = false;
        for (int i = 0; i < 60 && !gone; i++) {
            try {
                Files.deleteIfExists(op.oldJar());
                gone = true;
            } catch (IOException e) {
                sleep(500);
            }
        }
        if (!gone) {
            log(op.id() + ": 古いjarを消せませんでした（使用中の可能性があります） " + op.oldJar());
            return false;
        }
        // 2) move the new jar in
        for (int i = 0; i < 20; i++) {
            try {
                Files.move(op.staged(), op.target(), StandardCopyOption.REPLACE_EXISTING);
                log(op.id() + ": 入れ替えました -> " + op.target().getFileName());
                return true;
            } catch (IOException e) {
                if (i == 19) log(op.id() + ": 新しいjarを置けませんでした: " + e);
                sleep(500);
            }
        }
        return false;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void log(String s) {
        System.out.println(LocalDateTime.now().withNano(0) + " " + s);
    }
}
