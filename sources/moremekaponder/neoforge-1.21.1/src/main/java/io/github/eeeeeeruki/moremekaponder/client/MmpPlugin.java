package io.github.eeeeeeruki.moremekaponder.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.eeeeeeruki.moremekaponder.MoreMekaPonder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads scenes.json (made from the editor) and registers one scene per entry. */
public final class MmpPlugin implements PonderPlugin {
    private static final Logger LOG = LoggerFactory.getLogger("More MekaPonder");

    @Override
    public String getModId() {
        return MoreMekaPonder.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        JsonArray scenes;
        try (InputStream in = MmpPlugin.class.getResourceAsStream("/assets/moremekaponder/scenes.json")) {
            if (in == null) {
                LOG.error("scenes.json is missing");
                return;
            }
            scenes = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
        } catch (Exception e) {
            LOG.error("Could not read scenes.json", e);
            return;
        }
        int count = 0;
        for (JsonElement el : scenes) {
            JsonObject scene = el.getAsJsonObject();
            List<ResourceLocation> items = new ArrayList<>();
            for (JsonElement id : scene.getAsJsonArray("items")) {
                ResourceLocation rl = ResourceLocation.tryParse(id.getAsString());
                if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) items.add(rl);
            }
            if (items.isEmpty()) continue; // addon not installed
            helper.forComponents(items).addStoryBoard("base", (builder, util) -> SceneRunner.run(scene, builder, util));
            count++;
        }
        LOG.info("Registered {} Mekanism ponder scenes", count);
    }
}
