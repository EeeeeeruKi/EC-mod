package com.eeeeeeruki.picpop.client;

import com.eeeeeeruki.picpop.Align;
import com.eeeeeeruki.picpop.ImageArgs;
import com.eeeeeeruki.picpop.PicPopConfig;
import com.eeeeeeruki.picpop.network.ShowImagePacket;

public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handle(ShowImagePacket p) {
        if (p.clear()) {
            ImageManager.clearAll();
            return;
        }
        if (!PicPopConfig.ALLOW_SERVER_IMAGES.get() || !ImageArgs.isHttpUrl(p.url())) return;
        ImageManager.show(new ImageArgs(p.url(), Align.h(p.h()), Align.v(p.v()), p.seconds(),
                p.size() > 100 ? 100 : p.size()));
    }
}
