# -*- coding: utf-8 -*-
"""Tons of Advancements generator.
usage: python3 -I gen.py <registry-dir> <out-dir>
Writes:
  <out>/v1201/data/tonsofadv/advancements/...   (1.20.1 format)
  <out>/v1211/data/tonsofadv/advancement/...    (1.21.1 format)
  <out>/lang/ja_jp.json, en_us.json
  <out>/stats.json
"""
import json, os, re, sys

REG, OUT = sys.argv[1], sys.argv[2]
NS = "tonsofadv"
LANG_JA, LANG_EN = {}, {}

GODAN_SHI = ("倒し", "壊し", "治し", "渡し", "らし", "たし", "とし", "こし", "ごし", "話し", "出し", "かし", "減らし", "回し")
ICHIDAN_I = ("尽き", "延び", "落ち", "生き", "てみ", "でみ")
GODAN = {"き": "こ", "ぎ": "ご", "り": "ろ", "み": "も", "び": "ぼ", "ち": "と", "い": "お"}


def soften(text):
    """ゲーム内の文章を柔らかくします（〜しましょう → 〜しよう、〜しています → 〜してるよ）"""
    def vol(m):
        stem = m.group(1)
        if stem.endswith("し"):
            return stem[:-1] + "そう" if stem.endswith(GODAN_SHI) else stem + "よう"
        if stem[-1] in GODAN and not stem.endswith(ICHIDAN_I):
            return stem[:-1] + GODAN[stem[-1]] + "う"
        return stem + "よう"
    text = re.sub(r"(\S{1,6}?)ましょう", vol, text)
    return (text.replace("詰まっています", "詰まってるよ").replace("（製錬も含みます）", "（製錬も入るよ）"))


def L(key, ja, en):
    ja = soften(ja)
    LANG_JA[key] = ja
    LANG_EN[key] = en
    return key

def tr(key, *args):
    c = {"translate": key, "fallback": LANG_JA[key]}
    if args:
        c["with"] = [a if isinstance(a, dict) else {"text": str(a)} for a in args]
    return c

def vtr(key):
    return {"translate": key}

# ---------------- text formats ----------------
L(f"{NS}.obtain.desc", "%sを手に入れましょう", "Obtain %s")
L(f"{NS}.obtain.title2", "%s（%s）", "%s (%s)")
L(f"{NS}.obtain.many.desc", "%sを%s個同時に持ちましょう", "Have %2$s %1$s at once")
L(f"{NS}.eat.title", "いただきます：%s", "Bon Appetit: %s")
L(f"{NS}.eat.desc", "%sを食べましょう", "Eat %s")
L(f"{NS}.kill.title", "%s討伐", "%s Slayer")
L(f"{NS}.kill.desc", "%sを倒しましょう", "Defeat %s")
L(f"{NS}.killn.title", "%s討伐 ×%s", "%s Slayer ×%s")
L(f"{NS}.killn.desc", "%sを%s体倒しましょう", "Defeat %2$s %1$s")
L(f"{NS}.breed.title", "%sの赤ちゃん", "Baby %s")
L(f"{NS}.breed.desc", "%sを繁殖させましょう", "Breed %s")
L(f"{NS}.tame.title", "%sとなかよし", "Friends with %s")
L(f"{NS}.tame.desc", "%sを手なずけましょう", "Tame %s")
L(f"{NS}.biome.desc", "%sを訪れましょう", "Visit %s")
L(f"{NS}.struct.desc", "%sを見つけましょう", "Find %s")
L(f"{NS}.mine.title", "%s採掘 ×%s", "%s Miner ×%s")
L(f"{NS}.mine.desc", "%sを%s個掘りましょう", "Mine %2$s %1$s")
L(f"{NS}.summon.title", "%s、誕生", "Birth of %s")
L(f"{NS}.summon.desc", "%sを作り出しましょう", "Create %s")

# ---------------- data ----------------
def load(v):
    return json.load(open(os.path.join(REG, f"reg-{v}.json")))

