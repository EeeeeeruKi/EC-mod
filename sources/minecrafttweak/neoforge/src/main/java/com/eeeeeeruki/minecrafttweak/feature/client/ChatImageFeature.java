package com.eeeeeeruki.minecrafttweak.feature.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import com.eeeeeeruki.minecrafttweak.network.ChatImageNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * チャットの画像。チャット欄のいちばん左に「＋」ボタンを足して、画像ファイルかリンクを添付して送れる。
 * 画像（GIFも）はチャットの中に表示する。
 *
 * - リンク: ふつうのチャットとして送る。このmodを入れている人の画面で、リンク先の画像を読み込んで表示する。
 * - ファイル: サーバー（このmodが入っていて、この機能がオン）に送り、サーバーが「[画像]」付きのメッセージを全員に流す。
 *   画像そのものは、見る人が必要になったときにサーバーからもらう。
 */
public class ChatImageFeature extends Feature {
    public static ChatImageFeature INSTANCE;
    public ModConfigSpec.IntValue maxHeight;
    public ModConfigSpec.IntValue maxUploadKb;
    public ModConfigSpec.BooleanValue loadLinks;

    public ChatImageFeature() {
        super("chat_images", FeatureCategory.CLIENT,
                "Shows images and GIFs in chat. A + button on the left of the chat box attaches an image file or link.");
        INSTANCE = this;
    }

    @Override
    protected void addOptions(ModConfigSpec.Builder builder) {
        maxHeight = intOption(builder, "max_height", "Largest height of an image in chat (GUI pixels).", 60, 20, 240);
        maxUploadKb = intOption(builder, "max_upload_kb", "Largest image file that can be sent (KB). The server uses its own value too.", 1024, 64, 4096);
        loadLinks = boolOption(builder, "load_links", "Load images from links in chat (.png .jpg .jpeg .gif .bmp).", true);
    }

    @Override
    public void register(IEventBus modBus) {
        ChatImageNetwork.register(modBus);
    }

    @Override
    public void registerClient(IEventBus modBus) {
        com.eeeeeeruki.minecrafttweak.client.feature.ChatImages.register();
    }
}
