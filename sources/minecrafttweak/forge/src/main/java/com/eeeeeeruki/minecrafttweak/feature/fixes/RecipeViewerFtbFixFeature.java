package com.eeeeeeruki.minecrafttweak.feature.fixes;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;

/**
 * EMI・JEI と FTB のボタンが重ならないようにする（最初からオン）。
 * FTB Library などが画面の端に出すボタン（クエスト・チーム・地図など）の今の場所を、EMI・JEI に「ここは避けて」と毎回伝える。
 * 中身は compat/emi/FtbExclusionEmi・compat/jei/FtbExclusionJei・client/feature/FtbButtons。
 */
public class RecipeViewerFtbFixFeature extends Feature {
    public static RecipeViewerFtbFixFeature INSTANCE;

    public RecipeViewerFtbFixFeature() {
        super("recipe_viewer_ftb_fix", FeatureCategory.FIXES, true,
                "Keeps EMI and JEI panels away from the FTB sidebar buttons.");
        INSTANCE = this;
    }

    public static boolean active() {
        return INSTANCE == null || INSTANCE.isEnabled();
    }
}
