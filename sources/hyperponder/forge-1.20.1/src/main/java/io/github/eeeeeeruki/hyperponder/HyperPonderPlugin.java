package io.github.eeeeeeruki.hyperponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class HyperPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return "hyperponder";
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.addStoryBoard(new ResourceLocation("hyperdaimc", "muteki"), "muteki", HyperScenes::muteki);
        helper.addStoryBoard(new ResourceLocation("hyperdaimc", "novel"), "novel", HyperScenes::novel);
        helper.addStoryBoard(new ResourceLocation("hyperdaimc", "chronicle"), "chronicle", HyperScenes::chronicle);
        helper.addStoryBoard(new ResourceLocation("hyperdaimc", "paradox"), "paradox", HyperScenes::paradox);
        helper.addStoryBoard(new ResourceLocation("hyperdaimc", "vrx"), "vrx", HyperScenes::vrx);

    }
}
