"""Turn data.txt (editor export) into scenes.json + lang files for More MekaPonder."""
import json, re, sys
D = '/home/claude/dl/mek/'
OUT = sys.argv[1]

COLORS = ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black','dark_red','aqua']
TIERS = ['basic','advanced','elite','ultimate']
MATS = ['osmium','bronze','lapis_lazuli','refined_glowstone','refined_obsidian','steel']

# ---------- names -> ids ----------
names = {}   # japanese name -> registry id
chems = {}   # japanese name -> chemical id
blocks = set()
for f in ['mek_ja.json','generators_ja.json','additions_ja.json','tools_ja.json']:
    for k, v in json.load(open(D+f)).items():
        m = re.match(r'^(item|block)\.(mekanism\w*)\.(\w+)$', k)
        if m:
            rid = m.group(2)+':'+m.group(3)
            names.setdefault(v, rid)
            if m.group(1) == 'block': blocks.add(rid)
        m = re.match(r'^chemical\.(mekanism\w*)\.(\w+)$', k)
        if m: chems.setdefault(v, m.group(1)+':'+m.group(2))
VAN_ITEMS = {
 '鉄鉱石':'iron_ore','鉄の原石':'raw_iron','鉄インゴット':'iron_ingot','銅インゴット':'copper_ingot','石炭':'coal','木炭':'charcoal',
 'レッドストーン':'redstone','ダイヤモンド':'diamond','丸石':'cobblestone','砂利':'gravel','砂':'sand','火打石':'flint','黒曜石':'obsidian',
 '深層岩の丸石':'cobbled_deepslate','小麦の種':'wheat_seeds','ジャガイモ':'potato','小麦':'wheat','パン':'bread','グロウストーンダスト':'glowstone_dust',
 'オークの原木':'oak_log','オークの板材':'oak_planks','紙':'paper','ブラックストーン':'blackstone','きらめくブラックストーン':'gilded_blackstone',
 '白色の羊毛':'white_wool','赤色の羊毛':'red_wool','赤色の染料':'red_dye','チェスト':'chest','石':'stone','原木':'oak_log','土':'dirt',
 '水入りバケツ':'water_bucket','レバー':'lever','フェンス':'oak_fence','染料':'red_dye','棒':'stick','溶岩':'lava_bucket','TNT':'tnt','石の壁':'stone',
 '防具立て':'armor_stand','矢':'arrow','的':'target','金':'gold_ingot','錫':'tin',
}
for k, v in VAN_ITEMS.items():
    if ':' not in v: names[k] = 'minecraft:'+v
