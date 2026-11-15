package io.github.eeeeeeruki.ponderererer.element;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;

import java.util.UUID;

/** A client-only stand-in player used for Ponder scenes. Never added to a level. */
public class FxPlayer extends RemotePlayer {
    public FxPlayer(ClientLevel level, UUID id, String name) {
        super(level, new GameProfile(id, name));
    }

    public void stepSwing() {
        oAttackAnim = attackAnim;
        updateSwingTime();
    }

    public void setUsing(boolean using) {
        setLivingEntityFlag(1, using);
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
