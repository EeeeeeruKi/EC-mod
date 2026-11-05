package com.eeeeeeruki.claudeupdater;

import com.eeeeeeruki.claudeupdater.client.ClientHooks;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu's "config" button opens the update check (only used when Mod Menu is installed). */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClientHooks::openMenu;
    }
}
