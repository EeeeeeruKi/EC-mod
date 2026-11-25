"""Japanese names for Mekanism Extras (it ships only English and Chinese). Written into our jar as
assets/mekanism_extras/lang/ja_jp.json, so the scenes, the cards and the game use the same names."""
import json

TIERS = {'absolute': '絶対', 'supreme': '至高', 'cosmic': '宇宙', 'infinite': '無限'}
FACTORY = {'smelting': '精錬', 'enriching': '濃縮', 'crushing': '粉砕', 'compressing': '圧縮', 'combining': '結合',
           'purifying': '浄化', 'injecting': '注入', 'infusing': '吹込', 'sawing': '製材', 'oxidizing': '酸化',
           'dissolving': '溶解', 'washing': '洗浄', 'crystallizing': '結晶化', 'pressurised_reacting': '加圧反応',
           'centrifuging': '遠心分離', 'liquifying': '液化', 'pigment_extracting': '顔料抽出', 'painting': '塗装',
           'recycling': 'リサイクル', 'planting': '栽培', 'stamping': 'スタンピング', 'lathing': '旋盤',
           'rolling_mill': '圧延', 'pressing': 'プレス', 'replicating': '複製'}
TIERED_BLOCKS = {'bin': 'ビン', 'chemical_tank': '化学タンク', 'energy_cube': 'エネルギーキューブ', 'fluid_tank': '流体タンク',
                 'universal_cable': 'ユニバーサルケーブル', 'mechanical_pipe': 'メカニカルパイプ', 'pressurized_tube': '加圧チューブ',
                 'logistical_transporter': '物流トランスポーター', 'thermodynamic_conductor': '熱力学的コンダクター',
                 'induction_cell': 'インダクションセル', 'induction_provider': 'インダクションプロバイダ',
                 'radioactive_waste_barrel': '放射性廃棄物バレル'}
ALLOYS = {'radiance': '輝光', 'thermonuclear': '熱核', 'shining': '閃光', 'spectrum': 'スペクトル'}


