package com.eeeeeeruki.minecrafttweak.compat.emi;

import com.eeeeeeruki.minecrafttweak.client.feature.FtbButtons;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;

/** EMI: FTB 系のボタンの場所を「避ける場所」として伝える（EMI は毎フレーム確かめ直す）。 */
@EmiEntrypoint
public class FtbExclusionEmi implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericExclusionArea((screen, consumer) -> {
            for (int[] r : FtbButtons.areas(screen)) {
                consumer.accept(new Bounds(r[0], r[1], r[2], r[3]));
            }
        });
    }
}
