package com.eeeeeeruki.compacttoasts;

/** コンパクト表示で「実績達成！」「クエスト完了！」などの見出しを出すかどうか。 */
public enum HeaderMode {
    /** すべてのトーストで見出しを出す。 */
    ALL,
    /** 進捗（実績）以外のトーストで見出しを出す。 */
    EXCEPT_ADVANCEMENTS,
    /** 見出しを出さない。 */
    NONE;

    public boolean show(boolean advancement) {
        return this == ALL || (this == EXCEPT_ADVANCEMENTS && !advancement);
    }

    public HeaderMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String key() {
        return "compacttoasts.config.header." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static HeaderMode parse(String s) {
        try {
            return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            return EXCEPT_ADVANCEMENTS;
        }
    }
}
