package com.eeeeeeruki.claudeupdater;

import com.eeeeeeruki.claudeupdater.client.ClientHooks;
import com.eeeeeeruki.claudeupdater.client.UpdateIconButton;
import com.eeeeeeruki.claudeupdater.core.Env;
import com.eeeeeeruki.claudeupdater.core.InstalledMod;
import com.eeeeeeruki.claudeupdater.core.Log;
import com.eeeeeeruki.claudeupdater.core.UpdaterCore;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.IModInfo;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod(ClaudeUpdater.MODID)
public class ClaudeUpdater {
    public static final String MODID = "claudeupdater";
    public static final String MC_VERSION = "1.21.1";
    public static final String LOADER = "neoforge";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ClaudeUpdater(IEventBus modBus, ModContainer container) {
        Log.info = LOGGER::info;
        Log.warn = LOGGER::warn;

        Path mods = FMLPaths.MODSDIR.get().toAbsolutePath().normalize();
        Path self = container.getModInfo().getOwningFile().getFile().getFilePath();
        Env env = new Env(FMLPaths.GAMEDIR.get(), mods, MC_VERSION, LOADER, self);
        Set<String> loaded = new HashSet<>();
        for (IModInfo info : ModList.get().getMods()) loaded.add(info.getModId());
        UpdaterCore.start(env, installedMods(mods), loaded, FMLEnvironment.dist.isDedicatedServer());

        if (FMLEnvironment.dist == Dist.CLIENT) Client.init(modBus, container);
    }

    private static List<InstalledMod> installedMods(Path mods) {
        List<InstalledMod> out = new ArrayList<>();
        for (IModInfo info : ModList.get().getMods()) {
            try {
                Path jar = info.getOwningFile().getFile().getFilePath().toAbsolutePath().normalize();
                // only jars sitting directly in the mods folder (not NeoForge itself, not jar-in-jar libraries)
                if (!Files.isRegularFile(jar) || !mods.equals(jar.getParent())) continue;
                out.add(new InstalledMod(info.getModId(), info.getDisplayName(), info.getVersion().toString(), jar));
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    /** Client-only registration, kept in its own class so the server never loads screen classes. */
    private static final class Client {
        /** Hold this while opening the update menu to get the install screen. Shown as ??? in Controls. */
        static final KeyMapping SECRET = new KeyMapping("???", KeyConflictContext.GUI, KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, "key.categories.misc");
        /** Opens the update check from any menu. Ctrl+U by default; with the ??? key held it opens the install screen. */
        static final KeyMapping UPDATE = new KeyMapping("MODの更新を確認", KeyConflictContext.GUI, KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, "key.categories.misc");

        static void init(IEventBus modBus, ModContainer container) {
            modBus.addListener((RegisterKeyMappingsEvent e) -> {
                e.register(UPDATE);
                e.register(SECRET);
            });
            NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Pre e) -> {
                if (ClientHooks.onMenuKey(e.getScreen(), e.getKeyCode(), e.getScanCode(), UPDATE)) e.setCanceled(true);
            });
            ClientHooks.secretHeld = Client::secretHeld;
            UpdateIconButton.ICON = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/update.png");
            NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
                if (e.getScreen() instanceof TitleScreen) {
                    UpdateIconButton.addTo(e.getScreen(), e.getListenersList(), e::addListener);
                }
            });
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientHooks.onClientTick());
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (IConfigScreenFactory) (c, parent) -> ClientHooks.openMenu(parent));
        }

        /** Screens swallow key events, so the key's own state is not updated on the title screen: read GLFW. */
        static boolean secretHeld() {
            InputConstants.Key key = SECRET.getKey();
            if (key.equals(InputConstants.UNKNOWN)) return false;
            long w = Minecraft.getInstance().getWindow().getWindow();
            int code = key.getValue();
            boolean down;
            if (key.getType() == InputConstants.Type.KEYSYM) {
                down = InputConstants.isKeyDown(w, code);
                if (code == GLFW.GLFW_KEY_LEFT_SHIFT) down |= InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_SHIFT);
                if (code == GLFW.GLFW_KEY_LEFT_CONTROL) down |= InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_CONTROL);
            } else if (key.getType() == InputConstants.Type.MOUSE) {
                down = GLFW.glfwGetMouseButton(w, code) == GLFW.GLFW_PRESS;
            } else {
                down = false;
            }
            return down && SECRET.getKeyModifier().isActive(KeyConflictContext.GUI);
        }
    }
}
