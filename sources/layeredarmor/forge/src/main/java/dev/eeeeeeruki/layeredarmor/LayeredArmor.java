package dev.eeeeeeruki.layeredarmor;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

@Mod(LayeredArmor.MODID)
public class LayeredArmor {
    public static final String MODID = "layeredarmor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LayeredArmor() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadComplete);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> dev.eeeeeeruki.layeredarmor.client.ClientLayers::init);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // スロットに入れられるかの判定（バニラの防具欄と同じ部位かどうか）
        // Curios の登録先は普通の HashMap なので、他MODと同時に書き込まないようメインスレッドで登録する
        event.enqueueWork(() -> {
            for (EquipmentSlot slot : Layers.ARMOR_SLOTS) {
                CuriosApi.registerCurioPredicate(new ResourceLocation(MODID, Layers.shortName(slot)),
                        result -> {
                            try {
                                return Layers.SLOT_IDS.get(slot).equals(result.slotContext().identifier())
                                        && Layers.canLayer(result.stack(), slot);
                            } catch (Throwable t) {
                                return false;
                            }
                        });
            }
        });
    }

    /** 他のMODの初期化がすべて終わってから、防具を重ね着スロットに対応させる（先にItemStackを大量に作って他MODの初期化を邪魔しないため） */
    private void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            int count = 0;
            int skipped = 0;
            for (Item item : ForgeRegistries.ITEMS) {
                if (item instanceof ICurioItem) continue; // 自前でCurios対応しているアイテムはそのまま
                EquipmentSlot slot;
                try {
                    ItemStack probe = new ItemStack(item);
                    slot = LivingEntity.getEquipmentSlotForItem(probe);
                    if (!slot.isArmor()) continue;
                    // すでにCuriosとして登録されている（他MODが registerCurio したか、capability を付けている）アイテムは触らない。
                    // 二重に登録すると "Duplicate Capability Key" などで落ちたり、他MODの設定を上書きしてしまうため。
                    if (CuriosApi.getCurio(probe).isPresent()) {
                        skipped++;
                        continue;
                    }
                } catch (Throwable t) {
                    continue;
                }
                try {
                    CuriosApi.registerCurio(item, ArmorLayerCurio.INSTANCE);
                    Layers.LAYERABLE.add(item);
                    count++;
                } catch (Throwable t) {
                    LOGGER.warn("[LayeredArmor] {} を重ね着スロットに対応させられませんでした", item, t);
                }
            }
            if (skipped > 0) LOGGER.info("[LayeredArmor] すでに Curios 対応済みの防具 {} 個は、そのままにしました", skipped);
            LOGGER.info("[LayeredArmor] {} 個の防具を重ね着スロットに対応させました", count);
        });
    }
}
