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
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "muteki"), "muteki", HyperScenes::muteki);
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "novel"), "novel", HyperScenes::novel);
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "chronicle"), "chronicle", HyperScenes::chronicle);
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "paradox"), "paradox", HyperScenes::paradox);
        helper.addStoryBoard(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "vrx"), "vrx", HyperScenes::vrx);

    }
}
