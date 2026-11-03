package com.eeeeeeruki.tonsofadv;

/** エーテルの進捗の音（aether:ui.toast.*）の選び方。エーテル本体と同じく、ダンジョンのボスは銅・銀・金の音です。 */
final class AetherSounds {
    private AetherSounds() {
    }

    /** @return 鳴らす音のパス（aether名前空間）。エーテルのタブの進捗でなければ null */
    static String pick(String namespace, String path) {
        if (!TonsOfAdv.MOD_ID.equals(namespace) || !path.startsWith("mod_aether") || path.endsWith("/_root")) return null;
        if (path.endsWith("/kill_slider")) return "ui.toast.aether_bronze";
        if (path.endsWith("/kill_valkyrie_queen")) return "ui.toast.aether_silver";
        if (path.endsWith("/kill_sun_spirit")) return "ui.toast.aether_gold";
        return "ui.toast.aether_general";
    }
}
