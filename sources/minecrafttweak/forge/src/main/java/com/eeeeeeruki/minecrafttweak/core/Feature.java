package com.eeeeeeruki.minecrafttweak.core;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 1つの改善機能。これを継承して feature パッケージに作り、Features に登録する。
 *
 * - イベントは register() で登録する。処理の最初で isEnabled() を確認すれば、
 *   設定画面でオフにした瞬間から止まる。
 * - 機能ごとの追加設定は addOptions() で define する。
 * - 名前と説明は lang の minecrafttweak.feature.&lt;id&gt; と .desc に書く。
 * - クライアント専用のクラスを使う処理は、別クラスに分けて Dist を確認してから呼ぶ。
 */
public abstract class Feature {
    private final String id;
    private final FeatureCategory category;
    private final boolean enabledByDefault;
    private final String comment;
    private ForgeConfigSpec.BooleanValue enabled;

    protected Feature(String id, FeatureCategory category, boolean enabledByDefault, String comment) {
        this.id = id;
        this.category = category;
        this.enabledByDefault = enabledByDefault;
        this.comment = comment;
    }

    public final String id() {
        return id;
    }

    public final FeatureCategory category() {
        return category;
    }

    public final String nameKey() {
        return "minecrafttweak.feature." + id;
    }

    public final String descriptionKey() {
        return nameKey() + ".desc";
    }

    final void buildConfig(ForgeConfigSpec.Builder builder) {
        enabled = builder.comment(comment).define("enabled", enabledByDefault);
        addOptions(builder);
    }

    /** 機能ごとの追加設定を define する（必要なときだけ上書き）。 */
    protected void addOptions(ForgeConfigSpec.Builder builder) {
    }

    /** イベントの登録など（必要なときだけ上書き）。 */
    public void register(IEventBus modBus) {
    }

    public final boolean isEnabled() {
        return enabled != null && TweakConfig.SPEC.isLoaded() && enabled.get();
    }

    public final void setEnabled(boolean value) {
        if (enabled == null || !TweakConfig.SPEC.isLoaded()) {
            return;
        }
        enabled.set(value);
        enabled.save();
    }
}
