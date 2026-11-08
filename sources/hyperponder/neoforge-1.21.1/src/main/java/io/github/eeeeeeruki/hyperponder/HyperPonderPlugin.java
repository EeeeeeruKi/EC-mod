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
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "muteki")).addStoryBoard("muteki", HyperScenes::muteki);
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "novel")).addStoryBoard("novel", HyperScenes::novel);
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "chronicle")).addStoryBoard("chronicle", HyperScenes::chronicle);
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "paradox")).addStoryBoard("paradox", HyperScenes::paradox);
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "vrx")).addStoryBoard("vrx", HyperScenes::vrx);
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "god_sigil"), ResourceLocation.fromNamespaceAndPath("hyperdaimc", "soul"), ResourceLocation.fromNamespaceAndPath("hyperdaimc", "fumetsu_skull"), ResourceLocation.fromNamespaceAndPath("hyperdaimc", "fumetsu")).addStoryBoard("fumetsu_summon", HyperScenes::fumetsu_summon);
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "fumetsu")).addStoryBoard("fumetsu_arena", HyperScenes::fumetsu_defeat);
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "fumetsu")).addStoryBoard("fumetsu_arena", HyperScenes::fumetsu_rage);
        helper.forComponents(ResourceLocation.fromNamespaceAndPath("hyperdaimc", "fumetsu")).addStoryBoard("fumetsu_arena", HyperScenes::fumetsu_immortal);

    }
}
