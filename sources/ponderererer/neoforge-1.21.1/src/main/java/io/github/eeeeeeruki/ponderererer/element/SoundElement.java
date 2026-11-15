package io.github.eeeeeeruki.ponderererer.element;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundEvent;

/**
 * Plays a sound once. It only plays when actually drawn shortly after being created,
 * so scrubbing / skipping through a scene does not spam sounds.
 */
public class SoundElement extends FxElement {
    private final SoundEvent sound;
    private final float volume, pitch;
    private boolean played;

    public SoundElement(SoundEvent sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.alpha = this.prevAlpha = 1f;
    }

    @Override
    protected void update(net.createmod.ponder.foundation.PonderScene scene) {
        if (age > 40) setVisible(false);
    }

    @Override
    protected void draw(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt, float alpha) {
        if (played) return;
        played = true;
        if (age > 3) return; // we were skipped past, stay quiet
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
