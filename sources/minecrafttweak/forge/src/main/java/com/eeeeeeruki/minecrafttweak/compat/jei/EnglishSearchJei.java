package com.eeeeeeruki.minecrafttweak.compat.jei;

import com.eeeeeeruki.minecrafttweak.client.feature.EnglishNames;
import com.eeeeeeruki.minecrafttweak.client.feature.EnglishSearch;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** JEI: 英語の名前を「別名」として登録して、英語でも検索できるようにする。 */
@JeiPlugin
public class EnglishSearchJei implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("minecrafttweak", "english_search");
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        if (!EnglishSearch.active()) {
            return;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            String en = EnglishNames.of(new ItemStack(item));
            if (en != null) {
                registration.addAlias(item, en);
            }
        }
    }
}
