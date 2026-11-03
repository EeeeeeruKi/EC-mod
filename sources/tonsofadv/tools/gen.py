# -*- coding: utf-8 -*-
"""Tons of Advancements generator.
usage: python3 -I gen.py <registry-dir> <out-dir>
Writes:
  <out>/v1201/data/tonsofadv/advancements/...   (1.20.1 format)
  <out>/v1211/data/tonsofadv/advancement/...    (1.21.1 format)
  <out>/lang/ja_jp.json, en_us.json
  <out>/stats.json
"""
import json, os, sys

REG, OUT = sys.argv[1], sys.argv[2]
NS = "tonsofadv"
LANG_JA, LANG_EN = {}, {}

def L(key, ja, en):
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
        "zombie_villager", "zombified_piglin"]
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
]
FRAMES_BY_TIER = ["task", "task", "goal", "challenge", "challenge"]
XP = {"task": 5, "goal": 20, "challenge": 50}


class Gen:
    def __init__(self, ver):
        self.v = ver
        self.new = ver == "1.21.1"
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
                                  "stats": [{"type": f"minecraft:{stype}", "stat": f"minecraft:{stat}", "value": {"min": mn}}]}}

    def loc_biome(self, b):
        return {"biomes": f"minecraft:{b}"} if self.new else {"biome": f"minecraft:{b}"}

    def loc_struct(self, s):
        return {"structures": f"minecraft:{s}"} if self.new else {"structure": f"minecraft:{s}"}

    def item_name(self, item):
        kind = "block" if item in self.blocks else "item"
        base = vtr(f"{kind}.minecraft.{item}")
        if item.startswith("music_disc_") or item.endswith("_banner_pattern") or item == "disc_fragment_5":
            return tr(f"{NS}.obtain.title2", base, vtr(f"item.minecraft.{item}.desc"))
        if item.endswith("_armor_trim_smithing_template"):
            pat = item[:-len("_armor_trim_smithing_template")]
            return tr(f"{NS}.obtain.title2", base, vtr(f"trim_pattern.minecraft.{pat}"))
        if item == "netherite_upgrade_smithing_template":
            return tr(f"{NS}.obtain.title2", base, vtr("upgrade.minecraft.netherite_upgrade"))
        return base

    def plain_item_name(self, item):
        kind = "block" if item in self.blocks else "item"
        return vtr(f"{kind}.minecraft.{item}")

    def add(self, path, parent, icon, title, desc, criteria, frame="task", xp=None, chat=None, bg=None, hidden=False):
        disp = {"icon": self.icon(icon), "title": title, "description": desc, "frame": frame,
                "show_toast": bg is None, "announce_to_chat": (frame != "task") if chat is None else chat,
                "hidden": hidden}
        if bg:
            disp["background"] = bg
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
        if self.new:
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
        # sanity: icons must exist, parents must exist
        for p, a in self.advs.items():
            ic = a["display"]["icon"]
            iid = (ic.get("id") or ic.get("item")).split(":")[1]
            assert iid in self.items, (self.v, p, iid)
            if "parent" in a:
                assert a["parent"].split(":")[1] in self.advs, (p, a["parent"])
        folder = "advancement" if self.new else "advancements"
        base = os.path.join(OUT, "v1211" if self.new else "v1201", "data", NS, folder)
        for p, a in self.advs.items():
            fp = os.path.join(base, p + ".json")
            os.makedirs(os.path.dirname(fp), exist_ok=True)
            with open(fp, "w", encoding="utf-8") as f:
                json.dump(a, f, ensure_ascii=False, separators=(",", ":"))
        return len(self.advs)


stats = {}
for v in ["1.20.1", "1.21.1"]:
    g = Gen(v)
    n = g.run()
    roots = sum(1 for p in g.advs if p.endswith("/root"))
    stats[v] = {"total": n, "roots": roots, "advancements": n - roots,
                "by_tab": {}}
    for p in g.advs:
        t = p.split("/")[0]
        stats[v]["by_tab"][t] = stats[v]["by_tab"].get(t, 0) + 1
os.makedirs(os.path.join(OUT, "lang"), exist_ok=True)
json.dump(LANG_JA, open(os.path.join(OUT, "lang", "ja_jp.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
json.dump(LANG_EN, open(os.path.join(OUT, "lang", "en_us.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
json.dump(stats, open(os.path.join(OUT, "stats.json"), "w"), ensure_ascii=False, indent=1)
print(json.dumps(stats, ensure_ascii=False, indent=1))
