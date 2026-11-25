package com.eeeeeeruki.minecrafttweak.compat.jei;

import com.eeeeeeruki.minecrafttweak.client.feature.FtbButtons;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** JEI: FTB 系のボタンの場所を「ほかに使っている場所」として伝える。 */
@JeiPlugin
public class FtbExclusionJei implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("minecrafttweak", "ftb_exclusion");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                List<Rect2i> out = new ArrayList<>();
                for (int[] r : FtbButtons.areas(Minecraft.getInstance().screen)) {
                    out.add(new Rect2i(r[0], r[1], r[2], r[3]));
                }
                return out;
            }
        });
    }
}
