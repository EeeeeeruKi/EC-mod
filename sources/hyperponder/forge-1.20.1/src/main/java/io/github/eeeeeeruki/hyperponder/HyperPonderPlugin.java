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
        helper.forComponents(new ResourceLocation("hyperdaimc", "muteki")).addStoryBoard("muteki", HyperScenes::muteki);
        helper.forComponents(new ResourceLocation("hyperdaimc", "novel")).addStoryBoard("novel", HyperScenes::novel);
        helper.forComponents(new ResourceLocation("hyperdaimc", "chronicle")).addStoryBoard("chronicle", HyperScenes::chronicle);
        helper.forComponents(new ResourceLocation("hyperdaimc", "paradox")).addStoryBoard("paradox", HyperScenes::paradox);
        helper.forComponents(new ResourceLocation("hyperdaimc", "vrx")).addStoryBoard("vrx", HyperScenes::vrx);
        helper.forComponents(new ResourceLocation("hyperdaimc", "god_sigil"), new ResourceLocation("hyperdaimc", "soul"), new ResourceLocation("hyperdaimc", "fumetsu_skull"), new ResourceLocation("hyperdaimc", "fumetsu")).addStoryBoard("fumetsu_summon", HyperScenes::fumetsu_summon);
        helper.forComponents(new ResourceLocation("hyperdaimc", "fumetsu"), new ResourceLocation("hyperdaimc", "novel")).addStoryBoard("fumetsu_arena", HyperScenes::fumetsu_defeat);
        helper.forComponents(new ResourceLocation("hyperdaimc", "fumetsu")).addStoryBoard("fumetsu_arena", HyperScenes::fumetsu_rage);
        helper.forComponents(new ResourceLocation("hyperdaimc", "fumetsu")).addStoryBoard("fumetsu_arena", HyperScenes::fumetsu_immortal);

    }
}