COLORS = ["light_blue", "light_gray", "white", "orange", "magenta", "yellow", "lime", "pink",
          "gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
WOODS = ["dark_oak", "oak", "spruce", "birch", "jungle", "acacia", "mangrove", "cherry", "bamboo", "crimson", "warped"]
EXCLUDE = {"air", "bedrock", "barrier", "light", "structure_block", "structure_void", "jigsaw", "command_block",
           "chain_command_block", "repeating_command_block", "command_block_minecart", "debug_stick", "knowledge_book",
           "spawner", "trial_spawner", "vault", "end_portal_frame", "budding_amethyst", "reinforced_deepslate",
           "petrified_oak_slab", "farmland", "dirt_path", "chorus_plant", "frogspawn", "player_head", "bundle",
           "suspicious_sand", "suspicious_gravel", "potion", "splash_potion", "lingering_potion", "tipped_arrow"}
FOODS = ["apple", "golden_apple", "enchanted_golden_apple", "melon_slice", "sweet_berries", "glow_berries",
         "chorus_fruit", "carrot", "golden_carrot", "potato", "baked_potato", "poisonous_potato", "beetroot",
         "beetroot_soup", "dried_kelp", "beef", "cooked_beef", "porkchop", "cooked_porkchop", "mutton",
         "cooked_mutton", "chicken", "cooked_chicken", "rabbit", "cooked_rabbit", "cod", "cooked_cod", "salmon",
         "cooked_salmon", "tropical_fish", "pufferfish", "bread", "cookie", "pumpkin_pie", "mushroom_stew",
         "rabbit_stew", "suspicious_stew", "rotten_flesh", "spider_eye", "honey_bottle", "milk_bucket"]
RARE_CHALLENGE = {"dragon_egg", "nether_star", "netherite_block", "beacon", "enchanted_golden_apple", "elytra",
                  "heavy_core", "mace", "netherite_sword", "netherite_pickaxe", "netherite_axe", "netherite_shovel",
                  "netherite_hoe", "netherite_helmet", "netherite_chestplate", "netherite_leggings", "netherite_boots",
                  "dragon_head", "sniffer_egg", "heart_of_the_sea", "conduit", "totem_of_undying", "trident",
                  "music_disc_pigstep", "music_disc_otherside", "music_disc_5", "music_disc_relic",
                  "music_disc_creator", "music_disc_precipice", "music_disc_creator_music_box", "echo_shard",
                  "recovery_compass", "wither_skeleton_skull", "ominous_trial_key"}
RARE_GOAL = {"diamond", "emerald", "netherite_ingot", "netherite_scrap", "ancient_debris", "ender_eye", "end_crystal",
             "shulker_shell", "shulker_box", "enchanted_book", "experience_bottle", "golden_apple", "blaze_rod",
             "ghast_tear", "nautilus_shell", "phantom_membrane", "goat_horn", "disc_fragment_5", "breeze_rod",
             "trial_key", "sponge", "wet_sponge", "dragon_breath", "ender_chest", "diamond_block", "emerald_block",
             "skeleton_skull", "zombie_head", "creeper_head", "piglin_head", "turtle_helmet", "armadillo_scute",
             "lodestone", "respawn_anchor", "crying_obsidian", "sculk_shrieker", "sculk_catalyst", "calibrated_sculk_sensor"}
EQUIP_SUFFIX = ["_sword", "_pickaxe", "_axe", "_shovel", "_hoe", "_helmet", "_chestplate", "_leggings", "_boots", "_horse_armor"]
EQUIP_MISC = {"bow", "crossbow", "trident", "mace", "shield", "fishing_rod", "elytra", "turtle_helmet", "wolf_armor",
              "arrow", "spectral_arrow", "carrot_on_a_stick", "warped_fungus_on_a_stick", "flint_and_steel", "shears",
              "brush", "spyglass", "compass", "recovery_compass", "clock", "lead", "saddle", "name_tag", "totem_of_undying",
              "wind_charge", "fire_charge", "snowball", "egg", "ender_pearl", "trial_key", "ominous_trial_key"}
MINERALS = ["netherite", "ancient_debris", "diamond", "emerald", "lapis", "redstone", "gold", "iron", "copper", "coal",
            "charcoal", "quartz", "amethyst"]
REDSTONE_KW = ["redstone", "repeater", "comparator", "piston", "observer", "hopper", "dropper", "dispenser", "lever",
               "button", "pressure_plate", "rail", "minecart", "tnt", "daylight_detector", "target", "sculk_sensor",
               "note_block", "tripwire_hook", "lamp", "crafter", "trapped_chest", "lightning_rod", "slime_block",
               "honey_block", "door", "trapdoor", "fence_gate", "copper_bulb"]
STONE_KW = ["stone", "cobblestone", "brick", "bricks", "deepslate", "tuff", "granite", "diorite", "andesite",
            "sandstone", "prismarine", "blackstone", "basalt", "purpur", "mud", "calcite", "dripstone", "terracotta",
            "quartz", "end", "obsidian", "concrete", "glass", "glazed"]
NATURE_KW = ["sapling", "leaves", "flower", "grass", "fern", "seeds", "mushroom", "coral", "kelp", "vine", "vines",
             "moss", "dirt", "sand", "gravel", "clay", "snow", "ice", "cactus", "sugar_cane", "lily", "azalea",
             "dripleaf", "spore_blossom", "sea_pickle", "seagrass", "sponge", "pumpkin", "melon", "hay", "honeycomb",
             "bee_nest", "beehive", "dandelion", "poppy", "orchid", "allium", "bluet", "tulip", "daisy", "cornflower",
             "lily_of_the_valley", "wither_rose", "sunflower", "lilac", "rose_bush", "peony", "torchflower",
             "pitcher", "podzol", "mycelium", "roots", "fungus", "nylium", "wart", "propagule", "pink_petals",
             "cocoa_beans", "wheat", "bone_meal", "hanging_roots", "rooted_dirt", "lichen", "egg", "frog", "sculk",
             "shroomlight", "weeping", "twisting", "chorus_flower", "dead_bush", "bamboo", "turtle_egg", "sniffer_egg",
             "small_dripleaf", "big_dripleaf", "glow_berries", "sweet_berries"]

TABS = {  # key: (ja, en, icon, background)
    "food": ("食べ物", "Food", "cooked_beef", "minecraft:textures/block/hay_block_side.png"),
    "equip": ("装備・道具", "Gear & Tools", "iron_sword", "minecraft:textures/block/smithing_table_top.png"),
    "minerals": ("鉱石・鉱物", "Ores & Minerals", "diamond", "minecraft:textures/block/deepslate.png"),
    "colors": ("カラフル", "Colorful", "red_wool", "minecraft:textures/block/white_wool.png"),
    "woods": ("木材", "Woods", "oak_log", "minecraft:textures/block/oak_planks.png"),
    "redstone": ("レッドストーン", "Redstone", "redstone", "minecraft:textures/block/redstone_block.png"),
    "stone": ("石材・建材", "Building Blocks", "stone_bricks", "minecraft:textures/block/stone_bricks.png"),
    "nature": ("自然", "Nature", "oak_sapling", "minecraft:textures/block/moss_block.png"),
    "misc": ("いろいろ", "Miscellaneous", "chest", "minecraft:textures/block/barrel_side.png"),
    "eat": ("グルメ", "Gourmet", "cake", "minecraft:textures/block/cake_top.png"),
    "kill": ("討伐", "Hunting", "diamond_sword", "minecraft:textures/block/netherrack.png"),
    "killn": ("討伐やり込み", "Hunting Mastery", "netherite_sword", "minecraft:textures/block/crimson_nylium.png"),
    "animals": ("動物となかよし", "Animal Friends", "wheat", "minecraft:textures/block/grass_block_top.png"),
    "explore": ("探検", "Exploration", "filled_map", "minecraft:textures/block/sand.png"),
    "structures": ("構造物めぐり", "Structure Tour", "compass", "minecraft:textures/block/mossy_cobblestone.png"),
    "mining": ("採掘やり込み", "Mining Mastery", "diamond_pickaxe", "minecraft:textures/block/stone.png"),
    "stats": ("やり込み", "Dedication", "clock", "minecraft:textures/block/bookshelf.png"),
    "fun": ("ネタ", "Just for Fun", "carved_pumpkin", "minecraft:textures/block/pumpkin_side.png"),
}

STRUCT_NAMES = {
    "ancient_city": ("古代都市", "Ancient City"), "bastion_remnant": ("砦の遺跡", "Bastion Remnant"),
    "buried_treasure": ("埋もれた宝", "Buried Treasure"), "desert_pyramid": ("砂漠のピラミッド", "Desert Pyramid"),
    "end_city": ("エンドシティ", "End City"), "fortress": ("ネザー要塞", "Nether Fortress"),
    "igloo": ("イグルー", "Igloo"), "jungle_pyramid": ("ジャングルの寺院", "Jungle Temple"),
    "mansion": ("森の洋館", "Woodland Mansion"), "mineshaft": ("廃坑", "Mineshaft"),
    "mineshaft_mesa": ("荒野の廃坑", "Badlands Mineshaft"), "monument": ("海底神殿", "Ocean Monument"),
    "nether_fossil": ("ネザーの化石", "Nether Fossil"), "ocean_ruin_cold": ("冷たい海の海底遺跡", "Cold Ocean Ruins"),
    "ocean_ruin_warm": ("暖かい海の海底遺跡", "Warm Ocean Ruins"), "pillager_outpost": ("ピリジャーの前哨基地", "Pillager Outpost"),
    "ruined_portal": ("荒廃したポータル", "Ruined Portal"), "ruined_portal_desert": ("砂漠の荒廃したポータル", "Desert Ruined Portal"),
    "ruined_portal_jungle": ("ジャングルの荒廃したポータル", "Jungle Ruined Portal"),
    "ruined_portal_mountain": ("山の荒廃したポータル", "Mountain Ruined Portal"),
    "ruined_portal_nether": ("ネザーの荒廃したポータル", "Nether Ruined Portal"),
    "ruined_portal_ocean": ("海の荒廃したポータル", "Ocean Ruined Portal"),
    "ruined_portal_swamp": ("沼地の荒廃したポータル", "Swamp Ruined Portal"), "shipwreck": ("難破船", "Shipwreck"),
    "shipwreck_beached": ("打ち上げられた難破船", "Beached Shipwreck"), "stronghold": ("要塞", "Stronghold"),
    "swamp_hut": ("沼地の小屋", "Swamp Hut"), "trail_ruins": ("旅路の遺跡", "Trail Ruins"),
    "village_desert": ("砂漠の村", "Desert Village"), "village_plains": ("平原の村", "Plains Village"),
    "village_savanna": ("サバンナの村", "Savanna Village"), "village_snowy": ("雪の村", "Snowy Village"),
    "village_taiga": ("タイガの村", "Taiga Village"), "trial_chambers": ("試練の間", "Trial Chambers"),
}
STRUCT_CHALLENGE = {"ancient_city", "mansion", "end_city", "stronghold", "bastion_remnant", "monument", "trial_chambers"}

MOBS = ["allay", "armadillo", "axolotl", "bat", "bee", "blaze", "bogged", "breeze", "camel", "cat", "cave_spider",
        "chicken", "cod", "cow", "creeper", "dolphin", "donkey", "drowned", "elder_guardian", "ender_dragon", "enderman",
        "endermite", "evoker", "fox", "frog", "ghast", "glow_squid", "goat", "guardian", "hoglin", "horse", "husk",
        "iron_golem", "llama", "magma_cube", "mooshroom", "mule", "ocelot", "panda", "parrot", "phantom", "pig",
        "piglin", "piglin_brute", "pillager", "polar_bear", "pufferfish", "rabbit", "ravager", "salmon", "sheep",
        "shulker", "silverfish", "skeleton", "skeleton_horse", "slime", "sniffer", "snow_golem", "spider", "squid",
        "stray", "strider", "tadpole", "trader_llama", "tropical_fish", "turtle", "vex", "villager", "vindicator",
        "wandering_trader", "warden", "witch", "wither", "wither_skeleton", "wolf", "zoglin", "zombie", "zombie_horse",
        "zombie_villager", "zombified_piglin", "creaking", "happy_ghast", "copper_golem", "parched", "camel_husk",
        "nautilus", "zombie_nautilus", "sulfur_cube"]
MOB_CHALLENGE = {"ender_dragon", "wither", "warden", "elder_guardian"}
MOB_GOAL = {"evoker", "ravager", "piglin_brute", "shulker", "breeze", "ghast", "blaze", "wither_skeleton", "guardian",
            "hoglin", "zoglin", "vex", "allay", "skeleton_horse", "zombie_horse"}
KILL_TIERS = ["zombie", "skeleton", "creeper", "spider", "enderman", "slime", "witch", "blaze", "ghast",
              "zombified_piglin", "piglin", "drowned", "phantom", "pillager", "vindicator", "wither_skeleton",
              "magma_cube", "husk", "stray", "guardian", "hoglin", "cave_spider", "silverfish", "zombie_villager",
              "breeze", "bogged", "cow", "pig", "sheep", "chicken"]
BREED = ["cow", "pig", "sheep", "chicken", "horse", "donkey", "llama", "rabbit", "wolf", "cat", "ocelot", "fox",
         "panda", "bee", "turtle", "hoglin", "strider", "goat", "axolotl", "frog", "camel", "sniffer", "armadillo",
         "mooshroom", "trader_llama"]
TAME = ["wolf", "cat", "parrot", "horse", "donkey", "mule", "llama", "trader_llama", "skeleton_horse"]
MINE_TIERS = {  # block: tiers
    "stone": [64, 1000, 10000], "deepslate": [64, 1000, 10000], "dirt": [64, 1000, 5000],
    "grass_block": [64, 1000, 5000], "sand": [64, 1000, 5000], "gravel": [64, 1000], "netherrack": [64, 1000, 10000],
    "end_stone": [64, 1000], "oak_log": [64, 1000], "spruce_log": [64, 1000], "birch_log": [64, 1000],
    "jungle_log": [64, 1000], "acacia_log": [64, 1000], "dark_oak_log": [64, 1000], "cherry_log": [64, 1000],
    "mangrove_log": [64, 1000], "coal_ore": [64, 500], "iron_ore": [64, 500], "deepslate_iron_ore": [64, 500],
    "copper_ore": [64, 500], "gold_ore": [32, 256], "deepslate_gold_ore": [32, 256], "redstone_ore": [32, 256],
    "deepslate_redstone_ore": [32, 256], "lapis_ore": [16, 128], "deepslate_lapis_ore": [16, 128],
    "diamond_ore": [8, 64], "deepslate_diamond_ore": [8, 64, 256], "emerald_ore": [4, 32], "ancient_debris": [4, 32, 128],
    "nether_quartz_ore": [64, 500], "nether_gold_ore": [64, 500], "obsidian": [16, 128], "clay": [64],
    "ice": [64], "sugar_cane": [64, 1000], "wheat": [64, 1000], "carrots": [64, 1000], "potatoes": [64, 1000],
    "melon": [64, 500], "pumpkin": [64, 500], "amethyst_cluster": [16, 128], "sculk": [64, 1000],
}
CM = 100  # 1 m
STATS = [  # stat, tiers, ja title, en title, ja desc fmt, en desc fmt, display divisor, icon
    ("walk_one_cm", [1000 * CM, 10000 * CM, 100000 * CM, 1000000 * CM], "おさんぽ", "Strolling", "%skm歩きましょう", "Walk %s km", 1000 * CM, "leather_boots"),
    ("sprint_one_cm", [1000 * CM, 10000 * CM, 100000 * CM], "全力ダッシュ", "Sprinting", "%skm走りましょう", "Sprint %s km", 1000 * CM, "golden_boots"),
    ("swim_one_cm", [100 * CM, 1000 * CM, 10000 * CM], "スイマー", "Swimmer", "%sm泳ぎましょう", "Swim %s m", CM, "cod"),
    ("walk_under_water_one_cm", [100 * CM, 1000 * CM], "海底散歩", "Seafloor Walk", "水中を%sm歩きましょう", "Walk %s m under water", CM, "turtle_helmet"),
    ("walk_on_water_one_cm", [100 * CM, 1000 * CM], "水上散歩", "Water Walk", "水の上を%sm歩きましょう", "Walk %s m on water", CM, "lily_pad"),
    ("aviate_one_cm", [1000 * CM, 10000 * CM, 100000 * CM], "大空の旅", "Skyfarer", "エリトラで%skm飛びましょう", "Glide %s km with elytra", 1000 * CM, "elytra"),
    ("boat_one_cm", [1000 * CM, 10000 * CM], "船長", "Captain", "ボートで%skm進みましょう", "Travel %s km by boat", 1000 * CM, "oak_boat"),
    ("horse_one_cm", [1000 * CM, 10000 * CM], "騎手", "Jockey", "ウマで%skm走りましょう", "Ride a horse %s km", 1000 * CM, "saddle"),
    ("pig_one_cm", [100 * CM, 1000 * CM], "ブタライダー", "Pig Rider", "ブタに乗って%sm進みましょう", "Ride a pig %s m", CM, "carrot_on_a_stick"),
    ("strider_one_cm", [100 * CM, 1000 * CM], "溶岩の旅人", "Lava Voyager", "ストライダーに乗って%sm進みましょう", "Ride a strider %s m", CM, "warped_fungus_on_a_stick"),
    ("minecart_one_cm", [1000 * CM, 10000 * CM], "鉄道ファン", "Rail Fan", "トロッコで%skm進みましょう", "Ride a minecart %s km", 1000 * CM, "minecart"),
    ("climb_one_cm", [100 * CM, 1000 * CM], "クライマー", "Climber", "はしごやツタで%sm登りましょう", "Climb %s m", CM, "ladder"),
    ("crouch_one_cm", [100 * CM, 1000 * CM, 10000 * CM], "忍び足", "Sneaky Steps", "しゃがんで%sm歩きましょう", "Sneak %s m", CM, "leather_leggings"),
    ("fall_one_cm", [1000 * CM, 10000 * CM], "落下のプロ", "Gravity Expert", "合計%sm落ちましょう", "Fall a total of %s m", CM, "feather"),
    ("jump", [100, 1000, 10000, 100000], "ジャンプ", "Jumper", "%s回ジャンプしましょう", "Jump %s times", 1, "rabbit_foot"),
    ("deaths", [1, 10, 100], "命はたいせつに", "Life Is Precious", "%s回力尽きましょう", "Die %s times", 1, "skeleton_skull"),
    ("play_time", [72000, 720000, 7200000], "ベテラン", "Veteran", "%s時間遊びましょう", "Play for %s hours", 72000, "clock"),
    ("mob_kills", [100, 1000, 10000], "ハンター", "Hunter", "モブを%s体倒しましょう", "Kill %s mobs", 1, "iron_sword"),
    ("fish_caught", [10, 100, 1000], "釣り人", "Angler", "魚を%s匹釣りましょう", "Catch %s fish", 1, "fishing_rod"),
    ("animals_bred", [10, 100, 1000], "牧場主", "Rancher", "動物を%s回繁殖させましょう", "Breed animals %s times", 1, "wheat"),
    ("traded_with_villager", [10, 100, 1000], "商売上手", "Merchant", "村人と%s回取引しましょう", "Trade with villagers %s times", 1, "emerald"),
    ("talked_to_villager", [100, 1000], "村の話し相手", "Village Chatter", "村人と%s回話しましょう", "Talk to villagers %s times", 1, "bell"),
    ("damage_dealt", [10000, 100000], "アタッカー", "Attacker", "合計%sダメージを与えましょう", "Deal %s damage", 10, "golden_sword"),
    ("damage_taken", [10000, 100000], "タンク", "Tank", "合計%sダメージを受けましょう", "Take %s damage", 10, "iron_chestplate"),
    ("damage_blocked_by_shield", [1000, 10000], "鉄壁", "Iron Wall", "盾で%sダメージ防ぎましょう", "Block %s damage with a shield", 10, "shield"),
    ("sleep_in_bed", [10, 100, 365], "おやすみ", "Good Night", "ベッドで%s回寝ましょう", "Sleep in a bed %s times", 1, "red_bed"),
    ("eat_cake_slice", [1, 10, 100], "ケーキ大好き", "Cake Lover", "ケーキを%s切れ食べましょう", "Eat %s slices of cake", 1, "cake"),
    ("open_chest", [100, 1000, 10000], "宝箱あけ", "Chest Opener", "チェストを%s回開けましょう", "Open chests %s times", 1, "chest"),
    ("open_barrel", [100, 1000], "たる職人", "Barrel Keeper", "たるを%s回開けましょう", "Open barrels %s times", 1, "barrel"),
    ("open_enderchest", [10, 100], "異次元収納", "Pocket Dimension", "エンダーチェストを%s回開けましょう", "Open ender chests %s times", 1, "ender_chest"),
    ("open_shulker_box", [10, 100], "持ち歩き倉庫", "Portable Storage", "シュルカーボックスを%s回開けましょう", "Open shulker boxes %s times", 1, "shulker_box"),
    ("enchant_item", [10, 100, 1000], "魔法使い", "Enchanter", "%s回エンチャントしましょう", "Enchant items %s times", 1, "enchanting_table"),
    ("drop", [100, 1000], "おそうじ", "Littering", "アイテムを%s回捨てましょう", "Drop items %s times", 1, "rotten_flesh"),
    ("raid_win", [1, 10], "村の英雄", "Village Hero", "襲撃に%s回勝ちましょう", "Win %s raids", 1, "white_banner"),
    ("bell_ring", [10, 100], "鐘つき", "Bell Ringer", "鐘を%s回鳴らしましょう", "Ring bells %s times", 1, "bell"),
    ("play_noteblock", [100, 1000], "演奏家", "Musician", "音ブロックを%s回鳴らしましょう", "Play note blocks %s times", 1, "note_block"),
    ("play_record", [1, 10, 100], "DJ", "DJ", "レコードを%s回かけましょう", "Play records %s times", 1, "jukebox"),
    ("pot_flower", [1, 10, 100], "園芸家", "Gardener", "植木鉢に%s回植えましょう", "Pot plants %s times", 1, "flower_pot"),
    ("interact_with_crafting_table", [100, 1000, 10000], "職人", "Crafter", "作業台を%s回使いましょう", "Use crafting tables %s times", 1, "crafting_table"),
    ("interact_with_furnace", [100, 1000], "かまど番", "Furnace Keeper", "かまどを%s回使いましょう", "Use furnaces %s times", 1, "furnace"),
    ("interact_with_anvil", [10, 100], "鍛冶屋", "Blacksmith", "金床を%s回使いましょう", "Use anvils %s times", 1, "anvil"),
    ("interact_with_smithing_table", [10, 100], "名匠", "Master Smith", "鍛冶台を%s回使いましょう", "Use smithing tables %s times", 1, "smithing_table"),
    ("interact_with_brewingstand", [10, 100], "錬金術師", "Alchemist", "醸造台を%s回使いましょう", "Use brewing stands %s times", 1, "brewing_stand"),
    ("use_cauldron", [10, 100], "大釜の魔女", "Cauldron Witch", "大釜の水を%s回使いましょう", "Use cauldrons %s times", 1, "cauldron"),
    ("clean_armor", [1, 10], "クリーニング", "Dry Cleaning", "革の防具を%s回洗いましょう", "Clean leather armor %s times", 1, "leather_chestplate"),
    ("target_hit", [10, 100], "的当て名人", "Marksman", "的に%s回当てましょう", "Hit targets %s times", 1, "target"),
    ("sneak_time", [72000, 720000], "隠密", "Shadow", "しゃがんで%s時間過ごしましょう", "Sneak for %s hours", 72000, "black_wool"),
    ("leave_game", [10, 100, 1000], "また来てね", "See You Again", "ゲームを%s回やめましょう", "Leave the game %s times", 1, "oak_door"),
    ("player_kills", [1, 10, 100], "PvP", "PvP", "プレイヤーを%s回倒しましょう", "Kill players %s times", 1, "iron_axe"),
    ("time_since_death", [24000, 168000, 720000, 2400000], "無事これ名馬", "Survivor", "死なずに%s日（ゲーム内）過ごしましょう", "Survive %s in-game days without dying", 24000, "golden_apple"),
    ("time_since_rest", [72000, 168000], "眠らない人", "Insomniac", "寝ずに%s日（ゲーム内）過ごしましょう", "Go %s in-game days without sleeping", 24000, "phantom_membrane"),
    ("raid_trigger", [1, 10], "不吉な予感", "Bad Omen", "襲撃を%s回起こしましょう", "Trigger %s raids", 1, "crossbow"),
    ("interact_with_beacon", [10, 100], "光の番人", "Beacon Keeper", "ビーコンを%s回開きましょう", "Use beacons %s times", 1, "beacon"),
    ("interact_with_blast_furnace", [100, 1000], "溶鉱炉の主", "Smelter", "溶鉱炉を%s回使いましょう", "Use blast furnaces %s times", 1, "blast_furnace"),
    ("interact_with_smoker", [100, 1000], "燻製名人", "Smokehouse", "燻製器を%s回使いましょう", "Use smokers %s times", 1, "smoker"),
    ("interact_with_campfire", [10, 100], "キャンパー", "Camper", "焚き火を%s回使いましょう", "Use campfires %s times", 1, "campfire"),
    ("interact_with_cartography_table", [10, 100], "地図職人", "Cartographer", "製図台を%s回使いましょう", "Use cartography tables %s times", 1, "cartography_table"),
    ("interact_with_grindstone", [10, 100], "研ぎ師", "Grinder", "砥石を%s回使いましょう", "Use grindstones %s times", 1, "grindstone"),
    ("interact_with_lectern", [10, 100], "読書家", "Reader", "書見台を%s回使いましょう", "Use lecterns %s times", 1, "lectern"),
    ("interact_with_loom", [10, 100], "機織り", "Weaver", "機織り機を%s回使いましょう", "Use looms %s times", 1, "loom"),
    ("interact_with_stonecutter", [100, 1000], "石工", "Stonemason", "石切台を%s回使いましょう", "Use stonecutters %s times", 1, "stonecutter"),
    ("inspect_hopper", [10, 100], "ホッパー点検", "Hopper Inspector", "ホッパーを%s回のぞきましょう", "Inspect hoppers %s times", 1, "hopper"),
    ("inspect_dispenser", [10, 100], "発射装置点検", "Dispenser Inspector", "発射装置を%s回のぞきましょう", "Inspect dispensers %s times", 1, "dispenser"),
    ("trigger_trapped_chest", [10, 100], "罠師", "Trapper", "トラップチェストを%s回開けましょう", "Open trapped chests %s times", 1, "trapped_chest"),
    ("tune_noteblock", [10, 100], "調律師", "Tuner", "音ブロックを%s回調律しましょう", "Tune note blocks %s times", 1, "note_block"),
    ("fill_cauldron", [10, 100], "水くみ", "Water Carrier", "大釜を%s回満たしましょう", "Fill cauldrons %s times", 1, "cauldron"),
    ("clean_shulker_box", [1, 10], "色落とし", "Bleach", "シュルカーボックスを%s回洗いましょう", "Clean shulker boxes %s times", 1, "shulker_box"),
    ("clean_banner", [1, 10], "旗洗い", "Banner Wash", "旗を%s回洗いましょう", "Clean banners %s times", 1, "white_banner"),
    ("damage_absorbed", [1000, 10000], "衝撃吸収", "Absorber", "衝撃吸収で%sダメージ受け止めましょう", "Absorb %s damage", 10, "golden_apple"),
    ("damage_resisted", [1000, 10000], "かたい体", "Toughness", "耐性で%sダメージ防ぎましょう", "Resist %s damage", 10, "turtle_helmet"),
]
FRAMES_BY_TIER = ["task", "task", "goal", "challenge", "challenge"]
XP = {"task": 5, "goal": 20, "challenge": 50}


def mod_cond(self, modid):
    if self.new:
        return {"_modid": modid}  # becomes neoforge:conditions or fabric:load_conditions when written
    return {"conditions": [{"type": "forge:mod_loaded", "modid": modid}]}


ENT26 = {"type": "minecraft:entity_type", "location": "minecraft:location", "flags": "minecraft:flags",
         "equipment": "minecraft:equipment", "stepping_on": "minecraft:stepping_on", "distance": "minecraft:distance"}


def ent26(p):
    """pre-26.2 entity predicate -> 26.2 sub-predicate map"""
    out = {}
    for k, v in p.items():
        if k == "type_specific":
            out[f"minecraft:type_specific/{v['type'].split(':')[-1]}"] = {kk: vv for kk, vv in v.items() if kk != "type"}
        elif k in ("vehicle", "passenger", "targeted_entity"):
            out[f"minecraft:{k}"] = ent26(v)
        elif k in ENT26:
            out[ENT26[k]] = v
        else:
            raise ValueError(f"unknown entity predicate key {k}")
    return out


def fix26(a):
    for c in a["criteria"].values():
        cond = c.get("conditions")
        if not cond:
            continue
        for k in list(cond):
            if k in ("player", "entity", "parent", "child", "partner") and isinstance(cond[k], dict):
                cond[k] = [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": ent26(cond[k])}]
        dmg = cond.get("damage")
        if isinstance(dmg, dict):
            if "source_entity" in dmg:
                dmg.setdefault("type", {})["source_entity"] = ent26(dmg.pop("source_entity"))
    return a


def loader_cond(a, loader):
    a = dict(a)
    modid = a.pop("_modid", None)
    if modid:
        if loader == "fabric":
            a["fabric:load_conditions"] = [{"condition": "fabric:all_mods_loaded", "values": [modid]}]
        else:
            a["neoforge:conditions"] = [{"type": "neoforge:mod_loaded", "modid": modid}]
    return a


# the custom stats the Fabric builds keep (TonsOfAdv.NAMES in the Fabric projects); others are skipped there
FABRIC_STATS = {"turned_right", "turned_left", "looked_up", "looked_down", "looked_straight_up", "looked_straight_down",
                "moved_backward", "moved_left", "moved_right", "sneak_presses",
                "crafted_total", "smelted_total", "blocks_placed", "blocks_broken", "xp_gained", "max_level", "food_eaten",
                "potions_drunk", "arrows_shot", "animals_tamed", "advancements_earned",
                "chat_messages", "commands_used", "dims_changed", "crops_harvested",
                "logs_chopped", "ores_mined", "entity_interactions", "anvil_repairs"}


def fabric_ok(a):
    return all(st in FABRIC_STATS for st in re.findall(r'"' + NS + r':([a-z0-9_]+)"', json.dumps(a["criteria"])))


class Gen:
    def __init__(self, ver):
        self.v = ver
        self.new = ver != "1.20.1"
        self.v26 = ver.startswith("26")
        lp = os.path.join(REG, f"lang-{ver}.json")
        self.vlang = json.load(open(lp, encoding="utf-8")) if os.path.exists(lp) else None
        self.reg = load(ver)
        self.items = set(self.reg["item"])
        self.blocks = set(self.reg["block"])
        self.ents = set(self.reg["entity_type"])
        self.advs = {}

    # ---- format helpers ----
    def icon(self, item):
        return {"id": f"minecraft:{item}"} if self.new else {"item": f"minecraft:{item}"}

    def ipred(self, item, count=None):
        p = {"items": f"minecraft:{item}"} if self.new else {"items": [f"minecraft:{item}"]}
        if count:
            p["count"] = {"min": count}
        return p

    def player_stat(self, stype, stat, mn):
        return {"type_specific": {"type": "minecraft:player" if self.new else "player",
                                  "stats": [{"type": f"minecraft:{stype}", "stat": stat if ":" in stat else f"minecraft:{stat}", "value": {"min": mn}}]}}

    def loc_biome(self, b):
        return {"biomes": f"minecraft:{b}"} if self.new else {"biome": f"minecraft:{b}"}

    def loc_struct(self, s):
        return {"structures": f"minecraft:{s}"} if self.new else {"structure": f"minecraft:{s}"}

    def item_name(self, item):
        base = self.plain_item_name(item)
        if (item.startswith("music_disc_") or item.endswith("_banner_pattern") or item == "disc_fragment_5") and \
                (self.vlang is None or f"item.minecraft.{item}.desc" in self.vlang):
            return tr(f"{NS}.obtain.title2", base, vtr(f"item.minecraft.{item}.desc"))
        if item.endswith("_armor_trim_smithing_template"):
            pat = item[:-len("_armor_trim_smithing_template")]
            return tr(f"{NS}.obtain.title2", base, vtr(f"trim_pattern.minecraft.{pat}"))
        if item == "netherite_upgrade_smithing_template":
            return tr(f"{NS}.obtain.title2", base, vtr("upgrade.minecraft.netherite_upgrade"))
        return base

    def plain_item_name(self, item):
        kind = "block" if item in self.blocks else "item"
        key = f"{kind}.minecraft.{item}"
        if self.vlang is not None and key not in self.vlang:
            for alt in (f"item.minecraft.{item}", f"block.minecraft.{item}",
                        "item.minecraft.smithing_template" if item.endswith("smithing_template") else None):
                if alt and alt in self.vlang:
                    return vtr(alt)
        return vtr(key)

    def add(self, path, parent, icon, title, desc, criteria, frame="task", xp=None, chat=None, bg=None, hidden=False):
        disp = {"icon": self.icon(icon), "title": title, "description": desc, "frame": frame,
                "show_toast": bg is None, "announce_to_chat": (frame != "task") if chat is None else chat,
                "hidden": hidden}
        if bg:
            disp["background"] = bg
        for c in criteria.values():
            if c.get("trigger") == "minecraft:tick" and c.get("conditions"):
                c["trigger"] = "minecraft:location"
        a = {"display": disp, "criteria": criteria, "requirements": [list(criteria.keys())]}
        if parent:
            a["parent"] = f"{NS}:{parent}"
        x = XP[frame] if xp is None else xp
        if x:
            a["rewards"] = {"experience": x}
        assert path not in self.advs, path
        self.advs[path] = a
        return path

    def root(self, tab):
        ja, en, icon, bg = TABS[tab]
        t = L(f"{NS}.tab.{tab}", f"どっさり：{ja}", f"Tons: {en}")
        d = L(f"{NS}.tab.{tab}.desc", f"「{ja}」の進捗がどっさり詰まっています", f"Lots of {en} advancements")
        return self.add(f"{tab}/root", None, icon, tr(t), tr(d), {"tick": {"trigger": "minecraft:tick"}},
                        frame="task", xp=0, chat=False, bg=bg)

    # ---- categorisation ----
    def cat(self, it):
        toks = it.split("_")
        if it in FOODS or it == "cake":
            return "food", "food"
        if it in EQUIP_MISC or any(it.endswith(s) for s in EQUIP_SUFFIX) or it.endswith("smithing_template"):
            if it.endswith("smithing_template"):
                return "equip", "templates"
            for s in EQUIP_SUFFIX:
                if it.endswith(s) and it != "turtle_helmet":
                    return "equip", it[: -len(s)]
            return "equip", "misc_tools"
        if it.endswith("_dye"):
            return "colors", "dye"
        for c in COLORS:
            if it.startswith(c + "_"):
                return "colors", it[len(c) + 1:]
        for w in WOODS:
            if ("_" + w + "_") in ("_" + it + "_"):
                if w == "oak" and "dark_oak" in it:
                    continue
                return "woods", w
        if "copper" in it and "copper_bulb" not in it and not it.endswith("_door") and not it.endswith("trapdoor"):
            return "minerals", "copper"
        for m in MINERALS:
            if m in toks or it.startswith(m + "_") or it == m or (m == "lapis" and "lapis" in it) or (m == "amethyst" and "amethyst" in it):
                if m == "redstone" and it not in ("redstone", "redstone_block", "redstone_ore", "deepslate_redstone_ore"):
                    break
                if m == "quartz" and it not in ("quartz", "nether_quartz_ore"):
                    break
                if m == "charcoal":
                    return "minerals", "coal"
                if m == "ancient_debris":
                    return "minerals", "netherite"
                return "minerals", m
        if it.endswith("_ore") or it.startswith("raw_"):
            return "minerals", "ores"
        for k in REDSTONE_KW:
            if k in toks or k in it:
                return "redstone", "redstone"
        for k in STONE_KW:
            if k in toks:
                key = "end" if k == "end" else ("purpur" if k == "purpur" else k)
                return "stone", key
        for k in NATURE_KW:
            if k in toks or k == it or ("_" + k) in it or (k + "_") in it:
                return "nature", "nature"
        return "misc", "misc"

    # ---- generators ----
    def gen_obtain(self):
        buckets = {}
        for it in sorted(self.items):
            if it in EXCLUDE or it.endswith("_spawn_egg") or it.startswith("infested_"):
                continue
            tab, grp = self.cat(it)
            buckets.setdefault(tab, {}).setdefault(grp, []).append(it)
        order = ["woods", "stone", "colors", "minerals", "equip", "redstone", "nature", "food", "misc"]
        for tab in order:
            groups = buckets.get(tab, {})
            # merge tiny groups and chunk big ones
            final, small = [], []
            for g in sorted(groups):
                its = groups[g]
                if len(its) < 3:
                    small.extend(its)
                else:
                    for i in range(0, len(its), 16):
                        final.append(its[i:i + 16])
            small.sort()
            for i in range(0, len(small), 12):
                final.append(small[i:i + 12])
            # split tab into parts of ~170 items
            parts, cur, n = [], [], 0
            for g in final:
                if n + len(g) > 170 and cur:
                    parts.append(cur); cur, n = [], 0
                cur.append(g); n += len(g)
            if cur:
                if parts and sum(len(g) for g in cur) < 60:
                    parts[-1].extend(cur)
                else:
                    parts.append(cur)
            for pi, part in enumerate(parts):
                tkey = tab if pi == 0 else f"{tab}{pi + 1}"
                if tkey not in TABS:
                    ja, en, icon, bg = TABS[tab]
                    TABS[tkey] = (f"{ja} {pi + 1}", f"{en} {pi + 1}", part[0][0], bg)
                r = self.root(tkey)
                for g in part:
                    head = None
                    for it in g:
                        frame = "challenge" if it in RARE_CHALLENGE else ("goal" if it in RARE_GOAL else "task")
                        p = self.add(f"{tkey}/{it}", head or r, it, self.item_name(it),
                                     tr(f"{NS}.obtain.desc", self.plain_item_name(it)),
                                     {"get": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [self.ipred(it)]}}},
                                     frame=frame)
                        head = head or p

    def ent(self, m):
        return vtr(f"entity.minecraft.{m}")

    def gen_eat(self):
        r = self.root("eat")
        prev = r
        for i, f in enumerate([f for f in FOODS if f in self.items]):
            frame = "challenge" if f == "enchanted_golden_apple" else ("goal" if f in ("golden_apple", "rabbit_stew", "suspicious_stew", "pufferfish", "poisonous_potato") else "task")
            parent = r if i % 8 == 0 else prev
            prev = self.add(f"eat/{f}", parent, f, tr(f"{NS}.eat.title", self.plain_item_name(f)),
                            tr(f"{NS}.eat.desc", self.plain_item_name(f)),
                            {"eat": {"trigger": "minecraft:consume_item", "conditions": {"item": self.ipred(f)}}}, frame=frame)
        # cake (block)
        self.add("eat/cake_slice", r, "cake", tr(f"{NS}.eat.title", vtr("block.minecraft.cake")),
                 tr(f"{NS}.eat.desc", vtr("block.minecraft.cake")),
                 {"s": {"trigger": "minecraft:tick", "conditions": {"player": self.player_stat("custom", "eat_cake_slice", 1)}}})

    def gen_kill(self):
        r = self.root("kill")
        mobs = [m for m in MOBS if m in self.ents]
        heads = {}
        for i, m in enumerate(mobs):
            frame = "challenge" if m in MOB_CHALLENGE else ("goal" if m in MOB_GOAL else "task")
            parent = r if i % 10 == 0 else heads["last"]
            heads["last"] = self.add(f"kill/{m}", parent, self.mob_icon(m), tr(f"{NS}.kill.title", self.ent(m)),
                                     tr(f"{NS}.kill.desc", self.ent(m)),
                                     {"kill": {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {"type": f"minecraft:{m}"}}}},
                                     frame=frame)
        r2 = self.root("killn")
        for m in [m for m in KILL_TIERS if m in self.ents]:
            prev = r2
            for ti, n in enumerate([10, 100, 1000]):
                prev = self.add(f"killn/{m}_{n}", prev, self.mob_icon(m), tr(f"{NS}.killn.title", self.ent(m), n),
                                tr(f"{NS}.killn.desc", self.ent(m), n),
                                {"s": {"trigger": "minecraft:tick", "conditions": {"player": self.player_stat("killed", m, n)}}},
                                frame=FRAMES_BY_TIER[ti + 1], xp=[10, 30, 100][ti])

    def mob_icon(self, m):
        special = {"ender_dragon": "dragon_head", "wither": "nether_star", "iron_golem": "iron_block",
                   "snow_golem": "carved_pumpkin", "villager": "emerald", "wandering_trader": "emerald",
                   "player": "player_head"}
        if m in special:
            return special[m]
        if f"{m}_spawn_egg" in self.items:
            return f"{m}_spawn_egg"
        return "bone"

    def gen_animals(self):
        r = self.root("animals")
        prev = r
        for i, m in enumerate([m for m in BREED if m in self.ents]):
            key = "child" if m == "mule" else "parent"
            prev = self.add(f"animals/breed_{m}", r if i % 9 == 0 else prev, self.mob_icon(m),
                            tr(f"{NS}.breed.title", self.ent(m)), tr(f"{NS}.breed.desc", self.ent(m)),
                            {"b": {"trigger": "minecraft:bred_animals", "conditions": {key: {"type": f"minecraft:{m}"}}}})
        self.add("animals/breed_mule", r, "mule_spawn_egg", tr(f"{NS}.breed.title", self.ent("mule")),
                 tr(f"{NS}.breed.desc", self.ent("mule")),
                 {"b": {"trigger": "minecraft:bred_animals", "conditions": {"child": {"type": "minecraft:mule"}}}}, frame="goal")
        prev = r
        for i, m in enumerate([m for m in TAME if m in self.ents]):
            prev = self.add(f"animals/tame_{m}", r if i == 0 else prev, self.mob_icon(m),
                            tr(f"{NS}.tame.title", self.ent(m)), tr(f"{NS}.tame.desc", self.ent(m)),
                            {"t": {"trigger": "minecraft:tame_animal", "conditions": {"entity": {"type": f"minecraft:{m}"}}}},
                            frame="goal" if m == "skeleton_horse" else "task")

    def gen_explore(self):
        r = self.root("explore")
        dims = [("the_nether", "ネザーへようこそ", "Welcome to the Nether", "ネザーに行きましょう", "Enter the Nether", "netherrack", "goal"),
                ("the_end", "果ての世界", "The World's End", "ジ・エンドに行きましょう", "Enter the End", "end_stone", "challenge"),
                ("overworld", "ただいま", "I'm Home", "オーバーワールドに帰りましょう", "Return to the Overworld", "grass_block", "task")]
        for d, ja, en, dja, den, icon, frame in dims:
            t = L(f"{NS}.dim.{d}", ja, en); ds = L(f"{NS}.dim.{d}.desc", dja, den)
            self.add(f"explore/dim_{d}", r, icon, tr(t), tr(ds),
                     {"d": {"trigger": "minecraft:changed_dimension", "conditions": {"to": f"minecraft:{d}"}}}, frame=frame)
        biomes = [b for b in self.reg["worldgen/biome"] if b != "the_void"]
        icons = {"nether": "netherrack", "end": "end_stone", "ocean": "water_bucket", "forest": "oak_sapling",
                 "jungle": "jungle_sapling", "taiga": "spruce_sapling", "desert": "sand", "badlands": "red_sand",
                 "snow": "snowball", "frozen": "ice", "savanna": "acacia_sapling", "swamp": "lily_pad",
                 "cave": "pointed_dripstone", "peaks": "stone", "mushroom": "red_mushroom", "cherry": "cherry_sapling"}
        nether = {"nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas"}
        prev = r
        for i, b in enumerate(sorted(biomes)):
            ic = "grass_block"
            if b in nether: ic = "netherrack"
            elif b.startswith("end_") or b in ("the_end", "small_end_islands"): ic = "end_stone"
            else:
                for k, v in icons.items():
                    if k in b: ic = v; break
            name = vtr(f"biome.minecraft.{b}")
            prev = self.add(f"explore/biome_{b}", r if i % 8 == 0 else prev, ic, name, tr(f"{NS}.biome.desc", name),
                            {"v": {"trigger": "minecraft:location", "conditions": {"player": {"location": self.loc_biome(b)}}}},
                            frame="goal" if b in ("mushroom_fields", "deep_dark", "ice_spikes", "eroded_badlands", "bamboo_jungle") else "task")
        # Y extremes
        for key, ja, en, dja, den, ic, pos, frame in [
            ("bedrock", "どん底", "Rock Bottom", "Y-60より下に行きましょう", "Go below Y -60", "deepslate", {"y": {"max": -60}}, "task"),
            ("sky", "天空の果て", "Top of the World", "Y300より上に行きましょう", "Go above Y 300", "feather", {"y": {"min": 300}}, "goal"),
            ("far", "地の果て", "Far Lands", "原点から1万ブロック以上離れましょう", "Go 10,000 blocks from the origin", "compass", None, "challenge")]:
            t = L(f"{NS}.explore.{key}", ja, en); ds = L(f"{NS}.explore.{key}.desc", dja, den)
            if pos:
                crit = {"v": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"position": pos}}}}}
            else:
                crit = {"x1": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"position": {"x": {"min": 10000}}}}}},
                        "x2": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"position": {"x": {"max": -10000}}}}}},
                        "z1": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"position": {"z": {"min": 10000}}}}}},
                        "z2": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"position": {"z": {"max": -10000}}}}}}}
            p = self.add(f"explore/{key}", r, ic, tr(t), tr(ds), crit, frame=frame)
            if not pos:
                self.advs[p]["requirements"] = [["x1", "x2", "z1", "z2"]]
        # structures
        r2 = self.root("structures")
        structs = list(self.reg["worldgen/structure"]) or []
        if self.new and not structs:
            structs = list(load("1.20.1")["worldgen/structure"]) + ["trial_chambers"]
        sicon = {"ancient_city": "sculk_catalyst", "bastion_remnant": "gilded_blackstone", "buried_treasure": "heart_of_the_sea",
                 "desert_pyramid": "sandstone", "end_city": "purpur_block", "fortress": "nether_bricks", "igloo": "snow_block",
                 "jungle_pyramid": "mossy_cobblestone", "mansion": "dark_oak_planks", "mineshaft": "rail", "mineshaft_mesa": "rail",
                 "monument": "prismarine", "nether_fossil": "bone_block", "ocean_ruin_cold": "stone_bricks",
                 "ocean_ruin_warm": "sandstone", "pillager_outpost": "crossbow", "shipwreck": "oak_boat",
                 "shipwreck_beached": "oak_boat", "stronghold": "ender_eye", "swamp_hut": "cauldron", "trail_ruins": "brush",
                 "village_desert": "smooth_sandstone", "village_plains": "oak_planks", "village_savanna": "acacia_planks",
                 "village_snowy": "snow_block", "village_taiga": "spruce_planks", "trial_chambers": "trial_key"}
        prev = r2
        for i, s in enumerate(sorted(structs)):
            ja, en = STRUCT_NAMES[s]
            t = L(f"{NS}.structure.{s}", ja, en)
            prev = self.add(f"structures/{s}", r2 if i % 6 == 0 else prev, sicon.get(s, "obsidian"), tr(t),
                            tr(f"{NS}.struct.desc", tr(t)),
                            {"v": {"trigger": "minecraft:location", "conditions": {"player": {"location": self.loc_struct(s)}}}},
                            frame="challenge" if s in STRUCT_CHALLENGE else "task")

    def gen_mining(self):
        r = self.root("mining")
        for b, tiers in MINE_TIERS.items():
            if b not in self.blocks:
                continue
            prev = r
            icon = b if b in self.items else {"carrots": "carrot", "potatoes": "potato"}.get(b, "wheat")
            name = vtr(f"block.minecraft.{b}")
            for ti, n in enumerate(tiers):
                prev = self.add(f"mining/{b}_{n}", prev, icon, tr(f"{NS}.mine.title", name, n), tr(f"{NS}.mine.desc", name, n),
                                {"s": {"trigger": "minecraft:tick", "conditions": {"player": self.player_stat("mined", b, n)}}},
                                frame=FRAMES_BY_TIER[ti + 1] if ti else "task", xp=[5, 20, 50][min(ti, 2)])

    def gen_stats(self):
        r = self.root("stats")
        for stat, tiers, ja, en, dja, den, div, icon in STATS:
            prev = r
            for ti, n in enumerate(tiers):
                shown = n // div if n % div == 0 else n / div
                t = L(f"{NS}.stat.{stat}.{ti}", f"{ja} Lv.{ti + 1}", f"{en} Lv.{ti + 1}")
                d = L(f"{NS}.stat.{stat}.desc", dja, den)
                prev = self.add(f"stats/{stat}_{ti + 1}", prev, icon, tr(t), tr(d, f"{shown:,}"),
                                {"s": {"trigger": "minecraft:tick", "conditions": {"player": self.player_stat("custom", stat, n)}}},
                                frame=FRAMES_BY_TIER[min(ti, 4)], xp=[5, 15, 40, 100, 200][min(ti, 4)])

    def gen_fun(self):
        r = self.root("fun")
        F = []  # (key, ja, en, dja, den, icon, criteria, frame)
        def c(trigger, cond=None):
            x = {"trigger": f"minecraft:{trigger}"}
            if cond is not None:
                x["conditions"] = cond
            return {"c": x}
        tickp = lambda p: c("tick", {"player": p})
        eqp = lambda slot, it: {"equipment": {slot: self.ipred(it)}}
        F += [
            ("sleep", "すやすや", "Sweet Dreams", "ベッドで寝ましょう", "Sleep in a bed", "white_bed", c("slept_in_bed"), "task"),
            ("skydive", "スカイダイバー", "Skydiver", "50ブロック以上落ちて生き延びましょう", "Fall 50+ blocks and survive", "feather", c("fall_from_height", {"distance": {"y": {"min": 50}}}), "goal"),
            ("levitate", "ふわふわ", "Floaty", "浮遊で30ブロック以上浮かびましょう", "Levitate 30+ blocks", "shulker_shell", c("levitation", {"distance": {"y": {"min": 30}}}), "goal"),
            ("totem", "九死に一生", "Close Call", "不死のトーテムに助けられましょう", "Be saved by a Totem of Undying", "totem_of_undying", c("used_totem", {"item": self.ipred("totem_of_undying")}), "goal"),
            ("cure", "お医者さん", "Doctor", "村人ゾンビを治しましょう", "Cure a zombie villager", "golden_apple", c("cured_zombie_villager"), "goal"),
            ("brew", "はじめての醸造", "First Brew", "ポーションを醸造しましょう", "Brew a potion", "brewing_stand", c("brewed_potion"), "task"),
            ("beacon4", "光の柱", "Pillar of Light", "最大レベルのビーコンを作りましょう", "Build a full-power beacon", "beacon", c("construct_beacon", {"level": {"min": 4}}), "challenge"),
            ("channel", "雷神", "Thunder God", "忠誠のトライデントで雷を落としましょう", "Channel lightning with a trident", "trident", c("channeled_lightning"), "goal"),
            ("bullseye", "ど真ん中", "Dead Center", "的のど真ん中に当てましょう", "Hit the bullseye of a target", "target", c("target_hit", {"signal_strength": 15}), "task"),
            ("hero", "村のヒーロー", "Village Hero", "村の英雄になりましょう", "Become Hero of the Village", "emerald_block", c("hero_of_the_village"), "challenge"),
            ("exile", "追放", "Exile", "襲撃隊長を倒しましょう", "Kill a raid captain", "white_banner", c("voluntary_exile"), "task"),
            ("nether_travel", "近道", "Shortcut", "ネザー経由でオーバーワールドを7000ブロック移動しましょう", "Travel 7000 blocks via the Nether", "obsidian", c("nether_travel", {"distance": {"horizontal": {"min": 7000}}}), "challenge"),
            ("lava_ride", "溶岩クルーズ", "Lava Cruise", "ストライダーに乗って溶岩の上を50ブロック進みましょう", "Ride a strider 50 blocks on lava", "warped_fungus_on_a_stick", c("ride_entity_in_lava", {"distance": {"horizontal": {"min": 50}}}), "goal"),
            ("ride", "のりもの好き", "Passenger", "何かに乗りましょう", "Ride something", "saddle", c("started_riding"), "task"),
            ("allay", "おつかいアレイ", "Errand Allay", "アレイにアイテムを渡しましょう", "Give an item to an allay", "allay_spawn_egg", c("thrown_item_picked_up_by_entity", {"entity": {"type": "minecraft:allay"}}), "task"),
            ("enchant30", "大魔法", "Grand Magic", "レベル30のエンチャントをしましょう", "Enchant with 30 levels", "enchanted_book", c("enchanted_item", {"levels": {"min": 30}}), "goal"),
            ("poison", "毒見役", "Food Taster", "毒を受けましょう", "Get poisoned", "spider_eye", c("effects_changed", {"effects": {"minecraft:poison": {}}}), "task"),
            ("lightning", "雷の目撃者", "Lightning Witness", "近くに雷が落ちるのを見ましょう", "Witness a lightning strike nearby", "lightning_rod", c("lightning_strike"), "task"),
            ("bee_nest", "ハチの巣荒らし", "Nest Raider", "ハチの巣を壊しましょう", "Break a bee nest", "bee_nest", c("bee_nest_destroyed", {"block": "minecraft:bee_nest"}), "task"),
            ("axolotl_bucket", "バケツの中の友だち", "Bucket Buddy", "ウーパールーパーをバケツですくいましょう", "Catch an axolotl in a bucket", "axolotl_bucket", c("filled_bucket", {"item": self.ipred("axolotl_bucket")}), "task"),
            ("tadpole_bucket", "おたまじゃくし採集", "Tadpole Collector", "オタマジャクシをバケツですくいましょう", "Catch a tadpole in a bucket", "tadpole_bucket", c("filled_bucket", {"item": self.ipred("tadpole_bucket")}), "task"),
            ("sculk_kill", "スカルクのごはん", "Sculk Snack", "スカルクカタリストの近くでモブを倒しましょう", "Kill a mob near a sculk catalyst", "sculk_catalyst", c("kill_mob_near_sculk_catalyst"), "task"),
            ("sneak_sculk", "抜き足差し足", "Tiptoe", "スカルクセンサーの近くをしゃがんで歩きましょう", "Sneak near a sculk sensor", "sculk_sensor", c("avoid_vibration"), "task"),
            ("on_fire", "あちちっ", "Hot Hot Hot", "燃えましょう", "Catch fire", "flint_and_steel", tickp({"flags": {"is_on_fire": True}}), "task"),
            ("swim", "クロール", "Front Crawl", "泳ぎましょう", "Swim", "water_bucket", tickp({"flags": {"is_swimming": True}}), "task"),
            ("pumpkin", "かぼちゃ頭", "Pumpkin Head", "くり抜かれたカボチャをかぶりましょう", "Wear a carved pumpkin", "carved_pumpkin", tickp(eqp("head", "carved_pumpkin")), "task"),
            ("full_leather", "革のおしゃれ", "Leather Fashion", "革の防具を全身に着ましょう", "Wear full leather armor", "leather_chestplate",
             tickp({"equipment": {"head": self.ipred("leather_helmet"), "chest": self.ipred("leather_chestplate"), "legs": self.ipred("leather_leggings"), "feet": self.ipred("leather_boots")}}), "task"),
            ("full_gold", "成金", "Bling", "金の防具を全身に着ましょう", "Wear full golden armor", "golden_chestplate",
             tickp({"equipment": {"head": self.ipred("golden_helmet"), "chest": self.ipred("golden_chestplate"), "legs": self.ipred("golden_leggings"), "feet": self.ipred("golden_boots")}}), "goal"),
            ("full_netherite", "最強装備", "Fully Loaded", "ネザライトの防具を全身に着ましょう", "Wear full netherite armor", "netherite_chestplate",
             tickp({"equipment": {"head": self.ipred("netherite_helmet"), "chest": self.ipred("netherite_chestplate"), "legs": self.ipred("netherite_leggings"), "feet": self.ipred("netherite_boots")}}), "challenge"),
            ("dirt64", "土の山", "Dirt Pile", "土を64個同時に持ちましょう", "Have 64 dirt at once", "dirt", c("inventory_changed", {"items": [self.ipred("dirt", 64)]}), "task"),
            ("flesh64", "腐った在庫", "Rotten Stock", "腐った肉を64個同時に持ちましょう", "Have 64 rotten flesh at once", "rotten_flesh", c("inventory_changed", {"items": [self.ipred("rotten_flesh", 64)]}), "task"),
            ("diamond64", "ダイヤの山", "Diamond Hoard", "ダイヤモンドを64個同時に持ちましょう", "Have 64 diamonds at once", "diamond", c("inventory_changed", {"items": [self.ipred("diamond", 64)]}), "challenge"),
            ("emerald64", "大富豪", "Tycoon", "エメラルドを64個同時に持ちましょう", "Have 64 emeralds at once", "emerald", c("inventory_changed", {"items": [self.ipred("emerald", 64)]}), "goal"),
            ("netherite_block", "ネザライト長者", "Netherite Baron", "ネザライトブロックを持ちましょう", "Have a block of netherite", "netherite_block", c("inventory_changed", {"items": [self.ipred("netherite_block")]}), "challenge"),
            ("die_once", "はじめての死", "First Death", "力尽きましょう（ごめんなさい）", "Die (sorry)", "bone", tickp(self.player_stat("custom", "deaths", 1)), "task"),
            ("cake_hoarder", "ケーキ職人", "Cake Baker", "ケーキを10個作りましょう", "Craft 10 cakes", "cake", tickp(self.player_stat("crafted", "cake", 10)), "task"),
            ("pearl100", "テレポーター", "Teleporter", "エンダーパールを100回投げましょう", "Throw 100 ender pearls", "ender_pearl", tickp(self.player_stat("used", "ender_pearl", 100)), "goal"),
            ("totem5", "不死身", "Immortal", "不死のトーテムを5回使いましょう", "Use 5 totems of undying", "totem_of_undying", tickp(self.player_stat("used", "totem_of_undying", 5)), "challenge"),
            ("break_pick", "つるはし破壊神", "Pickaxe Breaker", "ダイヤのつるはしを壊しましょう", "Break a diamond pickaxe", "diamond_pickaxe", tickp(self.player_stat("broken", "diamond_pickaxe", 1)), "goal"),
            ("pick_up_1000", "拾い物名人", "Collector", "アイテムを1000個拾いましょう（丸石）", "Pick up 1000 cobblestone", "cobblestone", tickp(self.player_stat("picked_up", "cobblestone", 1000)), "task"),
            ("torches", "照明係", "Lighting Crew", "たいまつを1000本置きましょう", "Place 1000 torches", "torch", tickp(self.player_stat("used", "torch", 1000)), "goal"),
            ("iron_golem", None, None, None, None, "iron_block", c("summoned_entity", {"entity": {"type": "minecraft:iron_golem"}}), "task"),
            ("snow_golem", None, None, None, None, "carved_pumpkin", c("summoned_entity", {"entity": {"type": "minecraft:snow_golem"}}), "task"),
            ("wither", None, None, None, None, "wither_skeleton_skull", c("summoned_entity", {"entity": {"type": "minecraft:wither"}}), "challenge"),
        ]
        prev = r
        for i, (key, ja, en, dja, den, icon, crit, frame) in enumerate(F):
            if icon not in self.items:
                continue
            if ja is None:
                m = key
                title, desc = tr(f"{NS}.summon.title", self.ent(m)), tr(f"{NS}.summon.desc", self.ent(m))
            else:
                title = tr(L(f"{NS}.fun.{key}", ja, en)); desc = tr(L(f"{NS}.fun.{key}.desc", dja, den))
            prev = self.add(f"fun/{key}", r if i % 8 == 0 else prev, icon, title, desc, crit, frame=frame)

    def run(self):
        self.gen_obtain(); self.gen_eat(); self.gen_kill(); self.gen_animals()
        self.gen_explore(); self.gen_mining(); self.gen_stats(); self.gen_fun()
        self.gen_actions_vanilla()
        self.gen_more_vanilla()
        self.gen_small_vanilla()
        self.vanilla_paths = set(self.advs)
        self.gen_mods()
        self.gen_actions_java()
        self.gen_small_java()
        # sanity: icons must exist, parents must exist
        for p, a in self.advs.items():
            ic = a["display"]["icon"]
            full = ic.get("id") or ic.get("item")
            if full.startswith("minecraft:"):
                assert full.split(":")[1] in self.items, (self.v, p, full)
            if "parent" in a:
                assert a["parent"].split(":")[1] in self.advs, (p, a["parent"])
        folder = "advancement" if self.new else "advancements"
        tag = self.v.replace(".", "")
        if self.v26:
            for a in self.advs.values():
                fix26(a)
        # v<tag>: everything (mod jar); dp<tag>: vanilla only (datapack); 26.x: one mod jar folder per loader
        if self.v26:
            roots = [(f"v{tag}n", False, "neoforge"), (f"v{tag}f", False, "fabric")]
        elif self.new:
            roots = [(f"v{tag}", False, "neoforge"), (f"v{tag}f", False, "fabric")]
        else:
            roots = [(f"v{tag}", False, None)]
        for root, only_vanilla, loader in roots + [(f"dp{tag}", True, None)]:
            base = os.path.join(OUT, root, "data", NS, folder)
            for p, a in self.advs.items():
                if only_vanilla and p not in self.vanilla_paths:
                    continue
                if loader == "fabric" and not fabric_ok(a):
                    continue  # counters that the Fabric build does not keep
                if loader:
                    a = loader_cond(a, loader)
                fp = os.path.join(base, p + ".json")
                os.makedirs(os.path.dirname(fp), exist_ok=True)
                with open(fp, "w", encoding="utf-8") as f:
                    json.dump(a, f, ensure_ascii=False, separators=(",", ":"))
        return len(self.advs)