names.pop('錫', None)
EXTRA = {  # editor wording -> id
 '発電機':'mekanismgenerators:heat_generator','機械':'mekanism:energized_smelter','精鋭ユニバーサルケーブル':'mekanism:elite_universal_cable',
 '究極ユニバーサルケーブル':'mekanism:ultimate_universal_cable','基本ユニバーサルケーブル':'mekanism:basic_universal_cable',
 '発展ユニバーサルケーブル':'mekanism:advanced_universal_cable','ユニバーサルケーブル':'mekanism:basic_universal_cable',
 '物流トランスポーター':'mekanism:basic_logistical_transporter','トランスポーター':'mekanism:basic_logistical_transporter',
 'メカニカルパイプ':'mekanism:basic_mechanical_pipe','加圧チューブ':'mekanism:basic_pressurized_tube','熱力学的コンダクター':'mekanism:basic_thermodynamic_conductor',
 '化学タンク':'mekanism:basic_chemical_tank','流体タンク':'mekanism:basic_fluid_tank','メカニカルタンク':'mekanism:basic_fluid_tank','エネルギーキューブ':'mekanism:basic_energy_cube',
 'ビン':'mekanism:basic_bin','基本ティアインストーラー':'mekanism:basic_tier_installer','基本濃縮ファクトリー':'mekanism:basic_enriching_factory',
 '凝塊':'mekanism:clump_iron','欠片':'mekanism:shard_iron','汚れた粉':'mekanism:dirty_dust_iron','粉':'mekanism:dust_iron',
 '鉄の凝塊':'mekanism:clump_iron','鉄の欠片':'mekanism:shard_iron','鉄の結晶':'mekanism:crystal_iron','汚れた鉄の粉':'mekanism:dirty_dust_iron',
 '濃縮鉄':'mekanism:enriched_iron','鋼鉄の粉':'mekanism:dust_steel','黒曜石の粉':'mekanism:dust_obsidian','精製黒曜石の粉':'mekanism:dust_refined_obsidian',
 '濃縮黒曜石':'mekanism:enriched_refined_obsidian','青銅インゴット':'mekanism:ingot_bronze','おがくず':'mekanism:sawdust','基質':'mekanism:substrate',
 'HDPEペレット':'mekanism:hdpe_pellet','HDPEシート':'mekanism:hdpe_sheet','塩ブロック':'mekanism:block_salt','塩':'mekanism:salt','硫黄の粉':'mekanism:dust_sulfur',
 'リチウムの粉':'mekanism:dust_lithium','蛍石の粉':'mekanism:dust_fluorite','反物質ペレット':'mekanism:pellet_antimatter','モジュール':'mekanism:module_energy_unit',
 'ジェットパックユニット':'mekanism:module_jetpack_unit','ボディアーマー':'mekanism:mekasuit_bodyarmor','ヘルメット':'mekanism:mekasuit_helmet',
 'ブーツ':'mekanism:mekasuit_boots','パンツ':'mekanism:mekasuit_pants','MekaSuitヘルメット':'mekanism:mekasuit_helmet','タブレット':'mekanism:energy_tablet',
 '鋼鉄ブロック':'mekanism:block_steel','タービンローター':'mekanismgenerators:turbine_rotor','核融合炉制御装置':'mekanismgenerators:fusion_reactor_controller',
 '燃料を詰めたホールラウム':'mekanismgenerators:hohlraum','防護服':'mekanism:hazmat_gown','放射性廃棄物バレル':'mekanism:radioactive_waste_barrel',
 'テレポーター':'mekanism:teleporter','ロビット':'mekanism:robit','発熱器':'mekanism:resistive_heater','増幅器':'mekanism:laser_amplifier',
 'オスミウムのチェストプレート':'mekanismtools:osmium_chestplate','オスミウムのツルハシ':'mekanismtools:osmium_pickaxe','オスミウムのパクセル':'mekanismtools:osmium_paxel',
 'ツルハシ':'mekanismtools:osmium_pickaxe','オノ':'mekanismtools:osmium_axe','シャベル':'mekanismtools:osmium_shovel','ヘルメット・レギンス・ブーツ':'mekanismtools:osmium_helmet',
 'プラスチックブロック':'mekanismadditions:white_plastic','輝くプラスチックブロック':'mekanismadditions:white_plastic_glow','ニワトリ':'minecraft:chicken_spawn_egg','ブタ':'minecraft:pig_spawn_egg',
 'D-T燃料':None,
}
CHEM_EXTRA = {'きれいな鉄のスラリー':'mekanism:clean_iron','汚れた鉄のスラリー':'mekanism:dirty_iron','スラリー':'mekanism:dirty_iron','液体の水素':'mekanism:hydrogen',
 '液体のエチレン':'mekanism:ethene','エチレン':'mekanism:ethene','炭素':'mekanism:carbon','反物質':'mekanism:antimatter','核廃棄物':'mekanism:nuclear_waste',
 'プルトニウム':'mekanism:plutonium','三重水素':'mekanism:tritium','D-T燃料':'mekanismgenerators:fusion_fuel','栄養ペースト':'mekanism:nutritional_paste',
 '赤色の顔料':'mekanism:red','白色の顔料':'mekanism:white','桃色の顔料':'mekanism:pink','重水':'mekanism:heavy_water','水':'minecraft:water','塩水':'mekanism:brine',
 '二酸化硫黄':'mekanism:sulfur_dioxide','三酸化硫黄':'mekanism:sulfur_trioxide','硫酸':'mekanism:sulfuric_acid','酸素':'mekanism:oxygen','水素':'mekanism:hydrogen',
 '塩素':'mekanism:chlorine','塩化水素':'mekanism:hydrogen_chloride','ナトリウム':'mekanism:sodium','リチウム':'mekanism:lithium','気体':'mekanism:hydrogen','液体':'minecraft:water',
 'オスミウムのゲージ':'mekanism:osmium','レッドストーンのゲージ':'mekanism:redstone','ダイヤモンドのゲージ':'mekanism:diamond','金のゲージ':'mekanism:gold',
 '錫のゲージ':'mekanism:tin','精製黒曜石のゲージ':'mekanism:refined_obsidian','炭素のゲージ':'mekanism:carbon','反物質のゲージ':'mekanism:antimatter','酸素のゲージ':'mekanism:oxygen','電力のゲージ':'energy'}
