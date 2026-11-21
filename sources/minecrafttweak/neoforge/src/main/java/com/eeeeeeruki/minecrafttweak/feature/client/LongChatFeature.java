package com.eeeeeeruki.minecrafttweak.feature.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import com.eeeeeeruki.minecrafttweak.network.LongChatNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * チャットの文字数を増やす。チャット欄に256文字より長く書けるようにする。
 *
 * - サーバーにもこのmodがあってこの機能がオン … 長い文を専用の通信でまるごと送る（チャットはサーバーが流し、コマンドはサーバーで実行する）。
 *   バニラのチャットの通信そのものは変えないので、このmodが入っていない人がいても切断されない。
 * - サーバーにこのmodがない … チャットは256文字ずつに分けて1秒ごとに送る。コマンドは分けられないので256文字まで。
 */
public class LongChatFeature extends Feature {
    public static LongChatFeature INSTANCE;
    public ModConfigSpec.IntValue maxLength;

    public LongChatFeature() {
        super("longer_chat", FeatureCategory.CLIENT,
                "Lets you send chat messages and commands longer than 256 characters.");
        INSTANCE = this;
    }

    @Override
    protected void addOptions(ModConfigSpec.Builder builder) {
        maxLength = intOption(builder, "max_length", "Longest chat message or command (characters). The server uses its own value too.", 2048, 256, 8000);
    }

    @Override
    public void register(IEventBus modBus) {
        LongChatNetwork.register(modBus);
    }

    @Override
    public void registerClient(IEventBus modBus) {
        com.eeeeeeruki.minecrafttweak.client.feature.LongChat.register();
    }
}
