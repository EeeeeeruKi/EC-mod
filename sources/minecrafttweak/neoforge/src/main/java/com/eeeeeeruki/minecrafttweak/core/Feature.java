package com.eeeeeeruki.minecrafttweak.core;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1つの改善機能。これを継承して feature パッケージに作り、Features に登録する。
 *
 * - Quark にある機能は作らない（Quark は前提MOD）。
 * - 機能は最初はオフ（ゑるきが「デフォルトで有効」と言ったものだけオン）。
 * - イベントは register() で登録する。処理の最初で isEnabled() を確認すれば、
 *   設定でオフにした瞬間から止まる。
 * - 機能ごとの追加設定は addOptions() の中で intOption / doubleOption / boolOption を使って作る。
 *   作った順に「機能番号a」「機能番号b」…の番号が付き、詳細設定画面と設定コードで変更できる。
 * - 名前と説明は lang の minecrafttweak.feature.&lt;id&gt; と .desc、
 *   詳細設定の名前は minecrafttweak.feature.&lt;id&gt;.&lt;設定id&gt;、説明は .desc に書く。
 * - クライアント専用のクラスを使う処理は、別クラスに分けて Dist を確認してから呼ぶ。
 */
public abstract class Feature {
    private final String id;
    private final FeatureCategory category;
    private final String comment;
    private final boolean enabledByDefault;
    private final List<FeatureOption> options = new ArrayList<>();
    private ModConfigSpec.BooleanValue enabled;

    protected Feature(String id, FeatureCategory category, String comment) {
        this(id, category, false, comment);
    }

    /** 最初からオンにしたい機能だけ（ゑるきが指定したもの）。 */
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

    /** テキスト設定で使う番号（1から）。 */
    public final int number() {
        return Features.numberOf(this);
    }

    public final List<FeatureOption> options() {
        return Collections.unmodifiableList(options);
    }

    final void buildConfig(ModConfigSpec.Builder builder) {
        enabled = builder.comment(comment).define("enabled", enabledByDefault);
        addOptions(builder);
    }

    /** 機能ごとの追加設定を作る（必要なときだけ上書き）。 */
    protected void addOptions(ModConfigSpec.Builder builder) {
    }

    protected final ModConfigSpec.IntValue intOption(ModConfigSpec.Builder builder, String optionId,
                                                     String comment, int def, int min, int max) {
        ModConfigSpec.IntValue value = builder.comment(comment).defineInRange(optionId, def, min, max);
        options.add(new FeatureOption(this, optionId, FeatureOption.Type.INT, value, min, max));
        return value;
    }

    protected final ModConfigSpec.DoubleValue doubleOption(ModConfigSpec.Builder builder, String optionId,
                                                           String comment, double def, double min, double max) {
        ModConfigSpec.DoubleValue value = builder.comment(comment).defineInRange(optionId, def, min, max);
        options.add(new FeatureOption(this, optionId, FeatureOption.Type.DOUBLE, value, min, max));
        return value;
    }

    protected final ModConfigSpec.BooleanValue boolOption(ModConfigSpec.Builder builder, String optionId,
                                                          String comment, boolean def) {
        ModConfigSpec.BooleanValue value = builder.comment(comment).define(optionId, def);
        options.add(new FeatureOption(this, optionId, FeatureOption.Type.BOOLEAN, value, 0, 1));
        return value;
    }

    protected final ModConfigSpec.ConfigValue<String> stringOption(ModConfigSpec.Builder builder, String optionId,
                                                                   String comment, String def) {
        ModConfigSpec.ConfigValue<String> value = builder.comment(comment).define(optionId, def);
        options.add(new FeatureOption(this, optionId, FeatureOption.Type.STRING, value, 0, 0));
        return value;
    }

    /** イベントの登録など（必要なときだけ上書き）。 */
    public void register(IEventBus modBus) {
    }

    /** クライアントだけで動く処理の登録（ClientSetup から呼ばれる）。 */
    public void registerClient(IEventBus modBus) {
    }

    public final boolean enabledByDefault() {
        return enabledByDefault;
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