# ---------------- mod integration ----------------
MODS_INFO = {  # ns: (modid, ja, en, preferred icons, background, group)
    "mekanism": ("mekanism", "Mekanism", "Mekanism", ["metallurgic_infuser", "steel_casing", "ingot_osmium"], "minecraft:textures/block/iron_block.png", "工業"),
    "mekanismgenerators": ("mekanismgenerators", "Mekanism Generators", "Mekanism Generators", ["heat_generator", "solar_generator"], "minecraft:textures/block/iron_block.png", "工業"),
    "mekanismtools": ("mekanismtools", "Mekanism Tools", "Mekanism Tools", ["osmium_pickaxe", "steel_sword"], "minecraft:textures/block/iron_block.png", "工業"),
    "mekanismadditions": ("mekanismadditions", "Mekanism Additions", "Mekanism Additions", ["red_balloon", "white_plastic"], "minecraft:textures/block/white_concrete.png", "工業"),
    "create": ("create", "Create", "Create", ["cogwheel", "andesite_alloy", "brass_ingot"], "minecraft:textures/block/spruce_planks.png", "工業"),
    "ae2": ("ae2", "Applied Energistics 2", "Applied Energistics 2", ["controller", "certus_quartz_crystal", "fluix_crystal"], "minecraft:textures/block/smooth_stone.png", "工業"),
    "immersiveengineering": ("immersiveengineering", "Immersive Engineering", "Immersive Engineering", ["hammer", "ingot_steel", "coke"], "minecraft:textures/block/bricks.png", "工業"),
    "twilightforest": ("twilightforest", "黄昏の森", "The Twilight Forest", ["twilight_portal_miniature_structure", "naga_scale", "raven_feather", "liveroot"], "minecraft:textures/block/podzol_top.png", "冒険"),
    "aether": ("aether", "エーテル", "The Aether", ["golden_parachute", "ambrosium_shard", "zanite_gemstone", "skyroot_planks"], "minecraft:textures/block/snow.png", "冒険"),
    "alexsmobs": ("alexsmobs", "Alex's Mobs", "Alex's Mobs", ["animal_dictionary", "banana", "bear_fur"], "minecraft:textures/block/grass_block_top.png", "冒険"),
    "irons_spellbooks": ("irons_spellbooks", "Iron's Spells 'n Spellbooks", "Iron's Spells 'n Spellbooks", ["gold_spell_book", "copper_spell_book", "arcane_essence"], "minecraft:textures/block/purpur_block.png", "冒険"),
    "botania": ("botania", "Botania", "Botania", ["lexicon", "manasteel_ingot", "mana_pool"], "minecraft:textures/block/moss_block.png", "魔法"),
    "ars_nouveau": ("ars_nouveau", "Ars Nouveau", "Ars Nouveau", ["novice_spell_book", "source_gem", "worn_notebook"], "minecraft:textures/block/amethyst_block.png", "魔法"),
    "farmersdelight": ("farmersdelight", "Farmer's Delight", "Farmer's Delight", ["cooking_pot", "tomato", "cabbage"], "minecraft:textures/block/composter_side.png", "その他"),
}
MOD_EXCLUDE_KW = ["creative", "debug", "fake_", "dummy", "spawn_egg", "_fluid_block", "boss_spawner", "_spawner", "test_", "missing"]
MOD_BOSSES = {"naga", "lich", "hydra", "ur_ghast", "snow_queen", "knight_phantom", "minoshroom", "alpha_yeti",
              "slider", "valkyrie_queen", "sun_spirit", "gaia_guardian", "dead_king", "fire_boss", "wildfire", "wilden_boss",
              "fire_dragon", "ice_dragon", "lightning_dragon", "cyclops", "gorgon", "sea_serpent", "dread_lich",
              "chaos_guardian", "tremorzilla", "luxtructosaurus"}
