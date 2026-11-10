package dev.eeeeeeruki.layeredarmor;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> dev.eeeeeeruki.layeredarmor.client.ClientLayers::init);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // スロットに入れられるかの判定（バニラの防具欄と同じ部位かどうか）
        for (EquipmentSlot slot : Layers.ARMOR_SLOTS) {
            CuriosApi.registerCurioPredicate(new ResourceLocation(MODID, Layers.shortName(slot)),
                    result -> Layers.SLOT_IDS.get(slot).equals(result.slotContext().identifier())
                            && Layers.canLayer(result.stack(), slot));
        }
        event.enqueueWork(() -> {
            int count = 0;
            for (Item item : ForgeRegistries.ITEMS) {
                if (item instanceof ICurioItem) continue; // 自前でCurios対応しているアイテムはそのまま
                EquipmentSlot slot;
                try {
                    slot = Layers.defaultArmorSlot(item);
                } catch (Throwable t) {
                    continue;
                }
                if (slot == null) continue;
                CuriosApi.registerCurio(item, ArmorLayerCurio.INSTANCE);
                Layers.LAYERABLE.add(item);
                count++;
            }
            LOGGER.info("[LayeredArmor] {} 個の防具を重ね着スロットに対応させました", count);
        });
    }
}
