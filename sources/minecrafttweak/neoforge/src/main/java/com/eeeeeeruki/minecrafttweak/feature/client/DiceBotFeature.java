package com.eeeeeeruki.minecrafttweak.feature.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import com.eeeeeeruki.minecrafttweak.server.DiceBot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * BCDice（ダイスボット）。チャットにダイスのコマンド（例: 2D6, CCB<=50）を書くと、BCDice-API で振った結果をチャットに流す。
 * 「rccb」は、成功値を決めるロールと判定ロールをまとめて振るこのmodだけのコマンド。サーバーで動く。
 */
public class DiceBotFeature extends Feature {
    public static DiceBotFeature INSTANCE;
    public ModConfigSpec.ConfigValue<String> gameSystem;
    public ModConfigSpec.ConfigValue<String> apiUrl;

    public DiceBotFeature() {
        super("dice_bot", FeatureCategory.CLIENT,
                "BCDice dice bot: dice commands in chat (2D6, CCB<=50, ...) are rolled by the BCDice-API and the results are shown in chat. 'rccb' rolls a success value and then a CCB check.");
        INSTANCE = this;
    }

    @Override
    protected void addOptions(ModConfigSpec.Builder builder) {
        gameSystem = stringOption(builder, "game_system", "BCDice game system ID (Cthulhu, Cthulhu7th, SwordWorld2.5, DiceBot ...).", "Cthulhu");
        apiUrl = stringOption(builder, "api_url", "BCDice-API server URL.", "https://bcdice.onlinesession.app");
    }

    @Override
    public void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ServerChatEvent e) -> DiceBot.onChat(e.getPlayer(), e.getRawText()));
    }
}
