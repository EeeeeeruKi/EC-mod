package com.eeeeeeruki.hijikiaddon;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;

/** ツルハシ・斧・シャベル・クワの全部で掘れる道具。 */
public class PaxelItem extends DiggerItem {
    public static final TagKey<Block> MINEABLE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(HijikiAddon.MOD_ID, "mineable/paxel"));

    public PaxelItem(Tier tier, Properties properties) {
        super(tier, MINEABLE, properties);
    }
}