def build():
    j = {}
    for t, tj in TIERS.items():
        for f, fj in FACTORY.items():
            j[f'block.mekanism_extras.{t}_{f}_factory'] = f'{tj}{fj}ファクトリー'
            j[f'container.mekanism_extras.{t}_{f}_factory'] = f'{tj}{fj}ファクトリー'
        for b, bj in TIERED_BLOCKS.items():
            j[f'block.mekanism_extras.{t}_{b}'] = f'{tj}{bj}'
        j[f'item.mekanism_extras.{t}_tier_installer'] = f'{tj}ティアインストーラー'
        j[f'item.mekanism_extras.{t}_control_circuit'] = f'{tj}制御回路'
    for a, aj in ALLOYS.items():
        j[f'item.mekanism_extras.alloy_{a}'] = f'{aj}合金'
        j[f'item.mekanism_extras.enriched_{a}'] = f'濃縮{aj}'
        j[f'chemical.mekanism_extras.{a}'] = aj
    j.update({
        'item.mekanism_extras.enriched_osmium': '濃縮オスミウム',
        'item.mekanism_extras.enriched_lead': '濃縮鉛',
        'item.mekanism_extras.dust_radiance': '輝光の粉',
        'chemical.mekanism_extras.lead': '鉛',
        'chemical.mekanism_extras.molten_thermonuclear': '溶融熱核',
        'chemical.mekanism_extras.dirty_naquadah': '汚れたナクアダの懸濁液',
        'chemical.mekanism_extras.clean_naquadah': '純粋なナクアダの懸濁液',
        'chemical.mekanism_extras.naquadah_hexafluoride': '六フッ化ナクアダ',
        'chemical.mekanism_extras.fluorinated_naquadah_uranium_fuel': 'フッ化ナクアダウラン燃料',
        'chemical.mekanism_extras.naquadah_uranium_fuel': 'ナクアダウラン燃料',
        'chemical.mekanism_extras.rich_naquadah_fuel': '高濃度ナクアダ燃料',
        'chemical.mekanism_extras.rich_uranium_fuel': '高濃度ウラン燃料',
        'chemical.mekanism_extras.polonium_containing_steam': 'ポロニウムを含む蒸気',
        'chemical.mekanism_extras.polonium-208': 'ポロニウム208',
        'block.mekanism_extras.naquadah_hexafluoride': '液体の六フッ化ナクアダ',
        'block.mekanism_extras.fluorinated_naquadah_uranium_fuel': '液体のフッ化ナクアダウラン燃料',
        'block.mekanism_extras.rich_naquadah_fuel': '液体の高濃度ナクアダ燃料',
        'block.mekanism_extras.rich_uranium_fuel': '液体の高濃度ウラン燃料',
        'block.mekanism_extras.polonium_containing_solution': 'ポロニウムを含む溶液',
        'block.mekanism_extras.naquadah_ore': 'ナクアダ鉱石',
        'block.mekanism_extras.end_naquadah_ore': 'エンドのナクアダ鉱石',
        'block.mekanism_extras.block_naquadah': 'ナクアダブロック',
        'block.mekanism_extras.block_raw_naquadah': 'ナクアダの原石ブロック',
        'item.mekanism_extras.raw_naquadah': 'ナクアダの原石',
        'item.mekanism_extras.dust_naquadah': 'ナクアダの粉',
        'item.mekanism_extras.dirty_dust_naquadah': '汚れたナクアダの粉',
        'item.mekanism_extras.clump_naquadah': 'ナクアダの凝塊',
        'item.mekanism_extras.shard_naquadah': 'ナクアダの欠片',
        'item.mekanism_extras.crystal_naquadah': 'ナクアダの結晶',
        'item.mekanism_extras.ingot_naquadah': 'ナクアダインゴット',
        'item.mekanism_extras.nugget_naquadah': 'ナクアダ塊',
        'item.mekanism_extras.naquadah_hohlraum': 'ナクアダホールラウム',
        'item.mekanism_extras.upgrade_stack': 'スタックアップグレード',
        'item.mekanism_extras.upgrade_ionic_membrane': 'イオン膜アップグレード',
        'item.mekanism_extras.upgrade_creative': 'クリエイティブアップグレード',
        'upgrade.mekanism_extras.stack': 'スタック',
        'upgrade.mekanism_extras.stack.description': '同時に行う作業の数（並列数）を増やします。',
        'upgrade.mekanism_extras.ionic_membrane': 'イオン膜',
        'upgrade.mekanism_extras.ionic_membrane.description': '液体からイオンを取り出せるようになります。',
        'upgrade.mekanism_extras.creative': 'クリエイティブ',
        'upgrade.mekanism_extras.creative.description': '機械が電力を使わなくなり、作業時間もかからなくなります。',
        'gui.mekanism_extras.upgrades.stack': '並列数：%1$s倍',
        'item.mekanism_extras.qio_drive_collapse': 'コラプスQIOドライブ',
        'item.mekanism_extras.qio_drive_gamma': 'ガンマQIOドライブ',
        'item.mekanism_extras.qio_drive_black_hole': 'ブラックホールQIOドライブ',
        'item.mekanism_extras.qio_drive_singularity': 'シンギュラリティQIOドライブ',
        'block.mekanism_extras.advanced_electric_pump': '発展型電動ポンプ',
        'container.mekanism_extras.advanced_electric_pump': '発展型電動ポンプ',
        'block.mekanism_extras.reinforced_induction_casing': '強化インダクションケーシング',
        'block.mekanism_extras.reinforced_induction_port': '強化インダクションポート',
        'block.mekanism_extras.naquadah_reactor_controller': 'ナクアダ炉制御装置',
        'block.mekanism_extras.naquadah_reactor_casing': 'ナクアダ炉ケーシング',
        'block.mekanism_extras.naquadah_reactor_port': 'ナクアダ炉ポート',
        'block.mekanism_extras.naquadah_reactor_logic_adapter': 'ナクアダ炉ロジックアダプター',
        'block.mekanism_extras.lead_coated_laser_focus_matrix': '鉛コーティングのレーザー集束マトリックス',
        'description.mekanism_extras.naquadah_ore': '地球にはないはずのナクアダです。オーバーワールドの岩盤の近くで、まれに見つかります。',
        'description.mekanism_extras.end_naquadah_ore': 'オーバーワールドよりも、エンドのほうがナクアダ鉱石を見つけやすいです。奈落の近くを探してみましょう。',
    })
    return j


def write(out_dir):
    import os
    os.makedirs(out_dir + '/assets/mekanism_extras/lang', exist_ok=True)
    json.dump(build(), open(out_dir + '/assets/mekanism_extras/lang/ja_jp.json', 'w'), ensure_ascii=False, indent=1)


if __name__ == '__main__':
    json.dump(build(), open('/home/claude/dl/mek/extras_ja.json', 'w'), ensure_ascii=False, indent=1)
    print(len(build()))