MOD_ITEM_CAP = 400  # mods with thousands of block variants (Chipped, Rechiseled...) are sampled evenly
AUTO_BGS = ["minecraft:textures/block/stone.png", "minecraft:textures/block/oak_planks.png", "minecraft:textures/block/bricks.png",
            "minecraft:textures/block/smooth_stone.png", "minecraft:textures/block/spruce_planks.png", "minecraft:textures/block/deepslate.png",
            "minecraft:textures/block/moss_block.png", "minecraft:textures/block/sandstone.png"]
MOD_DIMS = {"twilightforest": [("twilightforest:twilight_forest", "黄昏の森へ", "Into the Twilight", "黄昏の森に行きましょう", "Enter the Twilight Forest")],
            "aether": [("aether:the_aether", "天空の楽園", "Paradise Above", "エーテルに行きましょう", "Enter the Aether")]}
MODDATA = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "modsdata.json"), encoding="utf-8"))



# ---------------- items that come in steps ----------------
TIER_WORDS = ["primitive", "crude", "wooden", "wood", "leather", "stone", "basic", "simple", "starter", "copper", "common",
              "standard", "normal", "iron", "bronze", "improved", "hardened", "reinforced", "silver", "gold", "golden",
              "advanced", "enhanced", "uncommon", "steel", "signalum", "lumium", "emerald", "diamond", "elite", "rare", "epic",
              "resonant", "enderium", "obsidian", "netherite", "ultimate", "legendary", "allthemodium", "vibranium",
              "unobtainium", "insane", "supreme", "infinity", "creative"]
