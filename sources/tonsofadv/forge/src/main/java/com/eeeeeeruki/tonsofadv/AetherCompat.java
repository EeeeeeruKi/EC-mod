package com.eeeeeeruki.tonsofadv;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Constructor;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * エーテルの「進捗の音の上書き」（aether:advancement_sound_override）に、このMODのエーテルのタブの進捗を登録します。
 * エーテルはトーストが画面に出たときにこの音を鳴らすので、表示と音がそろいます。
 * エーテルのクラスには直接つながず、リフレクションで作ります（エーテルが無くてもこのMODは動きます）。
 */
final class AetherCompat {
    private static final String[] SOUNDS = {
            "ui.toast.aether_general", "ui.toast.aether_bronze", "ui.toast.aether_silver", "ui.toast.aether_gold"};

    private AetherCompat() {
    }

    static void register(IEventBus modBus) {
        try {
            Class<?> overrideClass = Class.forName("com.aetherteam.aether.api.registers.AdvancementSoundOverride");
            Constructor<?> ctor = overrideClass.getConstructor(int.class, Predicate.class, Supplier.class);
            ResourceKey<Registry<Object>> key = ResourceKey.createRegistryKey(new ResourceLocation("aether", "advancement_sound_override"));
            DeferredRegister<Object> register = DeferredRegister.create(key, TonsOfAdv.MOD_ID);
            for (String sound : SOUNDS) {
                Predicate<Object> matches = advancement -> {
                    if (!Config.specialSounds()) return false;
                    ResourceLocation id = ((Advancement) advancement).getId();
                    return sound.equals(AetherSounds.pick(id.getNamespace(), id.getPath()));
                };
                Supplier<SoundEvent> soundEvent = () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("aether", sound));
                register.register(sound.substring("ui.toast.".length()), () -> {
                    try {
                        return ctor.newInstance(20, matches, soundEvent);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
            register.register(modBus);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LogUtils.getLogger().warn("[tonsofadv] エーテルの進捗の音を登録できませんでした: {}", e.toString());
        }
    }
}
