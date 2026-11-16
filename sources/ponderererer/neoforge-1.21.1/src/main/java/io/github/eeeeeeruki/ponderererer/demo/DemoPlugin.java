package io.github.eeeeeeruki.ponderererer.demo;

import io.github.eeeeeeruki.ponderererer.FxConfig;
import io.github.eeeeeeruki.ponderererer.Ponderererer;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** Registers the demo scenes on the Lightning Rod when enabled in the client config. */
public final class DemoPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return Ponderererer.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        boolean enabled;
        try {
            enabled = FxConfig.DEMO_SCENES.get();
        } catch (IllegalStateException notLoaded) {
            enabled = readFromFile();
        }
        if (!enabled) enabled = readFromFile();
        if (!enabled) return;
        helper.forComponents(BuiltInRegistries.ITEM.getKey(Items.LIGHTNING_ROD))
                .addStoryBoard("demo", DemoScenes::beamsAndArcs)
                .addStoryBoard("demo", DemoScenes::gaugesAndPanels)
                .addStoryBoard("demo", DemoScenes::players);
    }

    /** The scenes are registered before NeoForge loads client configs, so look at the file itself. */
    private static boolean readFromFile() {
        try {
            java.nio.file.Path file = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve(Ponderererer.MOD_ID + "-client.toml");
            if (!java.nio.file.Files.exists(file)) return false;
            return java.util.regex.Pattern.compile("(?m)^\\s*demoScenes\\s*=\\s*true").matcher(java.nio.file.Files.readString(file)).find();
        } catch (Exception e) {
            return false;
        }
    }
}