TIER_RANK = {w: n for n, w in enumerate(TIER_WORDS)}
ROW_MAX = 16


def tier_of(tok):
    """rank of one word of an item id (None if it is not a step word)"""
    if tok in TIER_RANK:
        return TIER_RANK[tok] * 100
    m = re.fullmatch(r"(?:tier|t|mk|lv|level)?(\d{1,2})", tok)
    if m:
        return 10000 + int(m.group(1))
    return None


def item_units(ids, items):
    """[("row", [ids in step order]) | ("group", [ids])] -- every id once"""
    fam = {}
    for i in ids:
        toks = i.split("_")
        rank = [tier_of(t) for t in toks]
        base = "_".join(t for t, r in zip(toks, rank) if r is None)
        if base:
            fam.setdefault(base, []).append((sum(r for r in rank if r is not None), i, any(r is not None for r in rank)))
    used, units = set(), []
    for base in sorted(fam):
        members = fam[base]
        if len(members) < 2 or sum(1 for m in members if m[2]) < 1 or len({m[0] for m in members}) < len(members):
            continue  # not a ladder (or two items on the same step)
        g = [i for _, i, _ in sorted(members)]
        for k in range(0, len(g), ROW_MAX):
            units.append(("row", g[k:k + ROW_MAX]))
        used.update(g)
    ups = [i for i in ids if i not in used and "upgrade" in i]
    for k in range(0, len(ups), ROW_MAX):
        units.append(("row", ups[k:k + ROW_MAX]))
    used.update(ups)
    rest = [i for i in ids if i not in used]
    for k in range(0, len(rest), 12):
        g = sorted(rest[k:k + 12], key=lambda i: (items[i] != "item", i))  # prefer a real item (not a block) as the group head
        units.append(("group", g))
    order = {i: n for n, i in enumerate(ids)}
    units.sort(key=lambda u: min(order[i] for i in u[1]))  # keep the tabs roughly in name order
    return units


def gen_mods(self):
    data = MODDATA.get(self.v, {})
    selected = MODDATA.get("_selected_" + self.v) or MODDATA.get("_selected", [])
    for ns in sorted(data, key=lambda n: (data[n]["rank"], n)):
        d = data[ns]
        if d["key"] not in selected:
            continue
        if ns in MODS_INFO:
            _, ja, en, prefer, bg, cat = MODS_INFO[ns]
        else:
            ja = en = d["name"]; prefer = []; bg = AUTO_BGS[d["rank"] % len(AUTO_BGS)]; cat = None
        modid = d["modid"]
        cond = mod_cond(self, modid)
        items = {i: k for i, k in d["items"].items() if not any(w in i for w in MOD_EXCLUDE_KW)}
        ids = sorted(items)
        if len(ids) > MOD_ITEM_CAP:
            step = len(ids) / MOD_ITEM_CAP
            ids = [ids[int(n * step)] for n in range(MOD_ITEM_CAP)]
            items = {i: items[i] for i in ids}
        egg_mobs = [e for e in d["entities"] if (f"{e}_spawn_egg" in d["items"] or f"spawn_egg_{e}" in d["items"] or e in MOD_BOSSES)]
        if not ids and not egg_mobs and not d["biomes"]:
            continue
        real_items = [i for i in ids if items[i] == "item"]
        icon = next((p for p in prefer if p in items), (real_items or ids or [None])[0])
        def mod_icon(i):
            return {"id": f"{ns}:{i}"} if self.new else {"item": f"{ns}:{i}"}
        def mod_ipred(i):
            return {"items": f"{ns}:{i}"} if self.new else {"items": [f"{ns}:{i}"]}
        def add(path, parent, iconspec, title, desc, criteria, frame="task", xp=None, bgx=None):
            if ns == "aether" and frame == "challenge":
                frame = "goal"  # the Aether sound plays instead of the vanilla challenge jingle
            p = self.add(path, parent, "stone", title, desc, criteria, frame=frame, xp=xp, bg=bgx,
                         chat=False if frame == "task" else None)
            a = self.advs[p]
            a["display"]["icon"] = iconspec
            a.update(cond)
            return p
        # items in steps (basic → advanced → elite → ultimate, tier 1 → 2 → 3, iron → diamond → netherite ...) are lined up
        # in a row in that order; other upgrades get a row of their own; the rest come in groups of 12 as before
        units = item_units(ids, items)
        chunks, cur = [], []
        for u in units:
            if cur and sum(len(x[1]) for x in cur) + len(u[1]) > 150:
                chunks.append(cur); cur = []
            cur.append(u)
        if cur:
            chunks.append(cur)
        if len(chunks) > 1 and sum(len(x[1]) for x in chunks[-1]) < 40:
            chunks[-2].extend(chunks.pop())
        roots = []
        for ci, chunk in enumerate(chunks or [[]]):
            tkey = f"mod_{ns}" + ("" if ci == 0 else str(ci + 1))
            sfx = "" if ci == 0 else f" {ci + 1}"
            t = L(f"{NS}.tab.{tkey}", f"どっさり：{ja}{sfx}", f"Tons: {en}{sfx}")
            ds = L(f"{NS}.tab.{tkey}.desc", f"{ja}（{cat}系）の進捗がどっさり詰まっています" if cat else f"{ja}の進捗がどっさり詰まっています", f"Lots of {en} advancements")
            first = chunk[0][1][0] if chunk else None
            root_icon = (icon if ci == 0 else first) if (icon or first) else None
            r = add(f"{tkey}/_root", None, mod_icon(root_icon) if root_icon else self.icon("book"), tr(t), tr(ds),
                    {"tick": {"trigger": "minecraft:tick"}}, xp=0, bgx=bg)
            self.advs[r]["display"]["show_toast"] = False
            self.advs[r]["display"]["announce_to_chat"] = False
            roots.append(r)
            for kind, g in chunk:
                head = None
                prev = r
                for n, i in enumerate(g):
                    frame = "challenge" if any(w in i for w in ("netherite", "ultimate", "infinity", "gaia", "terrasteel")) else (
                        "goal" if any(w in i for w in ("elite", "trophy", "_core", "diamond", "atomic", "fusion", "fission", "spell_book")) else "task")
                    if kind == "row" and n == len(g) - 1 and len(g) >= 3 and frame == "task":
                        frame = "goal"  # the last step of a row
                    key = f"{items[i]}.{ns}.{i}"
                    parent = prev if kind == "row" else (head or r)
                    p = add(f"{tkey}/{i}", parent, mod_icon(i), vtr(key), tr(f"{NS}.obtain.desc", vtr(key)),
                            {"get": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [mod_ipred(i)]}}}, frame=frame)
                    head = head or p
                    prev = p
        r0 = roots[0]
        # mobs
        prev = None
        for mi, e in enumerate(egg_mobs):
            egg = f"{e}_spawn_egg" if f"{e}_spawn_egg" in d["items"] else (f"spawn_egg_{e}" if f"spawn_egg_{e}" in d["items"] else None)
            ic = mod_icon(egg) if egg else self.icon("netherite_sword")
            name = vtr(f"entity.{ns}.{e}")
            boss = e in MOD_BOSSES
            prev = add(f"mod_{ns}/kill_{e}", r0 if (mi % 10 == 0 or prev is None) else prev, ic,
                       tr(f"{NS}.kill.title", name), tr(f"{NS}.kill.desc", name),
                       {"kill": {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {"type": f"{ns}:{e}"}}}},
                       frame="challenge" if boss else "task", xp=100 if boss else None)
            if not boss:
                kp = prev
                for ti, n in enumerate([10, 100]):
                    kp = add(f"mod_{ns}/killn_{e}_{n}", kp, ic, tr(f"{NS}.killn.title", name, n), tr(f"{NS}.killn.desc", name, n),
                             {"s": {"trigger": "minecraft:tick", "conditions": {"player": self.player_stat("killed", f"{ns}:{e}", n)}}},
                             frame=["goal", "challenge"][ti], xp=[10, 30][ti])
        # biomes
        prev = None
        for bi, b in enumerate(d["biomes"]):
            if ns == "ae2":
                continue
            name = vtr(f"biome.{ns}.{b}")
            loc = {"biomes": f"{ns}:{b}"} if self.new else {"biome": f"{ns}:{b}"}
            prev = add(f"mod_{ns}/biome_{b}", r0 if (bi % 8 == 0 or prev is None) else prev, self.icon("filled_map"),
                       name, tr(f"{NS}.biome.desc", name),
                       {"v": {"trigger": "minecraft:location", "conditions": {"player": {"location": loc}}}})
        for dim, dja, den, dd, ddn in MOD_DIMS.get(ns, []):
            k = dim.split(":")[1]
            t = L(f"{NS}.dim.{k}", dja, den); dsk = L(f"{NS}.dim.{k}.desc", dd, ddn)
            add(f"mod_{ns}/dim_{k}", r0, self.icon("ender_eye"), tr(t), tr(dsk),
                {"d": {"trigger": "minecraft:changed_dimension", "conditions": {"to": dim}}}, frame="goal")


Gen.gen_mods = gen_mods


# ---------------- "do something N times" ----------------
CRAFTED = [  # item, tiers
    ("stick", [100, 1000, 10000]), ("torch", [100, 1000, 10000]), ("crafting_table", [10, 100]), ("chest", [10, 100, 1000]),
    ("furnace", [10, 100]), ("oak_planks", [100, 1000, 10000]), ("spruce_planks", [100, 1000]), ("birch_planks", [100, 1000]),
    ("bread", [10, 100, 1000]), ("cake", [1, 10, 100]), ("cookie", [100, 1000]), ("pumpkin_pie", [10, 100]),
    ("golden_apple", [1, 10, 100]), ("golden_carrot", [10, 100, 1000]), ("iron_ingot", [100, 1000, 10000]),
    ("gold_ingot", [100, 1000]), ("copper_ingot", [100, 1000]), ("netherite_ingot", [1, 10, 64]), ("glass", [100, 1000, 10000]),
    ("stone", [100, 1000, 10000]), ("smooth_stone", [100, 1000]), ("bricks", [100, 1000]), ("stone_bricks", [100, 1000, 10000]),
    ("cooked_beef", [10, 100, 1000]), ("cooked_porkchop", [10, 100, 1000]), ("cooked_chicken", [10, 100]), ("baked_potato", [10, 100, 1000]),
    ("rail", [100, 1000]), ("powered_rail", [10, 100]), ("redstone_torch", [10, 100]), ("repeater", [10, 100]), ("piston", [10, 100]),
    ("hopper", [10, 100]), ("ladder", [100, 1000]), ("bookshelf", [10, 100]), ("book", [10, 100]), ("paper", [100, 1000]),
    ("arrow", [100, 1000]), ("bow", [1, 10]), ("shield", [1, 10]), ("bucket", [1, 10]), ("bone_meal", [100, 1000]),
    ("white_bed", [1, 10]), ("lantern", [10, 100]), ("scaffolding", [100, 1000]), ("diamond_pickaxe", [1, 10]),
    ("iron_pickaxe", [1, 10, 100]), ("beacon", [1, 5]), ("firework_rocket", [100, 1000]), ("tnt", [10, 100, 1000]),
    ("ender_eye", [12, 100]), ("enchanting_table", [1, 5]), ("anvil", [1, 10]), ("observer", [10, 100]),
    ("item_frame", [10, 100]), ("armor_stand", [1, 10]), ("lead", [1, 10]), ("crafter", [1, 10]),
]
USED = [
    ("firework_rocket", [10, 100, 1000]), ("bone_meal", [100, 1000, 10000]), ("snowball", [100, 1000]), ("egg", [100, 1000]),
    ("flint_and_steel", [10, 100]), ("water_bucket", [10, 100, 1000]), ("lava_bucket", [10, 100]), ("bucket", [10, 100, 1000]),
    ("bow", [100, 1000, 10000]), ("crossbow", [100, 1000]), ("trident", [10, 100]), ("fishing_rod", [10, 100]),
    ("potion", [10, 100]), ("splash_potion", [10, 100]), ("milk_bucket", [1, 10]), ("honey_bottle", [10, 100]),
    ("golden_apple", [10, 100]), ("enchanted_golden_apple", [1, 10]), ("goat_horn", [10, 100]), ("spyglass", [10, 100]),
    ("name_tag", [1, 10]), ("lead", [10, 100]), ("saddle", [1, 10]), ("fire_charge", [10, 100]), ("wind_charge", [10, 100]),
    ("experience_bottle", [100, 1000]), ("ender_eye", [12, 100]), ("shears", [100, 1000]), ("brush", [10, 100]),
    ("wooden_pickaxe", [100]), ("stone_pickaxe", [100, 1000]), ("iron_pickaxe", [1000, 10000]), ("diamond_pickaxe", [1000, 10000, 100000]),
    ("netherite_pickaxe", [1000, 10000, 100000]), ("iron_shovel", [1000]), ("diamond_shovel", [1000, 10000]), ("diamond_axe", [100, 1000]),
    ("iron_sword", [100, 1000]), ("diamond_sword", [100, 1000, 10000]), ("netherite_sword", [100, 1000, 10000]), ("mace", [10, 100]),
    ("iron_hoe", [100, 1000]), ("diamond_hoe", [100, 1000]),
]
BROKEN = [
    ("wooden_pickaxe", [1, 10]), ("stone_pickaxe", [1, 10]), ("iron_pickaxe", [1, 10]), ("netherite_pickaxe", [1]),
    ("wooden_sword", [1, 10]), ("stone_sword", [1, 10]), ("iron_sword", [1, 10]), ("diamond_sword", [1, 10]),
    ("iron_shovel", [1, 10]), ("iron_axe", [1, 10]), ("iron_hoe", [1]), ("bow", [1, 10]), ("shield", [1, 10]),
    ("fishing_rod", [1, 10]), ("flint_and_steel", [1, 10]), ("shears", [1, 10]), ("iron_helmet", [1]), ("iron_boots", [1]),
    ("diamond_chestplate", [1]), ("elytra", [1]),
]
KILLED_BY = ["zombie", "skeleton", "creeper", "spider", "enderman", "witch", "drowned", "husk", "stray", "phantom", "slime",
             "magma_cube", "blaze", "ghast", "piglin", "piglin_brute", "hoglin", "wither_skeleton", "guardian", "elder_guardian",
             "pillager", "vindicator", "evoker", "ravager", "vex", "silverfish", "bee", "wolf", "iron_golem", "polar_bear",
             "goat", "pufferfish", "llama", "breeze", "bogged", "warden", "wither", "ender_dragon", "shulker", "zombified_piglin"]