CHEM_COLOR = {'hydrogen':0xFFFFFF,'oxygen':0x6CE2FF,'chlorine':0xCFE800,'hydrogen_chloride':0xA8F1E9,'sulfuric_acid':0x82802B,'sulfur_dioxide':0xA99D90,
 'sulfur_trioxide':0xCE6C6C,'brine':0xFEEF9C,'sodium':0xE9FEF4,'lithium':0xEBA400,'ethene':0xEACCF9,'carbon':0x404040,'redstone':0xB30505,'diamond':0x6CEDD8,
 'gold':0xF2CD67,'tin':0xCCCCD9,'refined_obsidian':0x7C00ED,'osmium':0x52BDCA,'antimatter':0xA464B3,'nuclear_waste':0x4F412A,'plutonium':0x1F919C,
 'tritium':0x64FF70,'fusion_fuel':0x7E007D,'nutritional_paste':0xEB6B63,'clean_iron':0xC9C9C9,'dirty_iron':0x7F6E5C,'red':0xB02E26,'white':0xF9FFFE,'pink':0xF38BAA,
 'heavy_water':0x2A3FA0,'water':0x3F76E4,'energy':0x3CFE9A}

for k, v in EXTRA.items():
    if v: names[k] = v
for k, v in CHEM_EXTRA.items(): chems[k] = v
for k in [k for k in chems if len(k) < 2]: del chems[k]
for k in list(names):
    if names[k] is None or k in chems: del names[k]

def items_spec(eid, spec):
    if not spec: return [eid]
    if spec.startswith('F:'):
        kind = spec[2:]; return [eid] + [f'mekanism:{t}_{kind}_factory' for t in TIERS]
    if spec.startswith('T'):
        base = spec.split(':')[1]; l = [f'mekanism:{t}_{base}' for t in TIERS]
        return l + ([f'mekanism:creative_{base}'] if spec.startswith('T+') else [])
    if spec.startswith('C:'):
        return [f'mekanismadditions:' + spec[2:].replace('{c}', c) for c in COLORS]
    if spec == 'A:armor':
        return [f'mekanismtools:{m}_{p}' for m in MATS for p in ['helmet','chestplate','leggings','boots']]
    if spec == 'A:tools':
        l = [f'mekanismtools:{m}_paxel' for m in ['wood','stone','iron','gold','diamond','netherite']]
        return l + [f'mekanismtools:{m}_{p}' for m in MATS for p in ['paxel','sword','pickaxe','axe','shovel','hoe','shield']]
    return spec.split(',')

# ---------- parse ----------
entries = []
for block in open('/home/claude/mmp/data.txt').read().strip().split('\n\n'):
    lines = block.strip().split('\n')
    eid, spec, title = lines[0].split(';')
    steps = [(l[0], l[2:]) for l in lines[1:]]
    entries.append(dict(id=eid, items=items_spec(eid, spec), title=title, steps=steps))

def find_all(text, table):
    """Longest-match scan, returns [(pos, name, id)]."""
    keys = sorted(table, key=len, reverse=True); out = []; used = [False]*len(text)
    for k in keys:
        for m in re.finditer(re.escape(k), text):
            if not any(used[m.start():m.end()]):
                out.append((m.start(), k, table[k]))
                for i in range(m.start(), m.end()): used[i] = True
    return sorted(out)

def count_after(text, pos_end):
    m = re.match(r'(?:を|が)?(\d+)(?:つ|本|枚|個)', text[pos_end:])
    return int(m.group(1)) if m else 1

MACHINE_HINTS = ['機','室','装置','器','ファクトリー','ステーション','ポンプ','分離','タンク','キューブ','パッド','マイナー','ビン','統合器']
def is_machine(rid):
    return rid in blocks and any(h in names_rev.get(rid, '') for h in MACHINE_HINTS)
names_rev = {}
for k, v in names.items(): names_rev.setdefault(v, k)

GEAR_SLOT = {'helmet':'HEAD','mask':'HEAD','chestplate':'CHEST','bodyarmor':'CHEST','jetpack':'CHEST','gown':'CHEST','tank':'CHEST','leggings':'LEGS','pants':'LEGS','boots':'FEET','runners':'FEET'}
def slot_for(rid):
    p = rid.split(':')[1]
    for k, s in GEAR_SLOT.items():
        if p.endswith(k) or k in p and k in ('jetpack','runners','scuba_tank'): return s
    if 'scuba_tank' in p: return 'CHEST'
    return 'MAINHAND'

