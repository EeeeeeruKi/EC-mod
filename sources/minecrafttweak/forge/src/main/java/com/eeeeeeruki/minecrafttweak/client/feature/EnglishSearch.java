package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.feature.client.EnglishSearchFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

/**
 * 「英語でも検索」。
 * アイテムの説明（ツールチップ）の最後に、英語の名前を1行こっそり足す。
 * クリエイティブのインベントリやレシピ本の検索は説明の文字も探すので、英語でも見つかる。
 * 足した行は、画面に説明を出すときには消すので見えない。
 */
public final class EnglishSearch {
    /** 足した行の目印（文字のスタイルの insertion に入れる）。 */
    public static final String MARK = "minecrafttweak:english_name";

    private EnglishSearch() {
    }

    public static boolean active() {
        EnglishSearchFeature f = EnglishSearchFeature.INSTANCE;
        return f != null && f.isEnabled() && !EnglishNames.currentIsEnglish();
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EnglishSearch::onTooltip);
        MinecraftForge.EVENT_BUS.addListener(EnglishSearch::onGatherTooltip);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        if (!active()) {
            return;
        }
        String en = EnglishNames.of(event.getItemStack());
        if (en != null) {
            event.getToolTip().add(Component.literal(en)
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .withStyle(s -> s.withInsertion(MARK)));
        }
    }

    private static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        event.getTooltipElements().removeIf(e -> e.left().map(EnglishSearch::isMarked).orElse(false));
    }

    private static boolean isMarked(FormattedText text) {
        return text instanceof Component c && MARK.equals(c.getStyle().getInsertion());
    }
}