# stats counted by this mod's own code (tonsofadv:...) — mod jar only, never in the datapack
JAVA_STATS = [  # tab, stat, tiers, ja, en, desc ja, desc en, icon
    ("craft", "crafted_total", [1, 100, 1000, 10000, 100000], "クラフト職人", "Crafter", "合計%s回クラフトしましょう", "Craft %s times in total", "crafting_table"),
    ("craft", "smelted_total", [64, 1000, 10000, 100000], "かまど番長", "Smelting Boss", "かまどから合計%s個取り出しましょう", "Take %s smelted items out of furnaces", "furnace"),
    ("stats", "blocks_placed", [100, 1000, 10000, 100000, 1000000], "建築家", "Builder", "ブロックを合計%s個置きましょう", "Place %s blocks in total", "bricks"),
    ("stats", "blocks_broken", [100, 1000, 10000, 100000, 1000000], "解体屋", "Demolisher", "ブロックを合計%s個壊しましょう", "Break %s blocks in total", "iron_pickaxe"),
    ("stats", "xp_gained", [1000, 10000, 100000, 1000000], "経験豊富", "Experienced", "経験値を合計%s集めましょう", "Collect %s experience in total", "experience_bottle"),
    ("stats", "max_level", [10, 30, 50, 100, 1000], "レベルアップ", "Level Up", "レベル%sになりましょう", "Reach level %s", "enchanted_book"),
    ("stats", "food_eaten", [100, 1000, 10000], "大食い", "Big Eater", "食べ物を合計%s回食べましょう", "Eat %s times in total", "cooked_beef"),
    ("stats", "potions_drunk", [10, 100, 1000], "ポーション中毒", "Potion Lover", "ポーションを合計%s本飲みましょう", "Drink %s potions", "potion"),
    ("stats", "arrows_shot", [100, 1000, 10000], "弓の名手", "Archer", "弓を%s回引きましょう", "Loose %s arrows", "bow"),
    ("stats", "animals_tamed", [5, 50, 500], "動物使い", "Beast Tamer", "動物を合計%s回手なずけましょう", "Tame %s animals", "bone"),
    ("stats", "advancements_earned", [100, 500, 1000, 5000, 10000], "進捗の進捗", "Advancement Advancement", "進捗を合計%s個達成しましょう", "Earn %s advancements", "knowledge_book"),
    ("stats", "chat_messages", [1, 100, 1000, 10000], "おしゃべり", "Chatterbox", "チャットを%s回送りましょう", "Send %s chat messages", "writable_book"),
    ("stats", "commands_used", [1, 100, 1000], "コマンド使い", "Commander", "コマンドを%s回使いましょう", "Run %s commands", "command_block"),
    ("stats", "dims_changed", [10, 100, 1000], "次元の旅人", "Dimension Hopper", "ディメンションを%s回移動しましょう", "Change dimensions %s times", "obsidian"),
    ("stats", "crit_hits", [10, 100, 1000, 10000], "会心の一撃", "Critical Hit", "クリティカルを%s回出しましょう", "Land %s critical hits", "diamond_sword"),
    ("stats", "projectile_hits", [10, 100, 1000], "命中", "Bullseye", "飛び道具を生き物に%s回当てましょう", "Hit creatures with projectiles %s times", "arrow"),
    ("stats", "xp_orbs", [100, 1000, 10000], "経験値オーブ集め", "Orb Collector", "経験値オーブを%s個拾いましょう", "Pick up %s experience orbs", "experience_bottle"),
    ("stats", "crops_harvested", [100, 1000, 10000], "収穫祭", "Harvest Festival", "育った作物を%s回収穫しましょう", "Harvest %s grown crops", "wheat"),
    ("stats", "logs_chopped", [100, 1000, 10000], "きこり", "Lumberjack", "原木を%s個切りましょう", "Chop %s logs", "iron_axe"),
    ("stats", "ores_mined", [100, 1000, 10000], "鉱夫", "Miner", "鉱石を%s個掘りましょう", "Mine %s ores", "iron_ore"),
    ("stats", "entity_interactions", [100, 1000, 10000], "ふれあい", "Hands On", "生き物に%s回右クリックしましょう", "Interact with creatures %s times", "lead"),
    ("stats", "anvil_repairs", [10, 100, 1000], "金床の常連", "Anvil Regular", "金床で%s回仕上げましょう", "Use an anvil %s times", "anvil"),
]
IRONS_SCHOOLS = [("fire", "炎", "Fire", "fire_rune"), ("ice", "氷", "Ice", "ice_rune"), ("lightning", "雷", "Lightning", "lightning_rune"),
                 ("holy", "聖", "Holy", "holy_rune"), ("ender", "エンダー", "Ender", "ender_rune"), ("blood", "血", "Blood", "blood_rune"),
                 ("evocation", "召喚", "Evocation", "evocation_rune"), ("nature", "自然", "Nature", "nature_rune"), ("eldritch", "異界", "Eldritch", "arcane_rune")]
JAVA_MOD_STATS = [  # ns, stat, tiers, ja, en, desc ja, desc en, icon (in ns), tier labels (or None)
    ("irons_spellbooks", "irons_spells_cast", [1, 10, 100, 1000, 10000], "魔法使いの道", "Path of the Mage", "呪文を合計%s回唱えましょう", "Cast %s spells", "gold_spell_book", None),
    ("irons_spellbooks", "irons_scrolls_used", [1, 10, 100, 1000], "スクロール使い", "Scroll Reader", "スクロールを%s回使いましょう", "Use %s scrolls", "scroll", None),
    ("ars_nouveau", "ars_spells_cast", [1, 10, 100, 1000, 10000], "術式の使い手", "Glyph Caster", "呪文を合計%s回唱えましょう", "Cast %s spells", "novice_spell_book", None),
    ("ae2", "ae2_max_bytes", [1024, 4096, 16384, 65536, 262144, 1048576, 16777216], "デジタル倉庫", "Digital Hoard", "MEネットワークにしまったアイテムが%sバイトを超えるようにしましょう", "Store more than %s bytes in an ME network", None,
     ["1k", "4k", "16k", "64k", "256k", "1M", "16M"]),
    ("ae2", "ae2_max_items", [10000, 100000, 1000000, 100000000], "物量作戦", "Mass Storage", "MEネットワークにアイテムを合計%s個しまいましょう", "Store %s items in an ME network", "chest", None),
    ("ae2", "ae2_max_types", [100, 500, 1000, 5000], "なんでも倉庫", "Everything Store", "MEネットワークに%s種類のアイテムをしまいましょう", "Store %s kinds of items in an ME network", "terminal", None),
]
AE2_BYTE_ICONS = ["item_storage_cell_1k", "item_storage_cell_4k", "item_storage_cell_16k", "item_storage_cell_64k", "item_storage_cell_256k", "item_storage_cell_256k", "item_storage_cell_256k"]


STAT_NAMES = {
    "crafted_total": ("クラフトした回数", "Times Crafted"), "smelted_total": ("製錬して取り出した数", "Items Smelted"),
    "blocks_placed": ("置いたブロックの数", "Blocks Placed"), "blocks_broken": ("壊したブロックの数", "Blocks Broken"),
    "xp_gained": ("集めた経験値", "Experience Collected"), "max_level": ("最高レベル", "Highest Level"),
    "food_eaten": ("食べた回数", "Times Eaten"), "potions_drunk": ("飲んだポーションの数", "Potions Drunk"),
    "arrows_shot": ("弓を引いた回数", "Arrows Loosed"), "animals_tamed": ("手なずけた動物の数", "Animals Tamed"),
    "advancements_earned": ("達成した進捗の数", "Advancements Earned"),
    "chat_messages": ("送ったチャットの数", "Chat Messages Sent"), "commands_used": ("使ったコマンドの数", "Commands Run"),
    "dims_changed": ("ディメンションを移動した回数", "Dimension Changes"), "crit_hits": ("クリティカルの数", "Critical Hits"),
    "projectile_hits": ("飛び道具を当てた数", "Projectile Hits"), "xp_orbs": ("拾った経験値オーブの数", "Experience Orbs Picked Up"),
    "crops_harvested": ("収穫した作物の数", "Crops Harvested"), "logs_chopped": ("切った原木の数", "Logs Chopped"),
    "ores_mined": ("掘った鉱石の数", "Ores Mined"), "entity_interactions": ("生き物を右クリックした回数", "Creature Interactions"),
    "anvil_repairs": ("金床で仕上げた回数", "Anvil Uses"),
    "irons_spells_cast": ("Iron's Spells: 唱えた呪文の数", "Iron's Spells: Spells Cast"),
    "irons_scrolls_used": ("Iron's Spells: 使ったスクロールの数", "Iron's Spells: Scrolls Used"),
    "ars_spells_cast": ("Ars Nouveau: 唱えた呪文の数", "Ars Nouveau: Spells Cast"),
    "ae2_max_bytes": ("AE2: MEネットワークの最大使用バイト", "AE2: Most Bytes Stored"),
    "ae2_max_items": ("AE2: MEネットワークの最大アイテム数", "AE2: Most Items Stored"),
    "ae2_max_types": ("AE2: MEネットワークの最大種類数", "AE2: Most Item Types Stored"),
}


def gen_actions_vanilla(self):
    L(f"{NS}.configuration.title", "進捗どっさりの設定", "Tons of Advancements Settings")
    L(f"{NS}.configuration.specialSounds", "特別な進捗の音", "Special advancement sounds")
    L(f"{NS}.configuration.specialSounds.tooltip", "エーテルなどのタブの進捗で、そのMODの特別な音を鳴らすよ（オフにすると普通の音になるよ）",
      "Play the special sound of mods like the Aether for their tabs (off = normal sound)")
    L(f"{NS}.configuration.soundsAsRecords", "エーテルの音をジュークボックスの音量で", "Aether sounds use Jukebox volume")
    L(f"{NS}.configuration.soundsAsRecords.tooltip", "エーテルの進捗の音の大きさを「ジュークボックス/音ブロック」の音量で変えられるよ（オフにすると「音楽」の音量になるよ）",
      "Aether advancement sounds follow the Jukebox/Note Blocks volume (off = Music volume)")
    L(f"{NS}.crafted.title", "%s職人 ×%s", "%s Crafter ×%s")
    L(f"{NS}.crafted.title1", "%s職人", "%s Crafter")
    L(f"{NS}.used.title1", "%sの使い手", "%s User")
    L(f"{NS}.broken.title1", "使いつぶし：%s", "Worn Out: %s")
    L(f"{NS}.crafted.desc", "%sを%s個作りましょう（製錬も含みます）", "Make %2$s %1$s (smelting counts)")
    L(f"{NS}.used.title", "%sの使い手 ×%s", "%s User ×%s")
    L(f"{NS}.used.desc", "%sを%s回使いましょう", "Use %s %s times")
    L(f"{NS}.broken.title", "使いつぶし：%s ×%s", "Worn Out: %s ×%s")
    L(f"{NS}.broken.desc", "%sを%s回壊しましょう（耐久切れ）", "Break %s %s times (durability)")
    L(f"{NS}.killedby.title", "%sにやられた", "Killed by %s")
    L(f"{NS}.killedby.desc", "%sに倒されましょう", "Get killed by %s")
    L(f"{NS}.killedby10.title", "%sにやられまくった", "Killed by %s Again and Again")
    L(f"{NS}.killedby10.desc", "%sに10回倒されましょう", "Get killed by %s 10 times")
    TABS["craft"] = ("クラフト・製錬", "Crafting & Smelting", "crafting_table", "minecraft:textures/block/crafting_table_front.png")
    TABS["use"] = ("使いこみ", "Well Used", "diamond_pickaxe", "minecraft:textures/block/anvil.png")
    TABS["deaths"] = ("やられ図鑑", "Death Log", "skeleton_skull", "minecraft:textures/block/soul_sand.png")
    rc, ru, rd = self.root("craft"), self.root("use"), self.root("deaths")
    def chain(tab, root, kind, stype, lst):
        for it, tiers in lst:
            if it not in self.items:
                continue
            prev = root
            name = self.plain_item_name(it)
            for ti, n in enumerate(tiers):
                frame = "challenge" if ti >= 2 else ("goal" if ti == 1 else "task")
                prev = self.add(f"{tab}/{kind}_{it}_{n}", prev, it,
                                tr(f"{NS}.{kind}.title1", name) if n == 1 else tr(f"{NS}.{kind}.title", name, f"{n:,}"),
                                tr(f"{NS}.{kind}.desc", name, f"{n:,}"),
                                {"s": {"trigger": "minecraft:tick", "conditions": {"player": self.player_stat(stype, it, n)}}},
                                frame=frame, xp=[5, 20, 50][min(ti, 2)])
    chain("craft", rc, "crafted", "crafted", CRAFTED)
    chain("use", ru, "used", "used", USED)
    chain("use", ru, "broken", "broken", BROKEN)
    for i, m in enumerate([m for m in KILLED_BY if m in self.ents]):
        p = self.add(f"deaths/{m}", rd, self.mob_icon(m), tr(f"{NS}.killedby.title", self.ent(m)), tr(f"{NS}.killedby.desc", self.ent(m)),
                     {"s": {"trigger": "minecraft:tick", "conditions": {"player": self.player_stat("killed_by", m, 1)}}}, frame="task", chat=False)
        self.add(f"deaths/{m}_10", p, self.mob_icon(m), tr(f"{NS}.killedby10.title", self.ent(m)), tr(f"{NS}.killedby10.desc", self.ent(m)),
                 {"s": {"trigger": "minecraft:tick", "conditions": {"player": self.player_stat("killed_by", m, 10)}}}, frame="goal")


def own_stat(self, stat, mn):
    return {"type_specific": {"type": "minecraft:player" if self.new else "player",
                              "stats": [{"type": "minecraft:custom", "stat": f"{NS}:{stat}", "value": {"min": mn}}]}}


def gen_actions_java(self):
    for tab, stat, tiers, ja, en, dja, den, icon in JAVA_STATS:
        L(f"stat.{NS}.{stat}", *STAT_NAMES[stat])
        t = L(f"{NS}.jstat.{stat}", f"{ja} Lv.%s", f"{en} Lv.%s"); d = L(f"{NS}.jstat.{stat}.desc", dja, den)
        prev = f"{tab}/root"
        for ti, n in enumerate(tiers):
            prev = self.add(f"{tab}/j_{stat}_{ti + 1}", prev, icon, tr(t, ti + 1), tr(d, f"{n:,}"),
                            {"s": {"trigger": "minecraft:tick", "conditions": {"player": own_stat(self, stat, n)}}},
                            frame=FRAMES_BY_TIER[min(ti, 4)], xp=[5, 15, 40, 100, 200][min(ti, 4)])
    data = MODDATA.get(self.v, {})
    rows = list(JAVA_MOD_STATS) + [("irons_spellbooks", f"irons_school_{k}", [10, 100, 1000], f"{ja}の魔法使い", f"{en} Mage",
                                    f"{ja}の呪文を%s回唱えましょう", f"Cast %s {en} spells", rune, None) for k, ja, en, rune in IRONS_SCHOOLS]
    for ns, stat, tiers, ja, en, dja, den, icon, labels in rows:
        if ns not in data or f"mod_{ns}/_root" not in self.advs:
            continue
        modid = data[ns]["modid"]
        cond = mod_cond(self, modid)
        if stat.startswith("irons_school_"):
            L(f"stat.{NS}.{stat}", f"Iron's Spells: {ja.replace('の魔法使い', '')}の呪文を唱えた数", f"Iron's Spells: {en.replace(' Mage', '')} Spells Cast")
        else:
            L(f"stat.{NS}.{stat}", *STAT_NAMES[stat])
        t = L(f"{NS}.jstat.{stat}", f"{ja} Lv.%s", f"{en} Lv.%s"); d = L(f"{NS}.jstat.{stat}.desc", dja, den)
        prev = f"mod_{ns}/_root"
        for ti, n in enumerate(tiers):
            ic = icon if icon else AE2_BYTE_ICONS[ti]
            if ic not in data[ns]["items"]:
                ic = next(iter(data[ns]["items"]))
            shown = labels[ti] if labels else f"{n:,}"
            p = self.add(f"mod_{ns}/j_{stat}_{ti + 1}", prev, "stone", tr(t, ti + 1), tr(d, shown),
                         {"s": {"trigger": "minecraft:tick", "conditions": {"player": own_stat(self, stat, n)}}},
                         frame=FRAMES_BY_TIER[min(ti, 4)], xp=[5, 15, 40, 100, 200][min(ti, 4)])
            self.advs[p]["display"]["icon"] = {"id": f"{ns}:{ic}"} if self.new else {"item": f"{ns}:{ic}"}
            self.advs[p].update(cond)
            prev = p


Gen.gen_actions_vanilla = gen_actions_vanilla
Gen.gen_actions_java = gen_actions_java


# ---------------- v1.6.0: an advancement for (almost) every action ----------------
ENCH_MAX = {"protection": 4, "fire_protection": 4, "feather_falling": 4, "blast_protection": 4, "projectile_protection": 4,
            "respiration": 3, "thorns": 3, "depth_strider": 3, "frost_walker": 2, "soul_speed": 3, "swift_sneak": 3,
            "sharpness": 5, "smite": 5, "bane_of_arthropods": 5, "knockback": 2, "fire_aspect": 2, "looting": 3, "sweeping": 3,
            "sweeping_edge": 3, "efficiency": 5, "unbreaking": 3, "fortune": 3, "power": 5, "punch": 2, "luck_of_the_sea": 3,
            "lure": 3, "loyalty": 3, "impaling": 5, "riptide": 3, "quick_charge": 3, "piercing": 4, "density": 5, "breach": 4,
            "wind_burst": 3}
FISHED = ["cod", "salmon", "tropical_fish", "pufferfish", "enchanted_book", "name_tag", "saddle", "nautilus_shell", "bow",
          "fishing_rod", "lily_pad", "bowl", "leather_boots", "rotten_flesh", "stick", "string", "ink_sac", "tripwire_hook",
          "bone", "bamboo", "cocoa_beans", "potion"]
TRADED = ["enchanted_book", "name_tag", "bell", "filled_map", "diamond_sword", "diamond_pickaxe", "diamond_axe", "diamond_chestplate",
          "diamond_helmet", "iron_sword", "iron_chestplate", "bread", "pumpkin_pie", "cooked_chicken", "arrow", "glass", "bookshelf",
          "lantern", "clock", "compass", "ender_pearl", "experience_bottle", "redstone", "lapis_lazuli", "glowstone", "painting",
          "white_wool", "white_bed", "shears", "cake", "golden_carrot", "glistering_melon_slice", "emerald", "sea_pickle",
          "blue_ice", "packed_ice", "nautilus_shell", "pointed_dripstone", "small_dripleaf", "moss_block", "rooted_dirt",
          "mutton", "cooked_porkchop", "rabbit_stew", "suspicious_stew", "quartz_block", "terracotta", "crossbow", "shield",
          "iron_axe", "bell", "tipped_arrow"]