def compile_action(e, text, state):
    ops = []
    main = e['id'] if e['id'] in blocks else None
    cfound = find_all(text, chems)
    cspans = [(p, p+len(n)) for p, n, _ in cfound]
    found = [f for f in find_all(text, names) if not any(a <= f[0] < b for a, b in cspans)]
    ispans = [(p, p+len(n)) for p, n, _ in found]
    cfound = [c for c in cfound if not any(a <= c[0] and c[0]+len(c[1]) <= b and (b-a) > len(c[1]) for a, b in ispans)]
    # infrastructure
    def place_main():
        if main and not state.get('placed'):
            ops.append({'op':'block','pos':[3,1,3],'id':main}); state['placed'] = True
    def infra(kind):
        if kind in state: return
        state[kind] = True
        src = 'mekanismgenerators:heat_generator' if (kind == 'power' and '発電機から' in text) else None
        P = {'power':(src or 'mekanism:basic_energy_cube','mekanism:basic_universal_cable',[0,1,3],[[1,1,3],[2,1,3]]),
             'tube':('mekanism:basic_chemical_tank','mekanism:basic_pressurized_tube',[3,1,6],[[3,1,5],[3,1,4]]),
             'pipe':('mekanism:basic_fluid_tank','mekanism:basic_mechanical_pipe',[6,1,3],[[5,1,3],[4,1,3]]),
             'item':('minecraft:chest','mekanism:basic_logistical_transporter',[3,1,0],[[3,1,1],[3,1,2]])}[kind]
        ops.append({'op':'block','pos':P[2],'id':P[0]})
        ops.append({'op':'line','id':P[1],'cells':P[3]})
    focus = None
    if main: place_main(); focus = main
    elif found and found[0][2] in blocks and is_machine(found[0][2]) and not state.get('placed'):
        focus = found[0][2]; ops.append({'op':'block','pos':[3,1,3],'id':focus}); state['placed'] = True; state['machine'] = focus
    else:
        focus = state.get('machine')
        for _, n, rid in found:
            if is_machine(rid) and rid != state.get('machine') and not state.get('placed'):
                ops.append({'op':'block','pos':[3,1,3],'id':rid}); state['placed'] = True; state['machine'] = focus = rid; break
    if '電力' in text or 'ユニバーサルケーブル' in text or 'ケーブル' in text: infra('power')
    if '加圧チューブ' in text: infra('tube')
    if 'メカニカルパイプ' in text: infra('pipe')
    if '物流トランスポーター' in text or 'トランスポーター' in text: infra('item')
    if main and main.endswith('energy_cube') and '正面' in text and '機械' in text and 'front' not in state:
        state['front'] = True
        ops.append({'op':'line','id':'mekanism:basic_universal_cable','cells':[[3,1,2],[3,1,1]]})
        ops.append({'op':'block','pos':[3,1,0],'id':'mekanism:energized_smelter'})
        ops.append({'op':'machineon','pos':[3,1,0]})
        return ops
    if 'コンダクター' in text and main and 'conductor' not in main and 'heat' not in state:
        state['heat'] = True
        ops.append({'op':'line','id':'mekanism:basic_thermodynamic_conductor','cells':[[4,1,3],[5,1,3]]})

    machine_names = set()
    def mach_title():
        return state.get('machine') or main or (focus if focus else None)
    # effects
    if 'レーザーを消' in text:
        ops.append({'op':'laseroff'}); return ops
    for mob, et in [('ブタ','pig'),('ニワトリ','chicken'),('ロビット','mekanism:robit')]:
        if mob in text and ('召喚' in text or '射線' in text or 'ニワトリを' in text):
            ops.append({'op':'mob','type':et,'pos':[3.5,1,1.5] if 'レーザー' in text or 'ブタ' in text else [5.5,1,1.5]})
    if 'ブタが死亡' in text or '浮かび上が' in text or 'ロビットが' in text:
        ops.append({'op':'mobact','kind':'kill' if '死亡' in text else ('float' if '浮か' in text else 'move')})
    if 'レーザー' in text:
        ops.append({'op':'laser','from':[3.5,1.5,3.0],'to':[3.5,1.5,0.5],'width':0.12 if '太い' in text else 0.06})
    if '稲妻' in text: ops.append({'op':'lightning'})
    if '爆発' in text: ops.append({'op':'explode','r':2.5 if '大きな' in text else 1.5,'break':'壊れずに' not in text})
    if 'プレイヤー' in text or '着た' in text or '履いた' in text:
        pose = 'STAND'
        for k, p in [('飛び回','FLY'),('上昇','FLY'),('浮か','FLY'),('とどまる','FLY'),('速く走','SPRINT'),('走','SPRINT'),('歩','WALK'),('登','WALK'),('飛び降','WALK'),
                     ('ジャンプ','WALK'),('スニーク','SNEAK'),('構え','USE_ITEM'),('飲む','USE_ITEM'),('使う','USE_ITEM'),('放つ','USE_ITEM'),('水の中','STAND')]:
            if k in text: pose = p; break
        equip = []
        for _, n, rid in found:
            if rid.startswith('minecraft:') or rid in blocks: continue
            if rid.split(':')[1].startswith('mekasuit') or 'MekaSuit' in text:
                equip = [['HEAD','mekanism:mekasuit_helmet'],['CHEST','mekanism:mekasuit_bodyarmor'],['LEGS','mekanism:mekasuit_pants'],['FEET','mekanism:mekasuit_boots']]; break
            equip.append([slot_for(rid), rid])
        if not equip and e['id'] not in blocks:
            equip.append([slot_for(e['id']), e['id']])
        if '防護服' in text:
            equip = [['HEAD','mekanism:hazmat_mask'],['CHEST','mekanism:hazmat_gown'],['LEGS','mekanism:hazmat_pants'],['FEET','mekanism:hazmat_boots']]
        if 'スキューバマスク' in text: equip = [['HEAD','mekanism:scuba_mask'],['CHEST','mekanism:scuba_tank']]
        move = pose in ('WALK','SPRINT')
        po = {'op':'player','pose':pose,'equip':equip,'move':move,'rise':pose == 'FLY'}
        if '上に乗' in text or 'そばに立' in text: po['at'] = [3.5,1.1,3.5] if '上に乗' in text else [2.5,1,4.5]
        ops.append(po)
        return ops
    if 'クラフト' in text:
        ins = [[rid, count_after(text, p+len(n))] for p, n, rid in found if p < (text.rfind('クラフト'))]
        outs = [[rid, 1] for p, n, rid in found if p > text.rfind('クラフト')]
        if not outs and ins: outs = [ins.pop()]
        if not outs: outs = [[e['items'][0], 1]]
        ops.append({'op':'panel','title':'minecraft:crafting_table','in':ins[:9],'out':outs[:2],'gauge':None})
        return ops
    split = max(text.rfind('と、'), text.rfind('ると'), text.rfind('すと'))
    if ('入れ' in text or '送' in text or '吹き込' in text) and split > 0:
        ins, outs, gauges = [], [], []; gcol = 0x55AAFF
        for p, n, rid in found:
            if is_machine(rid) or rid in ('mekanism:basic_pressurized_tube','mekanism:basic_mechanical_pipe','mekanism:basic_universal_cable','mekanism:basic_logistical_transporter'):
                continue
            (ins if p < split else outs).append([rid, count_after(text, p+len(n))])
        for p, n, cid in cfound:
            if p >= split or '送' in text[p:p+8] or 'ゲージ' in n:
                gauges.append([n, cid])
        if '溜ま' in text and not gauges and outs:
            pass
        ms = [rid for p, n, rid in found if is_machine(rid) and not text[p+len(n):p+len(n)+2].startswith('から')]
        side_at = None
        if ms and main and ms[-1] != main:
            # the scene's own block stays in the middle; the other machine goes beside it
            if state.get('side') != ms[-1]:
                ops.append({'op':'block','pos':[5,1,1],'id':ms[-1]}); state['side'] = ms[-1]
            side_at = [5,1,1]
        elif ms and state.get('placed') and ms[-1] != (state.get('machine') or main):
            ops.append({'op':'swap','id':ms[-1]}); state['machine'] = ms[-1]
        elif ms and not state.get('placed'):
            ops.append({'op':'block','pos':[3,1,3],'id':ms[-1]}); state['placed'] = True; state['machine'] = ms[-1]
        title = side_at and ms[-1] or mach_title()
        seen = set(); g2 = []
        for n, c in gauges:
            if c in seen: continue
            seen.add(c); g2.append([n.replace('のゲージ',''), c])
        gauges = g2
        if not gauges and ('ゲージ' in text or '溜ま' in text):
            col = 0xB30505 if 'レッドストーン' in text else 0x3CFE9A if '電力' in text else 0x52BDCA if 'オスミウム' in text else 0x55AAFF
            gauges = [['', None]]; gcol = col
        ops.append({'op':'panel','title':title,'at':side_at,'in':ins[:4],'out':outs[:3],'gauge':[[n, (gcol if c is None else CHEM_COLOR.get(c.split(':')[-1], 0x55AAFF))] for n, c in gauges[:2]] or None,
                    'drain':'減' in text})
        return ops
    if 'ゲージ' in text or '溜ま' in text:
        cfound = [x for i, x in enumerate(cfound) if x[2] not in [y[2] for y in cfound[:i]]]
        gs = [[n.replace('のゲージ',''), CHEM_COLOR.get(c.split(':')[-1], 0x55AAFF)] for p, n, c in cfound] or [['', 0x3CFE9A]]
        ops.append({'op':'gauge','gauges':gs[:2],'drain':'減' in text})
        return ops
    if '右クリック' in text or '左クリック' in text:
        held = None
        for p, n, rid in found:
            if text[p+len(n):p+len(n)+2] in ('で右','で右','でス','でし') or text[p+len(n):p+len(n)+1] == 'で':
                held = rid
        if held is None and e['id'] not in blocks: held = e['id']
        m = re.search(r'(\S*?ケーブル)を.*?、(\S*?ケーブル)に変わる', text)
        if m and 'ユニバーサル' in text:
            a = find_all(m.group(1), names); b = find_all(m.group(2), names)
            if a and b:
                ops.append({'op':'line','id':a[-1][2],'cells':[[4,1,5],[5,1,5],[6,1,5]]})
                ops.append({'op':'click','left':False,'sneak':False,'item':held,'at':[5.5,2,5.5]})
                ops.append({'op':'swapcells','id':b[-1][2],'cells':[[4,1,5],[5,1,5],[6,1,5]]})
                return ops
        ops.append({'op':'click','left':'左クリック' in text,'sneak':'スニーク' in text or 'しゃがみ' in text,'item':held})
        # tier upgrades / transformations
        m = re.search(r'([^、。を]+?)に変わる', text)
        if m:
            tgt = find_all(m.group(1), names)
            if tgt: ops.append({'op':'swap','id':tgt[-1][2]})
        return ops
    # show / place things mentioned
    if 'レーザーの先' in text:
        ops.append({'op':'block','pos':[3,1,0],'id':'minecraft:stone'}); ops.append({'op':'breakat','pos':[3,1,0]}); return ops
    if any(o['op'] in ('mob','mobact') for o in ops): return ops
    INFRA = {'mekanism:basic_thermodynamic_conductor','mekanism:basic_universal_cable','mekanism:basic_pressurized_tube','mekanism:basic_mechanical_pipe','mekanism:basic_logistical_transporter'}
    shown = [rid for p, n, rid in found if rid != focus and rid not in INFRA and not (state.get('machine') == rid)]
    if shown:
        ops.append({'op':'show','ids':[r for r in shown][:6]})
    else:
        ops.append({'op':'pulse'})
    return ops

out_entries = []; lang = {}
for e in entries:
    sid = e['id'].replace(':', '_')
    state = {}
    steps = []
    n = 0
    for kind, val in e['steps']:
        if kind == 'T':
            n += 1; steps.append({'t': n}); lang[f'moremekaponder.ponder.{sid}.text_{n}'] = val
        else:
            ops = compile_action(e, val, state)
            ops = [o for o in ops if o['op'] != 'mobact'] + [o for o in ops if o['op'] == 'mobact']
            steps.append({'a': ops})
    lang[f'moremekaponder.ponder.{sid}.header'] = e['title']
    out_entries.append({'id': sid, 'items': e['items'], 'main': e['id'] if e['id'] in blocks else None, 'item': e['items'][0], 'steps': steps})

import os
os.makedirs(OUT+'/assets/moremekaponder/lang', exist_ok=True)
json.dump(out_entries, open(OUT+'/assets/moremekaponder/scenes.json','w'), ensure_ascii=False, separators=(',',':'))
for l in ['ja_jp','en_us']:
    json.dump(lang, open(OUT+f'/assets/moremekaponder/lang/{l}.json','w'), ensure_ascii=False, indent=1)
print(len(out_entries), 'scenes', len(lang), 'lang keys')
