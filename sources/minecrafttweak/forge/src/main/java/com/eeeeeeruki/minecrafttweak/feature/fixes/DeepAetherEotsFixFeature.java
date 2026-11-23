package com.eeeeeeruki.minecrafttweak.feature.fixes;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;

/**
 * Deep Aether のボス「EOTS」のクラッシュ修正（最初からオン）。
 * 攻撃の途中で狙う相手がいなくなると、いない相手を動かそうとしてサーバーが止まるのをよける。
 * 中身は mixin/compat/DeepAetherEotsGoalMixin。
 * （Deep Aether は 1.21.1 にしかないので、Forge 1.20.1 版では何もしない。番号をそろえるためにある）
 */
public class DeepAetherEotsFixFeature extends Feature {
    public static DeepAetherEotsFixFeature INSTANCE;

    public DeepAetherEotsFixFeature() {
        super("deep_aether_eots_fix", FeatureCategory.FIXES, true,
                "Fixes a server crash in Deep Aether when the EOTS boss loses its target in the middle of an attack.");
        INSTANCE = this;
    }

    public static boolean active() {
        return INSTANCE == null || INSTANCE.isEnabled();
    }
}
