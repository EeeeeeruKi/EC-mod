package net.ramune1256.hijikimod.block;

import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ramune1256.hijikimod.HijikiMod;
import net.ramune1256.hijikimod.item.ModItems;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HijikiMod.MOD_ID);

    public static final DeferredBlock<Block> HIJIKI_BLOCK = registerBlock("hijiki_block",
            () -> new Block(BlockBehaviour.Properties.of().sound(SoundType.GRASS)));

    public static final DeferredBlock<Block> YOWAIYO_BLOCK = registerBlock("yowaiyo_block",
            () -> new Block(BlockBehaviour.Properties.of().sound(SoundType.GRASS)));

    public static final DeferredBlock<Block> IRI_BLOCK = registerBlock("iri_block",
            () -> new Block(BlockBehaviour.Properties.of().sound(SoundType.GRASS)));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> registered = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
