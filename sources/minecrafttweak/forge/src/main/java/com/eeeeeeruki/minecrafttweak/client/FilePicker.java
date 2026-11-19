package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.util.function.Consumer;

/** パソコンの「ファイルを開く」画面を出して、選んだファイルの場所を返す（別のスレッドで開くので、ゲームは止まらない）。 */
public final class FilePicker {
    private static volatile boolean open;

    private FilePicker() {
    }

    public static void open(String[] patterns, String current, Consumer<String> onPicked) {
        if (open) {
            return;
        }
        open = true;
        String title = I18n.get("minecrafttweak.config.browse.title");
        String description = I18n.get("minecrafttweak.config.browse.filter", String.join(", ", patterns));
        String start = startPath(current);
        Thread thread = new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(patterns.length);
                for (String p : patterns) {
                    filters.put(stack.UTF8(p));
                }
                filters.flip();
                String result = TinyFileDialogs.tinyfd_openFileDialog(title, start, filters, description, false);
                if (result != null && !result.isEmpty()) {
                    Minecraft.getInstance().execute(() -> onPicked.accept(result));
                }
            } catch (Throwable t) {
                MinecraftTweak.LOGGER.warn("minEcraft twEEk: could not open the file dialog", t);
            } finally {
                open = false;
            }
        }, "minEcraft twEEk file dialog");
        thread.setDaemon(true);
        thread.start();
    }

    private static String startPath(String current) {
        if (current != null && !current.isBlank() && !current.startsWith("http")) {
            File f = new File(current.trim());
            if (f.getParentFile() != null && f.getParentFile().isDirectory()) {
                return f.getAbsolutePath();
            }
        }
        File music = new File(System.getProperty("user.home"), "Music");
        if (music.isDirectory()) {
            return music.getAbsolutePath() + File.separator;
        }
        return System.getProperty("user.home") + File.separator;
    }
}