DAMAGE = [("is_fire", "あちっ", "Ouch, Hot", "火でダメージを受けよう", "Take fire damage", "blaze_powder"),
          ("is_fall", "着地失敗", "Bad Landing", "落下ダメージを受けよう", "Take fall damage", "feather"),
          ("is_explosion", "ドカーン", "Kaboom", "爆発でダメージを受けよう", "Take explosion damage", "tnt"),
          ("is_projectile", "被弾", "Hit!", "飛び道具でダメージを受けよう", "Get hit by a projectile", "arrow"),
          ("is_lightning", "ビリビリ", "Zap", "雷に打たれよう", "Get struck by lightning", "lightning_rod"),
          ("is_freezing", "こごえる", "Freezing", "粉雪で凍えよう", "Freeze in powder snow", "powder_snow_bucket"),
          ("is_drowning", "ごぼごぼ", "Glub Glub", "おぼれてダメージを受けよう", "Take drowning damage", "water_bucket")]
VEHICLES = ["horse", "donkey", "mule", "pig", "strider", "camel", "llama", "trader_llama", "skeleton_horse", "zombie_horse",
            "boat", "chest_boat", "minecart", "oak_boat", "oak_chest_boat", "bamboo_raft", "happy_ghast", "nautilus", "camel_husk"]
VEHICLE_ICON = {"boat": "oak_boat", "chest_boat": "oak_chest_boat", "minecart": "minecart", "oak_boat": "oak_boat",
                "oak_chest_boat": "oak_chest_boat", "bamboo_raft": "bamboo_raft"}
AXES = ["wooden_axe", "stone_axe", "iron_axe", "golden_axe", "diamond_axe", "netherite_axe"]
HOES = ["wooden_hoe", "stone_hoe", "iron_hoe", "golden_hoe", "diamond_hoe", "netherite_hoe"]
SHOVELS = ["wooden_shovel", "stone_shovel", "iron_shovel", "golden_shovel", "diamond_shovel", "netherite_shovel"]
DYES = [f"{c}_dye" for c in COLORS]
NO_EFFECT = {"instant_health", "instant_damage"}


def chunked(self, base, ja, en, icon, bg, entries, size=200):
    """entries: (path, icon, title, desc, criteria, frame, xp, chain?) — tabs base, base2, ... with groups of 10"""
    for ci in range(0, len(entries), size):
        part = entries[ci:ci + size]
        k = ci // size
        tkey = base if k == 0 else f"{base}{k + 1}"
        TABS[tkey] = (ja if k == 0 else f"{ja} {k + 1}", en if k == 0 else f"{en} {k + 1}", part[0][1], bg)
        r = self.root(tkey)
        prev = r
        for i, (path, ic, title, desc, crit, frame, xp) in enumerate(part):
            prev = self.add(f"{tkey}/{path}", r if i % 10 == 0 else prev, ic, title, desc, crit, frame=frame, xp=xp,
                            chat=False if frame == "task" else None)


def ioub(self, blocks, items):
    tool = {"items": [f"minecraft:{i}" for i in items]} if not self.new else {"items": [f"minecraft:{i}" for i in items] if len(items) > 1 else f"minecraft:{items[0]}"}
    return {"c": {"trigger": "minecraft:item_used_on_block", "conditions": {"location": [
        {"condition": "minecraft:location_check", "predicate": {"block": {"blocks": [f"minecraft:{b}" for b in blocks]}}},
        {"condition": "minecraft:match_tool", "predicate": tool}]}}}


def gen_more_vanilla(self):
    reg = self.reg
    statc = lambda stype, it, n: {"s": {"trigger": "minecraft:tick", "conditions": {"player": self.player_stat(stype, it, n)}}}
    L(f"{NS}.make1.title", "%sを作った", "Made %s"); L(f"{NS}.make1.desc", "%sを作ろう（製錬も入るよ）", "Make %s (smelting counts)")
    L(f"{NS}.place.title", "%s設置 ×%s", "%s Placer ×%s"); L(f"{NS}.place.desc", "%sを%s個置きましょう", "Place %2$s %1$s")
    L(f"{NS}.mineall.title", "%s採掘 ×%s", "%s Miner ×%s"); L(f"{NS}.mineall.desc", "%sを%s個掘りましょう", "Mine %2$s %1$s")
    L(f"{NS}.effect.title", "%s状態", "Under %s"); L(f"{NS}.effect.desc", "%sの効果を受けましょう", "Get the %s effect")
    L(f"{NS}.brew.title", "醸造：%s%s", "Brewed: %s%s"); L(f"{NS}.brew.desc", "%s%sを醸造しましょう", "Brew %s%s")
    L(f"{NS}.brew.long", "（延長）", " (Long)"); L(f"{NS}.brew.strong", "（強化）", " (Strong)"); L(f"{NS}.brew.none", "", "")
    L(f"{NS}.ench.title", "%sの力", "Power of %s"); L(f"{NS}.ench.desc", "%sが付いたアイテムかエンチャントの本を手に入れましょう", "Get an item or book with %s")
    L(f"{NS}.enchmax.title", "極めし%s %s", "Mastered %s %s"); L(f"{NS}.enchmax.desc", "%s %sが付いたアイテムかエンチャントの本を手に入れましょう", "Get an item or book with %s %s")
    L(f"{NS}.fish.title", "%sを釣り上げた", "Fished Up %s"); L(f"{NS}.fish.desc", "釣りで%sを釣り上げましょう", "Fish up %s")
    L(f"{NS}.trade.title", "%sを買った", "Bought %s"); L(f"{NS}.trade.desc", "村人との取引で%sを手に入れましょう", "Get %s by trading with a villager")
    L(f"{NS}.ride.title", "%sに乗った", "Rode a %s"); L(f"{NS}.ride.desc", "%sに乗りましょう", "Ride a %s")
    L(f"{NS}.hurtby.title", "%sの攻撃を受けた", "Attacked by %s"); L(f"{NS}.hurtby.desc", "%sに攻撃されましょう", "Get hurt by %s")
    L(f"{NS}.strip.title", "皮むき：%s", "Stripped: %s"); L(f"{NS}.strip.desc", "斧で%sの皮をはぎましょう", "Strip %s with an axe")
    L(f"{NS}.disc.title", "%sを聴いた", "Listened to %s"); L(f"{NS}.disc.desc", "ジュークボックスで%sをかけましょう", "Play %s in a jukebox")
    # make every craftable/smeltable item once
    recipe_items = set()
    for rid in reg["recipe"]:
        base = rid.split("_from_")[0]
        for cand in (rid, base):
            if cand in self.items:
                recipe_items.add(cand)
    crafted_tiered = {it for it, _ in CRAFTED}
    ent = []
    for it in sorted(recipe_items - crafted_tiered - EXCLUDE):
        n = self.plain_item_name(it)
        ent.append((f"make_{it}", it, tr(f"{NS}.make1.title", n), tr(f"{NS}.make1.desc", n), statc("crafted", it, 1), "task", 5))
    TABS_JA = ("作ってみた", "Made It")
    chunked(self, "make", *TABS_JA, "crafting_table", "minecraft:textures/block/crafting_table_top.png", ent)
    # place every block 64 times
    ent = []
    for it in sorted(self.items & self.blocks - EXCLUDE):
        if it.endswith(("_spawn_egg",)) or it in ("water", "lava", "fire", "soul_fire") or it.startswith("infested_"):
            continue
        n = self.plain_item_name(it)
        ent.append((f"place_{it}", it, tr(f"{NS}.place.title", n, 64), tr(f"{NS}.place.desc", n, 64), statc("used", it, 64), "task", 5))
    chunked(self, "place", "設置", "Placing", "bricks", "minecraft:textures/block/bricks.png", ent)
    # mine every block 100 times
    ent = []
    for it in sorted(self.items & self.blocks - EXCLUDE - set(MINE_TIERS)):
        if it.startswith("infested_") or it in ("dragon_egg", "end_portal", "nether_portal", "sculk_shrieker"):
            continue
        n = vtr(f"block.minecraft.{it}")
        ent.append((f"mine_{it}", it, tr(f"{NS}.mineall.title", n, 100), tr(f"{NS}.mineall.desc", n, 100), statc("mined", it, 100), "task", 5))
    chunked(self, "mineall", "掘りまくり", "Dig Everything", "iron_pickaxe", "minecraft:textures/block/cobblestone.png", ent)
    # experiences: effects, potions, enchantments, fishing, trading, damage, riding, using items on blocks
    ent = []
    for e in sorted(reg["mob_effect"]):
        if e in NO_EFFECT:
            continue
        n = vtr(f"effect.minecraft.{e}")
        ent.append((f"effect_{e}", "potion", tr(f"{NS}.effect.title", n), tr(f"{NS}.effect.desc", n),
                    {"c": {"trigger": "minecraft:effects_changed", "conditions": {"effects": {f"minecraft:{e}": {}}}}}, "task", 10))
    for pot in sorted(reg["potion"]):
        if pot in ("water", "empty"):
            continue
        b = pot.replace("long_", "").replace("strong_", "")
        sfx = "long" if pot.startswith("long_") else ("strong" if pot.startswith("strong_") else "none")
        n = vtr(f"item.minecraft.potion.effect.{b}")
        ent.append((f"brew_{pot}", "brewing_stand", tr(f"{NS}.brew.title", n, tr(f"{NS}.brew.{sfx}")), tr(f"{NS}.brew.desc", n, tr(f"{NS}.brew.{sfx}")),
                    {"c": {"trigger": "minecraft:brewed_potion", "conditions": {"potion": f"minecraft:{pot}"}}}, "task", 10))
    enchs = reg.get("enchantment", [])
    for en_ in sorted(enchs):
        n = vtr(f"enchantment.minecraft.{en_}")
        for lvl in sorted({1, ENCH_MAX.get(en_, 1)}):
            if self.new:
                a = {"predicates": {"minecraft:enchantments": [{"enchantments": f"minecraft:{en_}", "levels": {"min": lvl}}]}}
                b = {"items": "minecraft:enchanted_book", "predicates": {"minecraft:stored_enchantments": [{"enchantments": f"minecraft:{en_}", "levels": {"min": lvl}}]}}
            else:
                a = {"enchantments": [{"enchantment": f"minecraft:{en_}", "levels": {"min": lvl}}]}
                b = {"items": ["minecraft:enchanted_book"], "stored_enchantments": [{"enchantment": f"minecraft:{en_}", "levels": {"min": lvl}}]}
            crit = {"a": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [a]}},
                    "b": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [b]}}}
            if lvl == 1:
                ent.append((f"ench_{en_}", "enchanted_book", tr(f"{NS}.ench.title", n), tr(f"{NS}.ench.desc", n), crit, "task", 10))
            else:
                lv = vtr(f"enchantment.level.{lvl}")
                ent.append((f"ench_{en_}_{lvl}", "enchanted_book", tr(f"{NS}.enchmax.title", n, lv), tr(f"{NS}.enchmax.desc", n, lv), crit, "goal", 20))
    for it in FISHED:
        if it in self.items:
            n = self.plain_item_name(it)
            ent.append((f"fish_{it}", it, tr(f"{NS}.fish.title", n), tr(f"{NS}.fish.desc", n),
                        {"c": {"trigger": "minecraft:fishing_rod_hooked", "conditions": {"item": self.ipred(it)}}},
                        "goal" if it in ("enchanted_book", "name_tag", "saddle", "nautilus_shell") else "task", 10))
    for it in dict.fromkeys(TRADED):
        if it in self.items:
            n = self.plain_item_name(it)
            ent.append((f"trade_{it}", it, tr(f"{NS}.trade.title", n), tr(f"{NS}.trade.desc", n),
                        {"c": {"trigger": "minecraft:villager_trade", "conditions": {"item": self.ipred(it)}}}, "task", 10))
    for tag, ja, en, dja, den, ic in DAMAGE:
        t = L(f"{NS}.dmg.{tag}", ja, en); d = L(f"{NS}.dmg.{tag}.desc", dja, den)
        ent.append((f"dmg_{tag}", ic, tr(t), tr(d), {"c": {"trigger": "minecraft:entity_hurt_player", "conditions": {
            "damage": {"type": {"tags": [{"id": f"minecraft:{tag}", "expected": True}]}}}}}, "task", 5))
    for m in [m for m in dict.fromkeys(MOBS) if m in self.ents and m not in ("bat", "cod", "salmon", "squid", "glow_squid", "tadpole", "tropical_fish", "chicken", "cow", "pig", "sheep", "mooshroom", "rabbit", "turtle", "horse", "donkey", "mule", "camel", "sniffer", "armadillo", "allay", "frog", "ocelot", "cat", "parrot", "villager", "wandering_trader", "snow_golem", "fox", "skeleton_horse", "zombie_horse", "strider", "happy_ghast", "copper_golem", "nautilus", "camel_husk")]:
        n = self.ent(m)
        ent.append((f"hurtby_{m}", self.mob_icon(m), tr(f"{NS}.hurtby.title", n), tr(f"{NS}.hurtby.desc", n),
                    {"c": {"trigger": "minecraft:entity_hurt_player", "conditions": {"damage": {"source_entity": {"type": f"minecraft:{m}"}}}}}, "task", 5))
    for v in VEHICLES:
        if v in self.ents:
            n = self.ent(v)
            ent.append((f"ride_{v}", VEHICLE_ICON.get(v, self.mob_icon(v)), tr(f"{NS}.ride.title", n), tr(f"{NS}.ride.desc", n),
                        {"c": {"trigger": "minecraft:tick", "conditions": {"player": {"vehicle": {"type": f"minecraft:{v}"}}}}}, "task", 5))
    for w in WOODS:
        logs = [x for x in (f"{w}_log", f"{w}_wood", f"{w}_stem", f"{w}_hyphae", f"{w}_block") if x in self.blocks]
        if not logs:
            continue
        n = self.plain_item_name(logs[0])
        ent.append((f"strip_{w}", logs[0], tr(f"{NS}.strip.title", n), tr(f"{NS}.strip.desc", n), ioub(self, logs, AXES), "task", 5))
    singles = [
        ("till", "耕した", "Tilled", "クワで土を耕そう", "Till dirt with a hoe", "wooden_hoe", ["dirt", "grass_block", "dirt_path", "coarse_dirt", "rooted_dirt"], HOES),
        ("path", "道づくり", "Path Maker", "シャベルで道を作ろう", "Make a path with a shovel", "wooden_shovel", ["grass_block", "dirt", "podzol", "coarse_dirt", "mycelium", "rooted_dirt"], SHOVELS),
        ("carve", "顔を彫った", "Carver", "ハサミでカボチャを彫ろう", "Carve a pumpkin with shears", "carved_pumpkin", ["pumpkin"], ["shears"]),
        ("scrape", "サビ落とし", "Rust Remover", "斧で銅のサビを落とそう", "Scrape copper with an axe", "copper_block", [b for b in self.blocks if b.startswith(("exposed_", "weathered_", "oxidized_")) and "copper" in b], AXES),
        ("unwax", "ろう落とし", "Unwaxed", "斧で銅のろうを落とそう", "Remove wax from copper with an axe", "honeycomb", [b for b in self.blocks if b.startswith("waxed_")], AXES),
        ("brush", "考古学者", "Archaeologist", "怪しげな砂か砂利をブラシではこう", "Brush suspicious sand or gravel", "brush", [b for b in ("suspicious_sand", "suspicious_gravel") if b in self.blocks], ["brush"]),
        ("glowsign", "光る看板", "Glowing Sign", "看板に輝くイカスミを塗ろう", "Use a glow ink sac on a sign", "glow_ink_sac", [b for b in self.blocks if b.endswith("_sign")], ["glow_ink_sac"]),
        ("dyesign", "色つき看板", "Colored Sign", "看板を染料で染めよう", "Dye a sign", "red_dye", [b for b in self.blocks if b.endswith("_sign")], DYES),
        ("tnt", "点火", "Ignition", "火打石と打ち金でTNTに火をつけよう", "Light TNT with flint and steel", "tnt", ["tnt"], ["flint_and_steel"]),
        ("anchor", "アンカー充電", "Charged", "リスポーンアンカーにグロウストーンを入れよう", "Charge a respawn anchor", "respawn_anchor", ["respawn_anchor"], ["glowstone"]),
        ("lodestone", "ロードストーンの導き", "Lodestone Link", "コンパスをロードストーンに使おう", "Use a compass on a lodestone", "lodestone", ["lodestone"], ["compass"]),
        ("candlecake", "ハッピーバースデー", "Happy Birthday", "ケーキにろうそくを立てよう", "Put a candle on a cake", "cake", ["cake"], ["candle"]),
        ("eye", "目をはめた", "Eye Fitted", "エンドポータルフレームにエンダーアイをはめよう", "Put an eye of ender in an end portal frame", "ender_eye", ["end_portal_frame"], ["ender_eye"]),
        ("bonemeal", "肥料まき", "Fertilizer", "骨粉で草を生やそう", "Use bone meal on grass", "bone_meal", ["grass_block", "moss_block"], ["bone_meal"]),
    ]
    for key, ja, en, dja, den, ic, blocks, tools in singles:
        blocks = sorted(b for b in blocks if b in self.blocks); tools = [t for t in tools if t in self.items]
        if not blocks or not tools or ic not in self.items:
            continue
        t = L(f"{NS}.act.{key}", ja, en); d = L(f"{NS}.act.{key}.desc", dja, den)
        ent.append((f"act_{key}", ic, tr(t), tr(d), ioub(self, blocks, tools), "task", 5))
    for disc in sorted(i for i in self.items if i.startswith("music_disc_")):
        n = vtr(f"item.minecraft.{disc}.desc")
        ent.append((f"disc_{disc}", disc, tr(f"{NS}.disc.title", n), tr(f"{NS}.disc.desc", n), ioub(self, ["jukebox"], [disc]), "task", 10))
    chunked(self, "exp", "体験", "Experiences", "potion", "minecraft:textures/block/amethyst_block.png", ent)
    # every mob: 10 / 100 / 1000 kills, every tool: broken
    have = {m for m in KILL_TIERS}
    r2 = "killn/root"
    for m in [m for m in MOBS if m in self.ents and m not in have and m not in MOB_CHALLENGE]:
        prev = r2
        for ti, n in enumerate([10, 100, 1000]):
            prev = self.add(f"killn/{m}_{n}", prev, self.mob_icon(m), tr(f"{NS}.killn.title", self.ent(m), n),
                            tr(f"{NS}.killn.desc", self.ent(m), n), statc("killed", m, n), frame=FRAMES_BY_TIER[ti + 1], xp=[10, 30, 100][ti])
    broken_have = {it for it, _ in BROKEN}
    tools = sorted(it for it in self.items if (it.endswith(tuple(EQUIP_SUFFIX[:9])) or it in ("bow", "crossbow", "trident", "shield", "fishing_rod", "flint_and_steel", "shears", "carrot_on_a_stick", "warped_fungus_on_a_stick", "brush", "mace", "elytra", "turtle_helmet")) and it not in broken_have)
    for it in tools:
        name = self.plain_item_name(it)
        p = self.add(f"use/broken_{it}_1", "use/root", it, tr(f"{NS}.broken.title1", name), tr(f"{NS}.broken.desc", name, 1), statc("broken", it, 1), xp=5)
        self.add(f"use/broken_{it}_10", p, it, tr(f"{NS}.broken.title", name, 10), tr(f"{NS}.broken.desc", name, 10), statc("broken", it, 10), frame="goal", xp=20)


