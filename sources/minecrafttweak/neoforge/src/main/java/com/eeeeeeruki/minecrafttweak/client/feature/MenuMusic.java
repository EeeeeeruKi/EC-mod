package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.feature.client.MenuMusicFeature;
import com.eeeeeeruki.minecrafttweak.feature.client.PauseMusicFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 「カスタムメニューBGM」と「ポーズ画面のBGM」。
 *
 * - カスタムメニューBGM … メニュー（ワールドに入っていないとき）で、URL かファイルの曲を OpenAL で直接鳴らす。
 *   鳴らしているあいだ、ほかの音楽（バニラや Aether などのMODのメニューの曲）は止める。
 * - ポーズ画面のBGM … ポーズ画面を開いているあいだ、タイトル画面で流れていた曲を流す。
 *   タイトル画面で鳴った音楽を覚えておくので、Aether などがタイトル画面の曲を変えていてもそれが流れる。
 *   カスタムメニューBGM がオンなら、その曲をポーズ画面でも流す（続きから）。
 */
public final class MenuMusic {
    private static boolean registered;

    private static final Executor LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "minEcraft twEEk music loader");
        t.setDaemon(true);
        return t;
    });

    // カスタムメニューBGM
    private static String loadedSource = "";
    private static int generation;
    private static CompletableFuture<AudioLoader.Audio> pending;
    private static int pendingGeneration;
    private static AlMusic player;
    private static boolean started;
    private static boolean finished;
    private static boolean customActive;
    private static Component status;
    /** 音の仕組みが作り直されたので、使えるようになったら曲を読み直す。 */
    private static boolean reloadWhenReady;

    // ポーズ画面のBGM
    private static ResourceLocation titleMusic;
    private static boolean pauseWanted;
    private static SoundInstance pauseSound;
    private static int pauseDelay;

    private MenuMusic() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(MenuMusic::onTick);
        NeoForge.EVENT_BUS.addListener(MenuMusic::onPlaySound);
        NeoForge.EVENT_BUS.addListener(MenuMusic::onRender);
    }

    private static boolean customEnabled() {
        return MenuMusicFeature.INSTANCE != null && MenuMusicFeature.INSTANCE.isEnabled();
    }

    private static boolean pauseEnabled() {
        return PauseMusicFeature.INSTANCE != null && PauseMusicFeature.INSTANCE.isEnabled();
    }

    private static Path cacheDir() {
        // ダウンロードした曲は設定ではないので、config ではなく cache/minecrafttweak/music に置く（前の場所からは移す）
        Path game = Minecraft.getInstance().gameDirectory.toPath();
        Path dir = game.resolve("cache").resolve("minecrafttweak").resolve("music");
        Path old = game.resolve("config").resolve("minecrafttweak").resolve("music_cache");
        try {
            if (java.nio.file.Files.isDirectory(old) && !java.nio.file.Files.exists(dir)) {
                java.nio.file.Files.createDirectories(dir.getParent());
                java.nio.file.Files.move(old, dir);
            }
        } catch (java.io.IOException ignored) {
            // 移せなければ新しい場所でダウンロードし直す
        }
        return dir;
    }

    private static void startLoad(String source) {
        int gen = ++generation;
        pendingGeneration = gen;
        status = Component.translatable("minecrafttweak.menu_music.loading");
        Path dir = cacheDir();
        pending = CompletableFuture.supplyAsync(() -> {
            try {
                return AudioLoader.load(source, dir);
            } catch (AudioLoader.LoadException e) {
                throw new CompletionException(e);
            }
        }, LOADER);
    }

    private static void release() {
        if (player != null) {
            if (SoundReady.ready()) {
                player.delete();
            }
            player = null;
        }
        if (pending != null) {
            CompletableFuture<AudioLoader.Audio> old = pending;
            old.thenAccept(AudioLoader.Audio::free); // 読み終わったら捨てる
            pending = null;
        }
        generation++;
        started = false;
        finished = false;
        status = null;
    }

    private static void stopOtherMusic(Minecraft mc) {
        mc.getSoundManager().stop(null, SoundSource.MUSIC);
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inMenu = mc.level == null;
        boolean onPause = mc.level != null && mc.screen instanceof PauseScreen;

        // ===== カスタムメニューBGM =====
        String source = customEnabled() ? MenuMusicFeature.INSTANCE.url.get().trim() : "";
        if (!source.equals(loadedSource)) {
            release();
            loadedSource = source;
            if (!source.isEmpty()) {
                startLoad(source);
            }
        }
        boolean soundReady = SoundReady.ready();
        if (!soundReady) {
            // 読み込み中・作り直し中は OpenAL を一切さわらない（さわると音もリソースの読み込みも壊れる）
            if (player != null) {
                player = null; // 中身は作り直しで消える
                started = false;
                reloadWhenReady = true;
            }
            customActive = false;
        } else if (reloadWhenReady) {
            reloadWhenReady = false;
            if (!source.isEmpty() && pending == null) {
                startLoad(source);
            }
        }
        if (soundReady && pending != null && pending.isDone()) {
            CompletableFuture<AudioLoader.Audio> done = pending;
            pending = null;
            try {
                AudioLoader.Audio audio = done.join();
                try {
                    if (pendingGeneration == generation) {
                        player = new AlMusic(audio);
                        status = null;
                    }
                } finally {
                    audio.free();
                }
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof AudioLoader.LoadException le) {
                    status = Component.translatable(le.key, le.detail).withStyle(ChatFormatting.RED);
                } else {
                    status = Component.translatable("minecrafttweak.menu_music.error.io", cause == null ? "?" : cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName())
                            .withStyle(ChatFormatting.RED);
                }
                MinecraftTweak.LOGGER.warn("minEcraft twEEk: could not load menu music {}", source, cause);
            } catch (Throwable t) {
                status = Component.translatable("minecrafttweak.menu_music.error.io", t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName())
                        .withStyle(ChatFormatting.RED);
                MinecraftTweak.LOGGER.warn("minEcraft twEEk: could not play menu music {}", source, t);
            }
        }
        if (soundReady && player != null && !player.valid()) {
            // 音の設定を変えたりして OpenAL が作り直された。読み直す（2回目はファイルから）
            player = null;
            started = false;
            startLoad(source);
        }

        // 「途切れさせない」がオンなら、ワールドの中でも・ポーズを解除しても・設定を開いても流しつづける
        boolean keepPlaying = customEnabled() && MenuMusicFeature.INSTANCE.keepPlaying.get();
        boolean customWanted = !source.isEmpty() && (keepPlaying || inMenu || (onPause && pauseEnabled()));
        if (!customWanted) {
            finished = false;
        }
        boolean wasCustomActive = customActive;
        customActive = soundReady && customWanted && player != null && !finished;
        if (customActive) {
            if (!wasCustomActive) {
                stopOtherMusic(mc);
            }
            boolean loop = MenuMusicFeature.INSTANCE.loop.get();
            player.setLooping(loop);
            player.setGain(MenuMusicFeature.INSTANCE.volume.get() / 100f
                    * mc.options.getSoundSourceVolume(SoundSource.MUSIC));
            if (!player.playing()) {
                if (started && !player.paused() && !loop) {
                    finished = true; // 1回だけ流す設定で、最後まで流れた
                    customActive = false;
                } else {
                    player.play();
                    started = true;
                }
            }
        } else if (soundReady && player != null && player.playing()) {
            player.pause();
        }

        // ===== ポーズ画面のBGM（バニラの音の仕組みで流す） =====
        boolean wasPauseWanted = pauseWanted;
        pauseWanted = pauseEnabled() && onPause && !customActive;
        if (pauseWanted) {
            if (!wasPauseWanted) {
                stopOtherMusic(mc);
                pauseSound = null;
                pauseDelay = 5; // ゲームが止まって音が一時停止されたあとに鳴らす
            }
            if (pauseSound == null || !mc.getSoundManager().isActive(pauseSound)) {
                if (pauseDelay-- <= 0) {
                    SoundEvent event2 = titleMusic != null
                            ? SoundEvent.createVariableRangeEvent(titleMusic)
                            : SoundEvents.MUSIC_MENU.value();
                    pauseSound = SimpleSoundInstance.forMusic(event2);
                    mc.getSoundManager().play(pauseSound);
                    pauseDelay = 100; // 次の曲までの間
                }
            }
        } else if (wasPauseWanted) {
            if (pauseSound != null) {
                mc.getSoundManager().stop(pauseSound);
            }
            pauseSound = null;
        }
    }

    private static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || sound.getSource() != SoundSource.MUSIC || sound == pauseSound) {
            return;
        }
        if (customActive || pauseWanted) {
            event.setSound(null); // ほかの音楽は止めておく
            return;
        }
        if (Minecraft.getInstance().level == null) {
            titleMusic = sound.getLocation(); // タイトル画面で流れた曲を覚える（Aether などの曲も）
        }
    }

    private static void onRender(ScreenEvent.Render.Post event) {
        if (status != null && customEnabled() && event.getScreen() instanceof TitleScreen) {
            Component text = Component.translatable("minecrafttweak.menu_music.status").append(status);
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, text, 2, 2, 0xFFFFFF);
        }
    }
}
