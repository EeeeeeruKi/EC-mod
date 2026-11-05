package com.eeeeeeruki.compacttoasts;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu の「設定」ボタンから設定画面を開く（Mod Menu が入っているときだけ使われる）。 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ToastsConfigScreen::new;
    }
}