Gen.gen_more_vanilla = gen_more_vanilla


# ---------------- v1.8.0: small everyday actions ----------------
FIRST = [  # custom stat, minimum, title (past tense), description
    ("walk_one_cm", 100, "歩いた", "歩いてみましょう"), ("sprint_one_cm", 100, "走った", "走ってみましょう"),
    ("crouch_one_cm", 100, "そろりそろり", "しゃがんだまま歩いてみましょう"), ("sneak_time", 20, "しゃがんだ", "しゃがんでみましょう"),
    ("jump", 1, "ジャンプした", "ジャンプしてみましょう"), ("swim_one_cm", 100, "泳いだ", "泳いでみましょう"),
    ("walk_on_water_one_cm", 100, "水の上を歩いた", "浅い水の上を歩いてみましょう"), ("walk_under_water_one_cm", 100, "水の中を歩いた", "水の中を歩いてみましょう"),
    ("climb_one_cm", 100, "登った", "はしごやツタを登ってみましょう"), ("fall_one_cm", 100, "落ちた", "1ブロック以上落ちてみましょう"),
    ("fly_one_cm", 100, "飛んだ", "クリエイティブで飛んでみましょう"), ("aviate_one_cm", 100, "滑空した", "エリトラで飛んでみましょう"),
    ("boat_one_cm", 100, "ボートをこいだ", "ボートで進んでみましょう"), ("minecart_one_cm", 100, "トロッコに乗った", "トロッコで進んでみましょう"),
    ("horse_one_cm", 100, "ウマで走った", "ウマに乗って進んでみましょう"), ("pig_one_cm", 100, "ブタで進んだ", "ブタに乗って進んでみましょう"),
    ("strider_one_cm", 100, "ストライダーで進んだ", "ストライダーに乗って進んでみましょう"),
    ("happy_ghast_one_cm", 100, "ハッピーガストで飛んだ", "ハッピーガストに乗って飛んでみましょう"),
    ("nautilus_one_cm", 100, "オウムガイで泳いだ", "オウムガイに乗って泳いでみましょう"),
    ("play_time", 1200, "遊びはじめた", "1分遊びましょう"), ("drop", 1, "アイテムを捨てた", "アイテムを捨ててみましょう"),
    ("damage_dealt", 1, "なぐった", "何かにダメージを与えましょう"), ("damage_taken", 1, "いたっ", "ダメージを受けましょう"),
    ("damage_blocked_by_shield", 1, "盾で防いだ", "盾で攻撃を防ぎましょう"), ("mob_kills", 1, "はじめての討伐", "モブを倒しましょう"),
    ("fish_caught", 1, "釣れた", "魚を釣りましょう"), ("talked_to_villager", 1, "話しかけた", "村人に話しかけましょう"),
    ("traded_with_villager", 1, "取引した", "村人と取引しましょう"), ("animals_bred", 1, "はじめての繁殖", "動物を繁殖させましょう"),
    ("open_chest", 1, "チェストを開けた", "チェストを開けましょう"), ("open_barrel", 1, "たるを開けた", "たるを開けましょう"),
    ("open_enderchest", 1, "エンダーチェストを開けた", "エンダーチェストを開けましょう"), ("open_shulker_box", 1, "シュルカーボックスを開けた", "シュルカーボックスを開けましょう"),
    ("interact_with_crafting_table", 1, "作業台を使った", "作業台を開きましょう"), ("interact_with_furnace", 1, "かまどを使った", "かまどを開きましょう"),
    ("interact_with_blast_furnace", 1, "溶鉱炉を使った", "溶鉱炉を開きましょう"), ("interact_with_smoker", 1, "燻製器を使った", "燻製器を開きましょう"),
    ("interact_with_campfire", 1, "焚き火で焼いた", "焚き火に食べ物を置きましょう"), ("interact_with_anvil", 1, "金床を使った", "金床を開きましょう"),
    ("interact_with_grindstone", 1, "砥石を使った", "砥石を開きましょう"), ("interact_with_stonecutter", 1, "石切台を使った", "石切台を開きましょう"),
    ("interact_with_loom", 1, "機織り機を使った", "機織り機を開きましょう"), ("interact_with_cartography_table", 1, "製図台を使った", "製図台を開きましょう"),
    ("interact_with_smithing_table", 1, "鍛冶台を使った", "鍛冶台を開きましょう"), ("interact_with_brewingstand", 1, "醸造台を使った", "醸造台を開きましょう"),
    ("interact_with_beacon", 1, "ビーコンを使った", "ビーコンを開きましょう"), ("interact_with_lectern", 1, "書見台を使った", "書見台で本を読みましょう"),
    ("inspect_hopper", 1, "ホッパーをのぞいた", "ホッパーを開きましょう"), ("inspect_dispenser", 1, "発射装置をのぞいた", "発射装置を開きましょう"),
    ("inspect_dropper", 1, "ドロッパーをのぞいた", "ドロッパーを開きましょう"), ("trigger_trapped_chest", 1, "トラップチェストを開けた", "トラップチェストを開けましょう"),
    ("play_noteblock", 1, "音ブロックを鳴らした", "音ブロックを鳴らしましょう"), ("tune_noteblock", 1, "音ブロックを調律した", "音ブロックを調律しましょう"),
    ("play_record", 1, "レコードをかけた", "ジュークボックスでレコードをかけましょう"), ("pot_flower", 1, "植木鉢に植えた", "植木鉢に植えましょう"),
    ("fill_cauldron", 1, "大釜に水を入れた", "大釜を満たしましょう"), ("use_cauldron", 1, "大釜の水を使った", "大釜の水を使いましょう"),
    ("clean_armor", 1, "革の防具を洗った", "大釜で革の防具を洗いましょう"), ("clean_banner", 1, "旗を洗った", "大釜で旗を洗いましょう"),
    ("clean_shulker_box", 1, "シュルカーボックスを洗った", "大釜でシュルカーボックスを洗いましょう"), ("bell_ring", 1, "鐘を鳴らした", "鐘を鳴らしましょう"),
    ("target_hit", 1, "的に当てた", "的に当てましょう"), ("enchant_item", 1, "エンチャントした", "エンチャントしましょう"),
    ("sleep_in_bed", 1, "おやすみ", "ベッドで寝ましょう"), ("leave_game", 1, "セーブして終了した", "ゲームを終了しましょう"),
]
FIRST_ICON = {"walk_one_cm": "leather_boots", "sprint_one_cm": "golden_boots", "crouch_one_cm": "leather_leggings", "sneak_time": "leather_leggings",
              "jump": "rabbit_foot", "swim_one_cm": "cod", "walk_on_water_one_cm": "lily_pad", "walk_under_water_one_cm": "turtle_helmet",
              "climb_one_cm": "ladder", "fall_one_cm": "feather", "fly_one_cm": "elytra", "aviate_one_cm": "elytra", "boat_one_cm": "oak_boat",
              "minecart_one_cm": "minecart", "horse_one_cm": "saddle", "pig_one_cm": "carrot_on_a_stick", "strider_one_cm": "warped_fungus_on_a_stick",
              "happy_ghast_one_cm": "white_harness", "nautilus_one_cm": "nautilus_shell", "play_time": "clock", "drop": "rotten_flesh",
              "damage_dealt": "wooden_sword", "damage_taken": "bone", "damage_blocked_by_shield": "shield", "mob_kills": "iron_sword",
              "fish_caught": "fishing_rod", "talked_to_villager": "emerald", "traded_with_villager": "emerald", "animals_bred": "wheat",
              "open_chest": "chest", "open_barrel": "barrel", "open_enderchest": "ender_chest", "open_shulker_box": "shulker_box",
              "interact_with_crafting_table": "crafting_table", "interact_with_furnace": "furnace", "interact_with_blast_furnace": "blast_furnace",
              "interact_with_smoker": "smoker", "interact_with_campfire": "campfire", "interact_with_anvil": "anvil", "interact_with_grindstone": "grindstone",
              "interact_with_stonecutter": "stonecutter", "interact_with_loom": "loom", "interact_with_cartography_table": "cartography_table",
              "interact_with_smithing_table": "smithing_table", "interact_with_brewingstand": "brewing_stand", "interact_with_beacon": "beacon",
              "interact_with_lectern": "lectern", "inspect_hopper": "hopper", "inspect_dispenser": "dispenser", "inspect_dropper": "dropper",
              "trigger_trapped_chest": "trapped_chest", "play_noteblock": "note_block", "tune_noteblock": "note_block", "play_record": "jukebox",
              "pot_flower": "flower_pot", "fill_cauldron": "water_bucket", "use_cauldron": "cauldron", "clean_armor": "leather_chestplate",
              "clean_banner": "white_banner", "clean_shulker_box": "shulker_box", "bell_ring": "bell", "target_hit": "target",
              "enchant_item": "enchanting_table", "sleep_in_bed": "red_bed", "leave_game": "oak_door"}
# counted by this mod's own code (mod jars only): look direction, turning and side/back steps
GESTURES = [  # stat, tiers, titles per tier, desc (%s = shown amount), icon, divisor for the shown amount
    ("turned_right", [45, 360, 3600, 36000, 360000], ["右を向いた", "右に一回転", "右に10回転", "右に100回転", "右に1000回転"], "右に%s度回りましょう", "compass", 1),
    ("turned_left", [45, 360, 3600, 36000, 360000], ["左を向いた", "左に一回転", "左に10回転", "左に100回転", "左に1000回転"], "左に%s度回りましょう", "compass", 1),
    ("looked_up", [1, 100, 1000], ["上を見た", "空ばかり見ている", "首が痛い（上）"], "上を%s回見ましょう", "feather", 1),
    ("looked_down", [1, 100, 1000], ["下を見た", "足もとに注意", "首が痛い（下）"], "下を%s回見ましょう", "leather_boots", 1),
    ("looked_straight_up", [1], ["真上を見た"], "真上を見ましょう", "spyglass", 1),
    ("looked_straight_down", [1], ["真下を見た"], "真下を見ましょう", "spyglass", 1),
    ("moved_backward", [100, 10000, 100000], ["後ろに歩いた", "後ろ歩き名人", "ムーンウォーカー"], "後ろ向きに%sm進みましょう", "leather_boots", 100),
    ("moved_left", [100, 10000, 100000], ["左に歩いた", "左歩き名人", "左歩きの達人"], "左に%sm歩きましょう", "leather_boots", 100),
    ("moved_right", [100, 10000, 100000], ["右に歩いた", "右歩き名人", "右歩きの達人"], "右に%sm歩きましょう", "leather_boots", 100),
    ("sneak_presses", [100, 1000, 10000], ["しゃがみ連打", "屈伸マスター", "屈伸の神"], "%s回しゃがみましょう", "leather_leggings", 1),
]
GESTURE_COMBOS = [  # key, title, desc, icon, [(stat, min)]
    ("kyoro", "キョロキョロ", "右と左を見回しましょう", "spyglass", [("turned_right", 90), ("turned_left", 90)]),
    ("nod", "うなずいた", "上と下を10回ずつ見ましょう", "player_head", [("looked_up", 10), ("looked_down", 10)]),
    ("crab", "カニ歩き", "右と左に100mずつ歩きましょう", "crab_claw", [("moved_left", 10000), ("moved_right", 10000)]),
    ("allround", "全方向マスター", "前後左右の全部に歩きましょう", "compass", [("moved_backward", 100), ("moved_left", 100), ("moved_right", 100)]),
]


def gen_small_vanilla(self):
    TABS["small"] = ("ちょっとした行動", "Little Things", "leather_boots", "minecraft:textures/block/oak_planks.png")
    r = self.root("small")
    have = {st for st, tiers, *_ in STATS if tiers[0] <= 1}
    prev = r
    i = 0
    for st, mn, ja, dja in FIRST:
        if st not in self.reg["custom_stat"] or st in have:
            continue
        ic = FIRST_ICON.get(st, "book")
        if ic not in self.items:
            ic = "book"
        t = L(f"{NS}.small.{st}", ja, ja); d = L(f"{NS}.small.{st}.desc", dja, dja)
        prev = self.add(f"small/first_{st}", r if i % 10 == 0 else prev, ic, tr(t), tr(d),
                        {"s": {"trigger": "minecraft:tick", "conditions": {"player": self.player_stat("custom", st, mn)}}}, xp=2, chat=False)
        i += 1


def gen_small_java(self):
    STAT_NAMES.update({"turned_right": ("右に回った角度", "Degrees Turned Right"), "turned_left": ("左に回った角度", "Degrees Turned Left"),
                       "looked_up": ("上を見た回数", "Times Looked Up"), "looked_down": ("下を見た回数", "Times Looked Down"),
                       "looked_straight_up": ("真上を見た回数", "Times Looked Straight Up"), "looked_straight_down": ("真下を見た回数", "Times Looked Straight Down"),
                       "moved_backward": ("後ろに歩いた距離", "Distance Moved Backward"), "moved_left": ("左に歩いた距離", "Distance Moved Left"),
                       "moved_right": ("右に歩いた距離", "Distance Moved Right"), "sneak_presses": ("しゃがんだ回数", "Times Sneaked")})
    for st, tiers, titles, dja, icon, div in GESTURES:
        L(f"stat.{NS}.{st}", *STAT_NAMES[st])
        d = L(f"{NS}.gesture.{st}.desc", dja, dja)
        prev = "small/root"
        for ti, n in enumerate(tiers):
            t = L(f"{NS}.gesture.{st}.{ti}", titles[ti], titles[ti])
            shown = n // div if n % div == 0 else n / div
            prev = self.add(f"small/g_{st}_{ti + 1}", prev, icon if icon in self.items else "compass", tr(t), tr(d, f"{shown:,}"),
                            {"s": {"trigger": "minecraft:tick", "conditions": {"player": own_stat(self, st, n)}}},
                            frame=["task", "task", "goal", "challenge", "challenge"][ti], xp=[2, 10, 30, 60, 100][ti], chat=False if ti == 0 else None)
    for key, ja, dja, icon, need in GESTURE_COMBOS:
        t = L(f"{NS}.gesture.{key}", ja, ja); d = L(f"{NS}.gesture.{key}.desc", dja, dja)
        pred = {"type_specific": {"type": "minecraft:player" if self.new else "player",
                                  "stats": [{"type": "minecraft:custom", "stat": f"{NS}:{st}", "value": {"min": mn}} for st, mn in need]}}
        self.add(f"small/g_{key}", "small/root", icon if icon in self.items else "compass", tr(t), tr(d),
                 {"s": {"trigger": "minecraft:tick", "conditions": {"player": pred}}}, frame="goal", xp=10)


Gen.gen_small_vanilla = gen_small_vanilla
Gen.gen_small_java = gen_small_java

stats = {}
for v in ["1.20.1", "1.21.1", "26.2"]:
    g = Gen(v)
    n = g.run()
    roots = sum(1 for p in g.advs if p.endswith("root"))
    stats[v] = {"total": n, "roots": roots, "advancements": n - roots,
                "vanilla": len(g.vanilla_paths) - sum(1 for p in g.vanilla_paths if p.endswith("root")),
                "by_tab": {}}
    for p in g.advs:
        t = p.split("/")[0]
        stats[v]["by_tab"][t] = stats[v]["by_tab"].get(t, 0) + 1
os.makedirs(os.path.join(OUT, "lang"), exist_ok=True)
json.dump(LANG_JA, open(os.path.join(OUT, "lang", "ja_jp.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
json.dump(LANG_EN, open(os.path.join(OUT, "lang", "en_us.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
json.dump(stats, open(os.path.join(OUT, "stats.json"), "w"), ensure_ascii=False, indent=1)
print(json.dumps(stats, ensure_ascii=False, indent=1))
