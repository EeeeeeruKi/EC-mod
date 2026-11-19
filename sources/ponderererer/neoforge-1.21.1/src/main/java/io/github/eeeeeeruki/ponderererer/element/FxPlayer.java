package io.github.eeeeeeruki.ponderererer.element;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;

import java.util.UUID;

/** A client-only stand-in player used for Ponder scenes. Never added to a level. */
public class FxPlayer extends RemotePlayer {
    public FxPlayer(ClientLevel level, UUID id, String name) {
        super(level, new GameProfile(id, name));
        setSilent(true); // no equip sounds when the figure is dressed
    }

    public void stepSwing() {
        oAttackAnim = attackAnim;
        updateSwingTime();
    }

    private static java.lang.reflect.Field fallFlyTicks;
    private static boolean fallFlyLookedUp;

    /**
     * The player renderer tilts the body for elytra flight based on how long the entity has been
     * gliding. That counter is private and only advances in a ticking world, so set it directly.
     */
    public void setFallFlyTicks(int ticks) {
        if (!fallFlyLookedUp) {
            fallFlyLookedUp = true;
            try {
                fallFlyTicks = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("fallFlyTicks");
                fallFlyTicks.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException e) {
                fallFlyTicks = null;
            }
        }
        if (fallFlyTicks == null) return;
        try {
            fallFlyTicks.setInt(this, ticks);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
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
