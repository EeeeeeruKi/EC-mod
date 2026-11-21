package net.ramune1256.hijikimod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ramune1256.hijikimod.HijikiMod;
import net.ramune1256.hijikimod.block.ModBlocks;

public class ModCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HijikiMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HIJIKI_TAB =
            CREATIVE_MODE_TABS.register("hijiki_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.HIJIKI.get()))
                    .title(Component.translatable("creativetab.hijiki_tab"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.HIJIKI.get());
                        output.accept(ModBlocks.HIJIKI_BLOCK.get());
                        output.accept(ModItems.YOWAIYO.get());
                        output.accept(ModBlocks.YOWAIYO_BLOCK.get());
                        output.accept(ModItems.IRI.get());
                        output.accept(ModBlocks.IRI_BLOCK.get());
                        output.accept(ModItems.HIJI_WITHER_SPAWN_EGG.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}
