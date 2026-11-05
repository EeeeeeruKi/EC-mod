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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.network.NetworkConstants;
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
    public static final String MC_VERSION = "1.20.1";
    public static final String LOADER = "forge";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ClaudeUpdater() {
        Log.info = LOGGER::info;
        Log.warn = LOGGER::warn;

        // optional on the other side: clients and servers do not both need it
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        Path mods = FMLPaths.MODSDIR.get().toAbsolutePath().normalize();
        Path self = ModList.get().getModFileById(MODID).getFile().getFilePath();
        Env env = new Env(FMLPaths.GAMEDIR.get(), mods, MC_VERSION, LOADER, self);
        Set<String> loaded = new HashSet<>();
        for (IModInfo info : ModList.get().getMods()) loaded.add(info.getModId());
        UpdaterCore.start(env, installedMods(mods), loaded, FMLEnvironment.dist.isDedicatedServer());

        if (FMLEnvironment.dist == Dist.CLIENT) Client.init();
    }

    private static List<InstalledMod> installedMods(Path mods) {
        List<InstalledMod> out = new ArrayList<>();
        for (IModInfo info : ModList.get().getMods()) {
            try {
                Path jar = info.getOwningFile().getFile().getFilePath().toAbsolutePath().normalize();
                // only jars sitting directly in the mods folder (not Forge itself, not jar-in-jar libraries)
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

        static void init() {
            FMLJavaModLoadingContext.get().getModEventBus()
                    .addListener((RegisterKeyMappingsEvent e) -> {
                        e.register(UPDATE);
                        e.register(SECRET);
                    });
            MinecraftForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Pre e) -> {
                if (ClientHooks.onMenuKey(e.getScreen(), e.getKeyCode(), e.getScanCode(), UPDATE)) e.setCanceled(true);
            });
            ClientHooks.secretHeld = Client::secretHeld;
            UpdateIconButton.ICON = new ResourceLocation(MODID, "textures/gui/update.png");
            MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
                if (e.getScreen() instanceof TitleScreen) {
                    UpdateIconButton.addTo(e.getScreen(), e.getListenersList(), e::addListener);
                }
            });
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
                if (e.phase == TickEvent.Phase.END) ClientHooks.onClientTick();
            });
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ClientHooks.openMenu(parent)));
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
