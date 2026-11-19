package com.eeeeeeruki.minecrafttweak.compat.emi;

import com.eeeeeeruki.minecrafttweak.client.feature.EnglishNames;
import com.eeeeeeruki.minecrafttweak.client.feature.EnglishSearch;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** EMI: 英語の名前を「別名」として登録して、英語でも検索できるようにする。 */
@EmiEntrypoint
public class EnglishSearchEmi implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        if (!EnglishSearch.active()) {
            return;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            String en = EnglishNames.of(new ItemStack(item));
            if (en != null) {
                registry.addAlias(EmiStack.of(item), Component.literal(en));
            }
        }
    }
}
