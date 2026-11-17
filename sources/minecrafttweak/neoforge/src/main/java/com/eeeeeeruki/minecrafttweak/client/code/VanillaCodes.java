package com.eeeeeeruki.minecrafttweak.client.code;

import com.eeeeeeruki.minecrafttweak.core.CodeEntry;
import com.eeeeeeruki.minecrafttweak.core.CodeProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * バニラの設定（頭文字 v）。
 * 番号は一度決めたら変えない。新しい項目は必ず一番下に足す。
 */
public final class VanillaCodes implements CodeProvider {
    private final List<CodeEntry> entries = new ArrayList<>();

    public VanillaCodes() {
        // ビデオ設定
        add(1, Options::renderDistance);
        add(2, Options::simulationDistance);
        add(3, Options::framerateLimit);
        add(4, Options::graphicsMode);
        add(5, Options::ambientOcclusion);
        add(6, Options::prioritizeChunkUpdates);
        add(7, Options::guiScale);
        add(8, Options::fov);
        add(9, Options::gamma);
        add(10, Options::enableVsync);
        add(11, Options::entityShadows);
        add(12, Options::particles);
        add(13, Options::mipmapLevels);
        add(14, Options::biomeBlendRadius);
        add(15, Options::entityDistanceScaling);
        add(16, Options::cloudStatus);
        add(17, Options::attackIndicator);
        add(18, Options::bobView);
        add(19, Options::screenEffectScale);
        add(20, Options::fovEffectScale);
        add(21, Options::darknessEffectScale);
        add(22, Options::glintSpeed);
        add(23, Options::glintStrength);
        add(24, Options::damageTiltStrength);
        add(25, Options::showAutosaveIndicator);
        add(26, Options::fullscreen);
        // 操作
        add(27, Options::sensitivity);
        add(28, Options::invertYMouse);
        add(29, Options::autoJump);
        add(30, Options::toggleCrouch);
        add(31, Options::toggleSprint);
        add(32, Options::mouseWheelSensitivity);
        add(33, Options::rawMouseInput);
        add(34, Options::discreteMouseScroll);
        // チャット
        add(35, Options::chatVisibility);
        add(36, Options::chatColors);
        add(37, Options::chatLinks);
        add(38, Options::chatOpacity);
        add(39, Options::textBackgroundOpacity);
        add(40, Options::chatScale);
        add(41, Options::chatWidth);
        add(42, Options::chatLineSpacing);
        add(43, Options::backgroundForChatOnly);
        // アクセシビリティ・その他
        add(44, Options::showSubtitles);
        add(45, Options::hideLightningFlash);
        add(46, Options::highContrast);
        add(47, Options::mainHand);
        // 音量
        add(48, o -> o.getSoundSourceOptionInstance(SoundSource.MASTER));
        add(49, o -> o.getSoundSourceOptionInstance(SoundSource.MUSIC));
        add(50, o -> o.getSoundSourceOptionInstance(SoundSource.RECORDS));
        add(51, o -> o.getSoundSourceOptionInstance(SoundSource.WEATHER));
        add(52, o -> o.getSoundSourceOptionInstance(SoundSource.BLOCKS));
        add(53, o -> o.getSoundSourceOptionInstance(SoundSource.HOSTILE));
        add(54, o -> o.getSoundSourceOptionInstance(SoundSource.NEUTRAL));
        add(55, o -> o.getSoundSourceOptionInstance(SoundSource.PLAYERS));
        add(56, o -> o.getSoundSourceOptionInstance(SoundSource.AMBIENT));
        add(57, o -> o.getSoundSourceOptionInstance(SoundSource.VOICE));
        // 1.21 から
        add(58, Options::menuBackgroundBlurriness);
    }

    private void add(int number, Function<Options, OptionInstance<?>> getter) {
        entries.add(new Entry("v" + number, getter));
    }

    @Override
    public String prefix() {
        return "v";
    }

    @Override
    public Component displayName() {
        return Component.translatable("minecrafttweak.code.provider.v");
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public List<CodeEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public void afterApply() {
        Minecraft.getInstance().options.save();
    }

    private record Entry(String key, Function<Options, OptionInstance<?>> getter) implements CodeEntry {
        private OptionInstance<?> option() {
            return getter.apply(Minecraft.getInstance().options);
        }

        @Override
        public Component name() {
            return Component.literal(option().toString());
        }

        @Override
        public boolean available() {
            return Minecraft.getInstance().options != null;
        }

        @Override
        public String textValue() {
            return CodeValues.format(option().get());
        }

        @Override
        public Component apply(String raw) {
            return applyTo(option(), raw);
        }

        @SuppressWarnings("unchecked")
        private <T> Component applyTo(OptionInstance<T> option, String raw) {
            T current = option.get();
            Object parsed = CodeValues.parseLike(current, current.getClass(), raw);
            if (parsed == null) {
                return Component.translatable("minecrafttweak.code.error.invalid", key, raw);
            }
            Optional<T> valid = option.values().validateValue((T) parsed);
            if (valid.isEmpty()) {
                return Component.translatable("minecrafttweak.code.error.invalid", key, raw);
            }
            option.set(valid.get());
            return null;
        }
    }
}
