"""Hand-made layouts for scenes the text-to-ops guesser gets wrong.

OVERRIDES[scene_id][n] replaces the ops of the n-th action line (0-based) of that scene.
Coordinates: the plate is x 0..6, z 0..6, ground level y = 1. The camera looks from the
north-west (low x, low z), so north (z-) and west (x-) faces show and things there stay visible.
"""

# ---------------------------------------------------------------- ids
CUBE = 'mekanism:creative_energy_cube'
CABLE = 'mekanism:basic_universal_cable'
TUBE = 'mekanism:basic_pressurized_tube'
PIPE = 'mekanism:basic_mechanical_pipe'
TRANS = 'mekanism:basic_logistical_transporter'
COND = 'mekanism:basic_thermodynamic_conductor'
CTANK = 'mekanism:basic_chemical_tank'
FTANK = 'mekanism:basic_fluid_tank'
CHEST = 'minecraft:chest'
STONE = 'minecraft:stone'
SMELTER = 'mekanism:energized_smelter'
CHAMBER = 'mekanism:enrichment_chamber'
CRUSHER = 'mekanism:crusher'
SEP = 'mekanism:electrolytic_separator'
INFUSER = 'mekanism:chemical_infuser'
PUMP = 'mekanism:electric_pump'
MINER = 'mekanism:digital_miner'
HEATGEN = 'mekanismgenerators:heat_generator'
CONFIG = 'mekanism:configurator'

WATER = 0x3F76E4
H2 = 0xF0F8FF
O2 = 0x6CE2FF
CL = 0xCFE800
SO2 = 0xA99D90
ACID = 0x82802B
HEAT = 0xFF5020
ENERGY = 0x3CFE9A
DIRTY = 0x7F6E5C
CLEAN = 0xC9C9C9

MEKASUIT = [['HEAD', 'mekanism:mekasuit_helmet'], ['CHEST', 'mekanism:mekasuit_bodyarmor'],
            ['LEGS', 'mekanism:mekasuit_pants'], ['FEET', 'mekanism:mekasuit_boots']]


# ---------------------------------------------------------------- helpers
def B(pos, bid, **k):
    d = {'op': 'block', 'pos': pos, 'id': bid}
    d.update(k)
    return d


def L(bid, cells):
    return {'op': 'line', 'id': bid, 'cells': cells}


def blocks(pairs, step=None):
    d = {'op': 'blocks', 'list': pairs}
    if step: d['step'] = step
    return d


def floor(bid, cells, step=None):
    d = {'op': 'floor', 'id': bid, 'cells': cells}
    if step: d['step'] = step
    return d


def remove(cells):
    return {'op': 'remove', 'cells': cells}


def destroy(cells, step=None):
    d = {'op': 'destroy', 'cells': cells}
    if step: d['step'] = step
    return d


def carry(item, path, count=1, speed=6, gap=None, keep=False, trail=None, wait=None, mob=None):
    d = {'op': 'carry', 'item': item, 'path': path, 'count': count, 'speed': speed, 'keep': keep}
    if gap is not None: d['gap'] = gap
    if trail: d['trail'] = trail
    if wait is not None: d['wait'] = wait
    if mob is not None: d['mob'] = mob
    return d


def movers(lst, speed=6, wait=None):
    d = {'op': 'carry', 'movers': [{'item': i, 'path': p, 'delay': dl} for i, p, dl in lst], 'speed': speed}
    if wait is not None: d['wait'] = wait
    return d


def stream(path, color=None, particle=None, rounds=2, speed=3, wait=None):
    d = {'op': 'stream', 'path': path, 'rounds': rounds, 'speed': speed}
    if color is not None: d['color'] = color
    if particle: d['particle'] = particle
    if wait is not None: d['wait'] = wait
    return d


def active(pos, on=True, wait=None):
    d = {'op': 'active', 'pos': pos, 'on': on}
    if wait is not None: d['wait'] = wait
    return d


def click(at, left=False, sneak=False, item=None):
    return {'op': 'click', 'at': at, 'left': left, 'sneak': sneak, 'item': item}


def indicate(pos, wait=None):
    d = {'op': 'indicate', 'pos': pos}
    if wait is not None: d['wait'] = wait
    return d


def item(iid, at, motion=None, float_=False, wait=None):
    d = {'op': 'item', 'id': iid, 'at': at}
    if motion: d['motion'] = motion
    if float_: d['float'] = True
    if wait is not None: d['wait'] = wait
    return d


def actor(**k):
    d = {'op': 'actor'}
    d.update(k)
    return d


def panel(title, ins=(), outs=(), gauge=None, at=None, top=None, src=None):
    d = {'op': 'panel', 'title': title, 'in': [list(x) for x in ins], 'out': [list(x) for x in outs], 'gauge': gauge}
    if at is not None: d['at'] = at
    if top is not None: d['top'] = top
    if src is not None: d['src'] = src
    return d


def beam(a, b, width=0.06, color=0xFFFF3030, wait=None):
    d = {'op': 'beam', 'from': a, 'to': b, 'width': width, 'color': color}
    if wait is not None: d['wait'] = wait
    return d


def wait(t):
    return {'op': 'wait', 'ticks': t}


def power_west():
    return [B([0, 1, 3], CUBE), L(CABLE, [[1, 1, 3], [2, 1, 3]])]


def power_north():
    return [B([3, 1, 0], CUBE), L(CABLE, [[3, 1, 1], [3, 1, 2]])]


# digital miner: 3 x 3 x 2, facing west so its front shows; power comes in on the north side
MINER_SET = [B([3, 1, 3], MINER, facing='west'), B([3, 1, 0], CUBE), L(CABLE, [[3, 1, 1]])]
ORES = [([0, 1], 'minecraft:iron_ore', 'minecraft:raw_iron'), ([1, 5], 'minecraft:gold_ore', 'minecraft:raw_gold'),
        ([5, 0], 'minecraft:diamond_ore', 'minecraft:diamond'), ([6, 5], 'minecraft:iron_ore', 'minecraft:raw_iron'),
        ([0, 4], 'minecraft:coal_ore', 'minecraft:coal'), ([6, 1], 'minecraft:redstone_ore', 'minecraft:redstone')]


def ores_in():
    return [floor(o, [c]) for c, o, _ in ORES]


def ores_out(as_blocks):
    out = [destroy([[c[0], 0, c[1]] for c, _, _ in ORES], step=3)]
    lst = []
    for i, (c, o, drop) in enumerate(ORES):
        x, z = c[0] + 0.5, c[1] + 0.5
        lst.append((o if as_blocks else drop, [[x, 0.4, z], [x, 1.6, z], [3.5, 2.6, 3.5]], i * 3))
    out.append(movers(lst, speed=5))
    return out


# pool of water standing at ground level, south-east of the middle
POOL = [[[x, 1, z], 'minecraft:water'] for x in (4, 5, 6) for z in (4, 5, 6)]

OVERRIDES = {
    # ------------------------------------------------------------ storage
    'mekanism_basic_bin': {
        0: [B([3, 1, 3], 'mekanism:basic_bin'), click([3.5, 2.0, 3.0], item='minecraft:cobblestone'),
            carry('minecraft:cobblestone', [[3.5, 1.9, 2.3], [3.5, 1.4, 2.95]], speed=8), indicate([3, 1, 3])],
        1: [click([3.5, 2.0, 3.0], left=True), item('minecraft:cobblestone', [3.5, 1.3, 2.8], motion=[0, 0.15, -0.1], wait=20)],
        2: [L(TRANS, [[4, 1, 3], [5, 1, 3]]), B([6, 1, 3], CHEST),
            carry('minecraft:cobblestone', [[3.9, 1.3, 3.5], [6.1, 1.3, 3.5]], count=3, speed=6)],
    },
    'mekanism_basic_chemical_tank': {
        0: [B([3, 1, 3], CTANK), B([6, 1, 3], SEP), L(TUBE, [[5, 1, 3], [4, 1, 3]]),
            B([6, 1, 0], CUBE), L(CABLE, [[6, 1, 1], [6, 1, 2]]), B([6, 1, 6], FTANK), L(PIPE, [[6, 1, 5], [6, 1, 4]]),
            active([6, 1, 3]), stream([[6.5, 1.5, 6.0], [6.5, 1.5, 4.0]], color=WATER, rounds=1),
            stream([[6.0, 1.5, 3.5], [3.8, 1.5, 3.5]], color=H2, rounds=3), indicate([3, 1, 3])],
        1: [panel(CTANK, ins=[['mekanism:jetpack', 1]], gauge=[['@chemical.mekanism.hydrogen', H2, 'out']])],
    },
    'mekanism_basic_fluid_tank': {
        0: [B([3, 1, 3], FTANK), click([3.5, 2.0, 3.5], item='minecraft:water_bucket'),
            stream([[3.5, 2.2, 3.5], [3.5, 1.2, 3.5]], color=WATER, rounds=3)],
        1: [B([3, 2, 3], FTANK), stream([[3.5, 1.5, 3.5], [3.5, 2.7, 3.5]], color=WATER, rounds=3), indicate([3, 2, 3])],
        2: [click([3.5, 3.0, 3.5], sneak=True, item=CONFIG), stream([[3.5, 2.7, 3.5], [3.5, 1.3, 3.5]], color=WATER, rounds=3)],
        3: [{'op': 'mine', 'tool': 'minecraft:iron_pickaxe', 'place': [], 'targets': [[3, 2, 3]], 'mode': 'one',
             'stand': [2.2, 1, 3.5], 'yaw': -90, 'drop': FTANK}],
    },
    # ------------------------------------------------------------ transmitters
    'mekanism_basic_logistical_transporter': {
        0: [B([3, 1, 3], CHAMBER), B([6, 1, 3], CUBE), L(CABLE, [[5, 1, 3], [4, 1, 3]]),
            B([0, 1, 3], CHEST), L(TRANS, [[1, 1, 3], [2, 1, 3]])],
        1: [click([1.2, 1.9, 3.5], sneak=True, item=CONFIG), indicate([1, 1, 3])],
        2: [carry('minecraft:iron_ore', [[0.9, 1.3, 3.5], [3.0, 1.3, 3.5]], count=3, speed=6), active([3, 1, 3], wait=30),
            active([3, 1, 3], False)],
        3: [L(TRANS, [[3, 1, 2], [3, 1, 1]]), B([3, 1, 0], CHEST), active([3, 1, 3], wait=5),
            carry('mekanism:dust_iron', [[3.5, 1.3, 3.0], [3.5, 1.3, 0.9]], count=3, speed=6), active([3, 1, 3], False)],
    },
    'mekanism_basic_mechanical_pipe': {
        0: [floor('minecraft:water', [[x, z] for x in (0, 1, 2) for z in (2, 3, 4)]), B([1, 1, 3], PUMP),
            B([1, 1, 0], CUBE), L(CABLE, [[1, 1, 1], [1, 1, 2]]), L(PIPE, [[2, 1, 3], [3, 1, 3], [4, 1, 3]]), B([5, 1, 3], FTANK),
            active([1, 1, 3]), stream([[1.5, 0.9, 3.5], [1.5, 1.5, 3.5], [5.0, 1.5, 3.5]], color=WATER, rounds=3)],
        1: [click([4.5, 1.9, 3.5], sneak=True, item=CONFIG), stream([[5.0, 1.5, 3.5], [2.0, 1.5, 3.5]], color=WATER, rounds=3)],
    },
    'mekanism_basic_pressurized_tube': {
        0: [B([1, 1, 3], SEP), B([1, 1, 0], CUBE), L(CABLE, [[1, 1, 1], [1, 1, 2]]), B([1, 1, 6], FTANK), L(PIPE, [[1, 1, 5], [1, 1, 4]]),
            L(TUBE, [[2, 1, 3], [3, 1, 3], [4, 1, 3]]), B([5, 1, 3], CTANK), active([1, 1, 3]),
            stream([[1.5, 1.5, 6.0], [1.5, 1.5, 4.0]], color=WATER, rounds=1), stream([[2.0, 1.5, 3.5], [5.0, 1.5, 3.5]], color=H2, rounds=3)],
        1: [click([4.5, 1.9, 3.5], sneak=True, item=CONFIG), stream([[5.0, 1.5, 3.5], [2.0, 1.5, 3.5]], color=H2, rounds=3)],
    },
    'mekanism_basic_thermodynamic_conductor': {
        0: [B([1, 1, 3], 'mekanism:resistive_heater'), B([1, 1, 0], CUBE), L(CABLE, [[1, 1, 1], [1, 1, 2]]),
            L(COND, [[2, 1, 3], [3, 1, 3], [4, 1, 3]]), B([5, 1, 3], SMELTER), active([1, 1, 3]),
            stream([[2.0, 1.5, 3.5], [5.0, 1.5, 3.5]], color=HEAT, rounds=3, speed=6), active([5, 1, 3])],
    },
    'mekanism_basic_universal_cable': {
        0: [B([1, 1, 3], HEATGEN, alt=CUBE), L(CABLE, [[2, 1, 3], [3, 1, 3], [4, 1, 3]]), B([5, 1, 3], SMELTER), active([1, 1, 3]),
            stream([[2.0, 1.5, 3.5], [5.0, 1.5, 3.5]], color=ENERGY, rounds=2), active([5, 1, 3])],
        1: [L(CABLE, [[3, 1, 2], [3, 1, 1]]), B([3, 1, 0], SMELTER), stream([[3.5, 1.5, 3.5], [3.5, 1.5, 1.0]], color=ENERGY, rounds=2),
            active([3, 1, 0])],
    },
    'mekanism_diversion_transporter': {
        0: [B([0, 1, 3], CHEST), L(TRANS, [[1, 1, 3], [2, 1, 3]]), B([3, 1, 3], 'mekanism:diversion_transporter'),
            L(TRANS, [[4, 1, 3], [5, 1, 3]]), B([6, 1, 3], CHEST),
            B([3, 1, 2], 'minecraft:lever', props={'face': 'floor', 'facing': 'north'}),
            carry('minecraft:iron_ingot', [[0.9, 1.3, 3.5], [6.1, 1.3, 3.5]], count=3, speed=5)],
        1: [{'op': 'lever', 'pos': [3, 1, 2], 'on': True},
            carry('minecraft:iron_ingot', [[0.9, 1.3, 3.5], [2.7, 1.3, 3.5], [0.9, 1.3, 3.5]], count=2, speed=5)],
    },
    'mekanism_logistical_sorter': {
        0: [B([1, 1, 3], CHEST), B([2, 1, 3], 'mekanism:logistical_sorter', facing='east'),
            L(TRANS, [[3, 1, 3], [4, 1, 3], [5, 1, 3]]), B([6, 1, 3], CHEST)],
        1: [item('minecraft:cobblestone', [1.3, 2.1, 3.3], float_=True, wait=2), item('minecraft:oak_log', [1.7, 2.1, 3.7], float_=True, wait=2),
            item('minecraft:iron_ore', [1.5, 2.2, 3.5], float_=True, wait=10),
            carry('minecraft:iron_ore', [[1.9, 1.3, 3.5], [6.1, 1.3, 3.5]], count=3, speed=5)],
        2: [L(TRANS, [[4, 1, 2], [4, 1, 1]]), B([4, 1, 0], CHEST),
            carry('minecraft:iron_ore', [[2.5, 1.3, 3.5], [4.5, 1.3, 3.5], [4.5, 1.3, 0.9]], count=3, speed=5, trail='red')],
    },
    'mekanism_restrictive_transporter': {
        0: [B([0, 1, 3], CHEST), L(TRANS, [[1, 1, 3], [2, 1, 3], [3, 1, 3]]), L(TRANS, [[3, 1, 2], [3, 1, 1]]), B([3, 1, 0], CHEST),
            B([4, 1, 3], 'mekanism:restrictive_transporter'), L(TRANS, [[5, 1, 3]]), B([6, 1, 3], CHEST)],
        1: [carry('minecraft:iron_ingot', [[0.9, 1.3, 3.5], [3.5, 1.3, 3.5], [3.5, 1.3, 0.9]], count=3, speed=5)],
        2: [indicate([3, 1, 0], wait=10),
            carry('minecraft:iron_ingot', [[0.9, 1.3, 3.5], [3.5, 1.3, 3.5], [6.1, 1.3, 3.5]], count=3, speed=5)],
    },
    # ------------------------------------------------------------ machines with chemicals on two sides
    'mekanism_chemical_infuser': {
        0: [B([3, 1, 3], INFUSER), *power_north(), B([6, 1, 3], CTANK), L(TUBE, [[5, 1, 3], [4, 1, 3]]),
            B([0, 1, 3], CTANK), L(TUBE, [[1, 1, 3], [2, 1, 3]]),
            stream([[6.5, 1.5, 3.5], [4.0, 1.5, 3.5]], color=H2, rounds=2), stream([[0.5, 1.5, 3.5], [3.0, 1.5, 3.5]], color=CL, rounds=2),
            active([3, 1, 3])],
        1: [stream([[3.5, 1.6, 3.5], [3.5, 2.8, 3.5]], color=0xA8F1E9, rounds=4), active([3, 1, 3], False)],
    },
    'mekanism_pigment_mixer': {
        0: [B([3, 1, 3], 'mekanism:pigment_mixer'), *power_north(), B([6, 1, 3], CTANK), L(TUBE, [[5, 1, 3], [4, 1, 3]]),
            B([0, 1, 3], CTANK), L(TUBE, [[1, 1, 3], [2, 1, 3]]),
            stream([[6.5, 1.5, 3.5], [4.0, 1.5, 3.5]], color=0xB02E26, rounds=2), stream([[0.5, 1.5, 3.5], [3.0, 1.5, 3.5]], color=0xF9FFFE, rounds=2),
            active([3, 1, 3])],
        1: [stream([[3.5, 1.6, 3.5], [3.5, 3.2, 3.5]], color=0xF38BAA, rounds=4), active([3, 1, 3], False)],
    },
    'mekanism_electrolytic_separator': {
        0: [B([3, 1, 3], SEP), *power_north(), B([3, 1, 6], FTANK), L(PIPE, [[3, 1, 5], [3, 1, 4]]),
            stream([[3.5, 1.5, 6.0], [3.5, 1.5, 4.0]], color=WATER, rounds=2), active([3, 1, 3])],
        1: [stream([[3.5, 1.6, 3.5], [3.5, 2.8, 3.5]], color=H2, rounds=3), stream([[3.5, 1.6, 3.5], [3.5, 2.8, 3.5]], color=O2, rounds=3)],
        2: [B([6, 1, 3], CTANK), L(TUBE, [[5, 1, 3], [4, 1, 3]]), B([0, 1, 3], CTANK), L(TUBE, [[1, 1, 3], [2, 1, 3]]),
            stream([[4.0, 1.5, 3.5], [6.5, 1.5, 3.5]], color=H2, rounds=2), stream([[3.0, 1.5, 3.5], [0.5, 1.5, 3.5]], color=O2, rounds=2)],
    },
    'mekanism_chemical_dissolution_chamber': {
        2: [L(TUBE, [[3, 1, 2]]), B([3, 1, 1], 'mekanism:chemical_washer'), B([2, 1, 1], CUBE),
            L(TUBE, [[4, 1, 1]]), B([5, 1, 1], 'mekanism:chemical_crystallizer'), B([6, 1, 1], CUBE),
            stream([[3.5, 1.5, 3.0], [3.5, 1.5, 1.5]], color=DIRTY, rounds=2), active([3, 1, 1]),
            stream([[4.0, 1.5, 1.5], [5.0, 1.5, 1.5]], color=CLEAN, rounds=2), active([5, 1, 1])],
    },
    'mekanism_chemical_oxidizer': {
        1: [L(TUBE, [[4, 1, 3], [5, 1, 3]]), B([6, 1, 3], INFUSER), B([6, 1, 0], CUBE), L(CABLE, [[6, 1, 1], [6, 1, 2]]),
            stream([[4.0, 1.5, 3.5], [6.0, 1.5, 3.5]], color=SO2, rounds=3), active([6, 1, 3])],
    },
    'mekanism_dust_sulfur': {
        1: [L(TUBE, [[4, 1, 3]]), B([5, 1, 3], INFUSER), B([6, 1, 3], CUBE), L(TUBE, [[5, 1, 2]]), B([5, 1, 1], INFUSER), B([6, 1, 1], CUBE),
            stream([[4.0, 1.5, 3.5], [5.0, 1.5, 3.5]], color=SO2, rounds=2), active([5, 1, 3]),
            stream([[5.5, 1.5, 3.0], [5.5, 1.5, 2.0]], color=0xCE6C6C, rounds=2), active([5, 1, 1]),
            stream([[5.5, 2.0, 1.5], [5.5, 3.0, 1.5]], color=ACID, rounds=3)],
    },
    # ------------------------------------------------------------ machine scenes needing the right surroundings
    'mekanism_configuration_card': {
        0: [B([3, 1, 3], CHAMBER), *power_west(), B([5, 1, 3], CHAMBER),
            click([3.5, 2.0, 3.5], sneak=True, item='mekanism:configuration_card'), indicate([3, 1, 3])],
        1: [click([5.5, 2.0, 3.5], item='mekanism:configuration_card'), indicate([5, 1, 3])],
    },
    'mekanism_configurator': {
        0: [B([3, 1, 3], CHAMBER), *power_west(), click([3.5, 2.0, 3.5], sneak=True, item=CONFIG), remove([[3, 1, 3]]),
            item(CHAMBER, [3.5, 1.3, 3.5], motion=[0, 0.2, 0], wait=20)],
        1: [B([3, 1, 3], CHAMBER), B([3, 1, 0], CHEST), L(TRANS, [[3, 1, 1], [3, 1, 2]]),
            click([3.5, 1.9, 1.1], sneak=True, item=CONFIG), indicate([3, 1, 1])],
        2: [click([3.5, 2.0, 3.0], item=CONFIG), indicate([3, 1, 3])],
    },
    'mekanism_dictionary': {
        0: [B([3, 1, 3], 'minecraft:iron_ore'), click([3.5, 2.0, 3.5], item='mekanism:dictionary'), indicate([3, 1, 3])],
    },
    'mekanism_digital_miner': {
        0: [*MINER_SET, *ores_in(), indicate([0, 0, 1], wait=4), indicate([6, 0, 5], wait=4), indicate([5, 0, 0])],
        1: ores_out(False),
        2: [floor(o, [c], step=2) for c, o, _ in ORES],
        3: ores_out(True),
        4: [B([5, 2, 3], CHEST), carry('minecraft:raw_iron', [[4.4, 2.4, 3.5], [5.5, 2.4, 3.5]], count=3, speed=5)],
    },
    'mekanism_upgrade_anchor': {
        0: [*MINER_SET, panel(MINER, ins=[['mekanism:upgrade_anchor', 1]], top=3.1)],
    },
    'mekanism_upgrade_stone_generator': {
        0: [*MINER_SET, panel(MINER, ins=[['mekanism:upgrade_stone_generator', 1]], top=3.1)],
        1: [*ores_in(), wait(10), destroy([[c[0], 0, c[1]] for c, _, _ in ORES], step=2),
            floor('minecraft:stone', [c for c, _, _ in ORES], step=2)],
    },
    'mekanism_upgrade_muffling': {
        0: [B([3, 1, 3], CRUSHER), *power_west(), panel(CRUSHER, ins=[['mekanism:upgrade_muffling', 1]])],
    },
    'mekanism_upgrade_filter': {
        0: [floor('minecraft:water', [[x, z] for x in (2, 3, 4) for z in (2, 3, 4)]), B([3, 1, 3], PUMP), *power_west(),
            panel(PUMP, ins=[['mekanism:upgrade_filter', 1]], gauge=[['@block.mekanism.heavy_water', 0x2A3FA0, 'out']], src=[3.5, 0.9, 3.5])],
    },
    'mekanism_dimensional_stabilizer': {
        0: [B([3, 1, 3], 'mekanism:dimensional_stabilizer'), *power_west(), active([3, 1, 3], wait=5),
            B([5, 1, 1], SMELTER), B([6, 1, 1], CUBE), B([1, 1, 5], CRUSHER), B([0, 1, 5], CUBE),
            active([5, 1, 1], wait=5), active([1, 1, 5])],
    },
    'mekanism_electric_pump': {
        0: [floor('minecraft:water', [[x, z] for x in (2, 3, 4) for z in (2, 3, 4)]), B([3, 1, 3], PUMP), *power_west(),
            active([3, 1, 3]), stream([[3.5, 0.9, 3.5], [3.5, 1.8, 3.5]], color=WATER, rounds=3)],
        1: [B([3, 2, 3], FTANK), stream([[3.5, 1.6, 3.5], [3.5, 2.7, 3.5]], color=WATER, rounds=3), indicate([3, 2, 3])],
        2: [panel(PUMP, ins=[['mekanism:upgrade_filter', 1]], top=3.1)],
    },
    'mekanism_fluidic_plenisher': {
        0: [floor('minecraft:air', [[x, z] for x in (2, 3, 4) for z in (2, 3, 4)]), B([3, 1, 3], 'mekanism:fluidic_plenisher'), *power_west(),
            B([6, 1, 3], FTANK), L(PIPE, [[5, 1, 3], [4, 1, 3]]), stream([[6.0, 1.5, 3.5], [4.0, 1.5, 3.5]], color=WATER, rounds=2)],
        1: [active([3, 1, 3], wait=5), stream([[3.5, 1.2, 3.5], [3.5, 0.3, 3.5]], color=WATER, rounds=2),
            floor('minecraft:water', [[3, 3], [2, 3], [4, 3], [3, 2], [3, 4], [2, 2], [4, 2], [2, 4], [4, 4]], step=4)],
    },
    'mekanism_energy_tablet': {
        1: [{'op': 'swap', 'id': CHAMBER}, panel(CHAMBER, ins=[['mekanism:energy_tablet', 1]])],
    },
    'mekanism_formulaic_assemblicator': {
        0: [B([3, 1, 3], 'mekanism:formulaic_assemblicator'), *power_west(),
            panel('mekanism:formulaic_assemblicator', ins=[['minecraft:wheat', 3], ['mekanism:crafting_formula', 1]],
                  outs=[['mekanism:crafting_formula', 1]])],
        1: [item('mekanism:crafting_formula', [3.5, 2.3, 3.5], float_=True, wait=40)],
        2: [panel('mekanism:formulaic_assemblicator', ins=[['minecraft:wheat', 3]])],
        3: [item('minecraft:wheat', [3.5, 1.3, 2.8], motion=[0, 0.15, -0.1], wait=25)],
        4: [B([3, 1, 0], CHEST), L(TRANS, [[3, 1, 1], [3, 1, 2]]),
            carry('minecraft:wheat', [[3.5, 1.3, 0.9], [3.5, 1.3, 3.0]], count=3, speed=5), active([3, 1, 3], wait=25),
            active([3, 1, 3], False, wait=2), item('minecraft:bread', [3.5, 2.1, 3.5], motion=[0, 0.2, 0], wait=25)],
    },
    'mekanism_quantum_entangloporter': {
        0: [B([1, 1, 4], 'mekanism:quantum_entangloporter'), B([5, 1, 1], 'mekanism:quantum_entangloporter'),
            click([1.5, 2.0, 4.5]), click([5.5, 2.0, 1.5]), indicate([1, 1, 4], wait=4), indicate([5, 1, 1])],
        1: [B([1, 1, 6], CHEST), L(TRANS, [[1, 1, 5]]), L(TRANS, [[5, 1, 2]]), B([5, 1, 3], CHEST),
            carry('minecraft:iron_ingot', [[1.5, 1.3, 6.1], [1.5, 1.3, 4.6]], count=2, speed=5, wait=2),
            carry('minecraft:iron_ingot', [[5.5, 1.3, 1.9], [5.5, 1.3, 3.1]], count=2, speed=5)],
        2: [B([1, 1, 2], CUBE), L(CABLE, [[1, 1, 3]]), B([6, 1, 1], SMELTER),
            stream([[1.5, 1.5, 2.5], [1.5, 1.5, 4.0]], color=ENERGY, rounds=2), active([6, 1, 1])],
    },
    'mekanism_chargepad': {
        1: [actor(at=[1.5, 1, 1.0], yaw=-30, path=[[3.5, 1.07, 3.4, 20]], pose='WALK', wait=2), actor(pose='STAND', wait=2),
            active([3, 1, 3]), indicate([3, 1, 3])],
        3: [actor(path=[[1.5, 1, 1.0, 15, 150]], pose='WALK', wait=2), actor(pose='STAND', wait=2), active([3, 1, 3], False, wait=2),
            {'op': 'mobact', 'kind': 'move', 'to': [3.5, 1, 3.5]}, active([3, 1, 3])],
    },
    'mekanism_laser_amplifier': {
        0: [B([3, 1, 3], 'mekanism:laser_amplifier', facing='east'),
            B([0, 1, 3], 'mekanism:laser', facing='east'), B([0, 1, 1], CUBE), L(CABLE, [[0, 1, 2]]),
            B([3, 1, 0], 'mekanism:laser', facing='south'), B([5, 1, 0], CUBE), L(CABLE, [[4, 1, 0]]),
            beam([1.0, 1.5, 3.5], [3.0, 1.5, 3.5]), beam([3.5, 1.5, 1.0], [3.5, 1.5, 3.0])],
        1: [beam([4.0, 1.5, 3.5], [6.95, 1.5, 3.5], width=0.14)],
    },
    'mekanism_precision_sawmill': {
        1: [active([3, 1, 3], wait=20), active([3, 1, 3], False, wait=2),
            item('mekanism:sawdust', [3.5, 1.3, 2.85], motion=[0, 0.18, -0.12], wait=25)],
    },
    'mekanism_solar_neutron_activator': {
        1: [B([3, 3, 3], STONE), active([3, 1, 3], False)],
    },
    'mekanism_network_reader': {
        0: [B([3, 1, 3], SMELTER), *power_west(), active([3, 1, 3], wait=5),
            click([1.5, 1.9, 3.5], item='mekanism:network_reader'), indicate([1, 1, 3])],
    },
    'mekanism_resistive_heater': {
        1: [L(COND, [[4, 1, 3], [5, 1, 3]]), B([6, 1, 3], HEATGEN, alt=COND),
            stream([[4.0, 1.5, 3.5], [6.0, 1.5, 3.5]], color=HEAT, rounds=3, speed=5)],
    },
    'mekanism_fuelwood_heater': {
        1: [L(COND, [[4, 1, 3], [5, 1, 3]]), B([6, 1, 3], HEATGEN, alt=COND),
            stream([[4.0, 1.5, 3.5], [6.0, 1.5, 3.5]], color=HEAT, rounds=3, speed=5)],
    },
    'mekanism_portable_teleporter': {
        0: [B([5, 1, 4], 'mekanism:teleporter'),
            blocks([[[x, y, 4], 'mekanism:teleporter_frame'] for x, y in
                    [(4, 1), (6, 1), (4, 2), (6, 2), (4, 3), (6, 3), (4, 4), (5, 4), (6, 4)]], step=2),
            B([5, 1, 6], CUBE), L(CABLE, [[5, 1, 5]]), click([5.5, 2.0, 4.0]), indicate([5, 1, 4])],
        1: [actor(at=[1.5, 1, 1.5], yaw=-135, equip=[['MAINHAND', 'mekanism:portable_teleporter']], pose='USE_ITEM', wait=15),
            stream([[1.5, 1.2, 1.5], [1.5, 2.8, 1.5]], particle='portal', rounds=3, speed=2),
            {'op': 'flash', 'color': 0x60D0A0FF},
            actor(at=[5.5, 2.0, 4.5], jump=True, yaw=180, pose='STAND'),
            stream([[5.5, 2.2, 4.5], [5.5, 3.6, 4.5]], particle='portal', rounds=2, speed=2)],
    },
    'mekanism_hazmat_mask': {
        1: [B([3, 1, 3], 'mekanism:radioactive_waste_barrel'),
            actor(at=[0.5, 1, 3.5], yaw=-90, equip=[['HEAD', 'mekanism:hazmat_mask'], ['CHEST', 'mekanism:hazmat_gown'],
                                                    ['LEGS', 'mekanism:hazmat_pants'], ['FEET', 'mekanism:hazmat_boots']]),
            actor(path=[[2.1, 1, 3.5, 15, -90]], pose='WALK', wait=2), actor(pose='STAND', wait=20)],
    },
    'mekanism_electric_bow': {
        0: [B([5, 1, 1], 'minecraft:target'),
            actor(at=[1.5, 1, 4.5], yaw=-131, equip=[['MAINHAND', 'mekanism:electric_bow']], pose='USE_ITEM', wait=15),
            carry('minecraft:arrow', [[2.0, 2.4, 4.0], [5.1, 1.6, 1.9]], speed=2), indicate([5, 1, 1])],
        1: [actor(pose='STAND', wait=5), actor(pose='USE_ITEM', wait=15),
            carry('minecraft:arrow', [[2.0, 2.4, 4.0], [5.1, 1.6, 1.9]], speed=2, trail='flame'), indicate([5, 1, 1])],
    },
    'mekanism_flamethrower': {
        1: [actor(at=[1.5, 1, 5.5], yaw=180, equip=[['MAINHAND', 'mekanism:flamethrower']], pose='USE_ITEM', wait=5),
            stream([[1.5, 2.2, 5.0], [1.5, 1.8, 1.8]], particle='flame', rounds=5, speed=1)],
    },
    'mekanism_jetpack': {
        2: [actor(pose='FLY', wait=5), stream([[0.5, 4.1, 5.2], [0.5, 3.2, 5.2]], particle='flame', rounds=5, speed=1)],
    },
    'mekanism_free_runners': {
        0: [L(STONE, [[2, 1, 5], [3, 1, 5], [4, 1, 5]]),
            actor(at=[0.5, 1, 5.5], yaw=-90, equip=[['FEET', 'mekanism:free_runners']], pose='WALK',
                  path=[[1.65, 1, 5.5, 10], [1.68, 2.02, 5.5, 5], [4.5, 2.02, 5.5, 14]], wait=2), actor(pose='STAND', wait=10)],
        1: [L(STONE, [[5, 1, 2], [5, 2, 2], [5, 3, 2]]), actor(at=[5.5, 4, 2.5], jump=True, yaw=180, pose='WALK',
            path=[[5.5, 4, 1.3, 8], [5.5, 1, 0.9, 7]], wait=2), actor(pose='STAND', wait=2), indicate([5, 0, 0])],
    },
    'mekanism_mekasuit_helmet': {
        0: [actor(at=[1.5, 1, 1.5], yaw=135, equip=MEKASUIT, wait=10), {'op': 'flash', 'color': 0x40FF0000},
            stream([[1.5, 2.0, 1.5], [1.5, 3.2, 1.5]], color=ENERGY, rounds=3)],
    },
    'mekanism_mekasuit_bodyarmor': {
        1: [actor(at=[1.5, 1, 1.5], yaw=135, equip=MEKASUIT, pose='FLY',
                  path=[[1.5, 3, 1.5, 20], [4.5, 3, 1.5, 20, -90], [4.5, 3, 4.5, 20, 0], [1.5, 3, 4.5, 20, 90], [1.5, 3, 1.5, 20, 180]])],
    },
    'mekanism_mekasuit_boots': {
        1: [actor(at=[1.5, 1, 1.5], yaw=135, equip=MEKASUIT, wait=5),
            actor(path=[[1.5, 4.0, 1.5, 8], [1.5, 1, 1.5, 12]], wait=10)],
    },
    'mekanism_mekasuit_pants': {
        1: [actor(at=[0.5, 1, 1.0], yaw=-90, equip=MEKASUIT, wait=5), actor(pose='SPRINT', path=[[5.5, 1, 1.0, 12, -90]], wait=2),
            actor(pose='STAND', wait=10)],
    },
    'mekanism_scuba_mask': {
        0: [blocks(POOL), actor(at=[1.5, 1, 5.0], yaw=-90, equip=[['HEAD', 'mekanism:scuba_mask'], ['CHEST', 'mekanism:scuba_tank']],
                                pose='WALK', path=[[5.0, 1, 5.0, 25]], wait=2), actor(pose='STAND', wait=2),
            stream([[5.0, 2.4, 5.0], [5.0, 3.0, 5.0]], particle='bubble', rounds=4, speed=4)],
    },
    'mekanism_scuba_tank': {
        1: [blocks(POOL), actor(at=[1.5, 1, 5.0], yaw=-90, equip=[['HEAD', 'mekanism:scuba_mask'], ['CHEST', 'mekanism:scuba_tank']],
                                pose='WALK', path=[[5.0, 1, 5.0, 25]], wait=2), actor(pose='STAND', wait=2),
            stream([[5.0, 2.4, 5.0], [5.0, 3.0, 5.0]], particle='bubble', rounds=4, speed=4)],
    },
    'mekanism_pellet_polonium': {
        0: [item('mekanism:pellet_polonium', [3.5, 1.6, 3.5], float_=True, wait=10)] +
           [item(m, [3.5 + dx, 1.4, 3.5 + dz], float_=True, wait=4) for m, dx, dz in [
               ('mekanism:module_energy_unit', -1.5, 0), ('mekanism:module_jetpack_unit', -0.75, -1.3),
               ('mekanism:module_radiation_shielding_unit', 0.75, -1.3), ('mekanism:module_gravitational_modulating_unit', 1.5, 0),
               ('mekanism:module_vision_enhancement_unit', 0.75, 1.3), ('mekanism:module_hydraulic_propulsion_unit', -0.75, 1.3)]] +
           [wait(30)],
    },
    # ------------------------------------------------------------ generators
    'mekanismgenerators_advanced_solar_generator': {
        1: [L(CABLE, [[2, 1, 3], [1, 1, 3]]), B([0, 1, 3], SMELTER), stream([[3.0, 1.5, 3.5], [1.0, 1.5, 3.5]], color=ENERGY, rounds=2),
            active([0, 1, 3])],
    },
    'mekanismgenerators_bio_generator': {
        1: [B([6, 1, 3], CRUSHER)],
        2: [L(CABLE, [[4, 1, 3], [5, 1, 3]]), stream([[4.0, 1.5, 3.5], [6.0, 1.5, 3.5]], color=ENERGY, rounds=2)],
        3: [active([6, 1, 3], wait=2), {'op': 'sound', 'id': 'mekanism:tile.machine.crusher'},
            stream([[4.0, 1.5, 3.5], [6.0, 1.5, 3.5]], color=ENERGY, rounds=2), wait(20)],
    },
    'mekanismgenerators_heat_generator': {
        1: [B([4, 1, 3], 'minecraft:lava'), active([3, 1, 3]), indicate([3, 1, 3])],
        2: [L(CABLE, [[2, 1, 3], [1, 1, 3]]), B([0, 1, 3], SMELTER), stream([[3.0, 1.5, 3.5], [1.0, 1.5, 3.5]], color=ENERGY, rounds=2),
            active([0, 1, 3])],
    },
    'mekanismgenerators_hohlraum': {
        1: [B([5, 1, 1], 'mekanismgenerators:fusion_reactor_controller'),
            panel('mekanismgenerators:fusion_reactor_controller', ins=[['mekanismgenerators:hohlraum', 1]], at=[5, 1, 1])],
    },
    'mekanismgenerators_solar_generator': {
        1: [B([3, 2, 3], STONE), active([3, 1, 3], False)],
    },
    'mekanismgenerators_turbine_blade': {
        0: [B([3, 1, 3], 'mekanismgenerators:turbine_rotor'), B([3, 2, 3], 'mekanismgenerators:turbine_rotor'),
            click([3.5, 3.0, 3.5], item='mekanismgenerators:turbine_blade'), indicate([3, 2, 3])],
    },
    # ------------------------------------------------------------ additions
    'mekanismadditions_obsidian_tnt': {
        0: [B([3, 1, 3], 'mekanismadditions:obsidian_tnt'), B([3, 1, 2], 'minecraft:lever', props={'face': 'floor', 'facing': 'north'}),
            L(STONE, [[5, 1, 2], [1, 1, 5], [5, 1, 5], [3, 1, 6], [5, 1, 4], [2, 1, 5]]), {'op': 'lever', 'pos': [3, 1, 2], 'on': True}],
        1: [wait(10), {'op': 'explode', 'at': [3.5, 1.5, 3.5], 'r': 3.5, 'break': True}],
    },
    'mekanismadditions_white_balloon': {
        0: [B([3, 1, 3], 'minecraft:oak_fence'), click([3.5, 2.2, 3.5], item='mekanismadditions:white_balloon'),
            item('mekanismadditions:white_balloon', [3.5, 2.5, 3.5], float_=True, wait=20)],
        1: [{'op': 'mob', 'type': 'chicken', 'pos': [5.5, 1, 1.5]}, click([5.5, 1.9, 1.5], item='mekanismadditions:white_balloon'),
            carry('mekanismadditions:white_balloon', [[5.5, 2.1, 1.5], [5.5, 3.8, 1.5]], speed=20, mob=[0, -1.1, 0], keep=True)],
    },
    'mekanismadditions_white_glow_panel': {
        0: [B([3, 1, 3], 'mekanismadditions:white_glow_panel', facing='up'),
            B([6, 1, 1], STONE), B([5, 1, 1], 'mekanismadditions:white_glow_panel', facing='west'),
            B([5, 3, 5], STONE), B([5, 2, 5], 'mekanismadditions:white_glow_panel', facing='down'),
            indicate([3, 1, 3], wait=4), indicate([5, 1, 1], wait=4), indicate([5, 2, 5])],
    },
    'mekanismadditions_white_plastic': {
        1: [blocks([[[x, 1, z], f'mekanismadditions:{c}_plastic'] for (x, z), c in zip(
            [(x, z) for z in (4, 5, 6) for x in range(6)],
            ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue',
             'brown', 'green', 'red', 'black', 'dark_red', 'aqua'])], step=2)],
    },
    'mekanismadditions_white_plastic_road': {
        0: [L('mekanismadditions:white_plastic_road', [[x, 1, 1] for x in range(1, 6)]),
            actor(at=[1.5, 1, 2.8], yaw=-90, pose='WALK', path=[[5.5, 1, 2.8, 40]], wait=2),
            actor(at=[1.5, 2, 1.5], jump=True, yaw=-90, pose='SPRINT', path=[[5.5, 2, 1.5, 14]], wait=2), actor(pose='STAND', wait=10)],
    },
    'mekanismadditions_white_plastic_transparent': {
        0: [B([3, 1, 4], CHEST), B([4, 1, 4], 'mekanism:energized_smelter'), B([2, 1, 4], STONE),
            blocks([[[x, y, 2], 'mekanismadditions:light_blue_plastic_transparent'] for y in (1, 2) for x in (2, 3, 4)], step=2)],
    },
    'mekanismadditions_white_reinforced_plastic': {
        0: [blocks([[[x, y, 3], 'mekanismadditions:white_reinforced_plastic'] for y in (1, 2) for x in (2, 3, 4)], step=2),
            B([3, 1, 1], 'minecraft:tnt'), B([5, 1, 1], STONE), wait(15),
            {'op': 'explode', 'at': [3.5, 1.5, 1.5], 'r': 2.0, 'break': False}, destroy([[3, 1, 1], [5, 1, 1]])],
    },
    'mekanismadditions_white_slick_plastic': {
        0: [L('mekanismadditions:white_slick_plastic', [[x, 1, 1] for x in range(1, 6)]),
            carry('minecraft:iron_ingot', [[1.5, 2.6, 1.5], [1.5, 2.05, 1.5], [5.5, 2.05, 1.5]], speed=4, keep=True)],
    },
    # ------------------------------------------------------------ tools and armor
    'mekanismtools_armor': {
        1: [{'op': 'show', 'ids': ['mekanismtools:osmium_helmet', 'mekanismtools:osmium_leggings', 'mekanismtools:osmium_boots']}],
    },
    'mekanism_antiprotonic_nucleosynthesizer': {
        1: [{'op': 'lightning'}, panel('mekanism:antiprotonic_nucleosynthesizer', ins=[['minecraft:coal', 1]], outs=[['minecraft:diamond', 1]])],
    },
}


# ================================================================ 1.0.9: on-screen cards, light, spinning parts
WIND = 'mekanismgenerators:wind_generator'
SOLAR = 'mekanismgenerators:solar_generator'
ROTOR = 'mekanismgenerators:turbine_rotor'
BLADE = 'mekanismgenerators:turbine_blade'
SORTER = 'mekanism:logistical_sorter'
RHEATER = 'mekanism:resistive_heater'
ENRICH_FACTORY = 'mekanism:basic_enriching_factory'
INFUSER_M = 'mekanism:metallurgic_infuser'
NOTE = 0x2A5DB0
DARKTXT = 0x404040
RED = 0xE03030
BLUE = 0x3060E0
DIAMOND_C = 0x6CEDD8
CARBON_C = 0x404040
REDSTONE_C = 0xB30505
OSMIUM_C = 0x52BDCA
ANTIMATTER_C = 0xA464B3
FUSION_C = 0x7E007D
CLEAR = {'op': 'clear'}
SCREEN_GREEN = 0x3CFE9A

SCENE_PROPS = {
    # two wind generators (one on a pillar) are tall: see more of the scene
    'mekanismgenerators_wind_generator': {'zoom': 0.62, 'offsetY': -2.0},
}


def card(cid, title, icon=None, at=None, slots=None, big=None, bars=None, rows=None, keep=False, wait=None):
    """An information card in the left column. slots: [[id, n] or None], bars: [[rgb, value, label]], rows: see SceneRunner."""
    d = {'op': 'card', 'id': cid, 'title': title}
    if icon: d['icon'] = icon
    if at is not None: d['at'] = at
    if slots is not None: d['slots'] = slots
    if big: d['big'] = big
    if bars: d['bars'] = bars
    if rows: d['rows'] = rows
    if keep: d['keep'] = True
    if wait is not None: d['wait'] = wait
    return d


def cslot(cid, slot, iid=None, n=1, wait=None):
    d = {'op': 'cardslot', 'id': cid, 'slot': slot, 'item': [iid, n] if iid else None}
    if wait is not None: d['wait'] = wait
    return d


def cbar(cid, bar, value, ticks=30, wait=None):
    d = {'op': 'cardbar', 'id': cid, 'bar': bar, 'value': value, 'ticks': ticks}
    if wait is not None: d['wait'] = wait
    return d


def cprog(cid, value, ticks=30, wait=None):
    d = {'op': 'cardprogress', 'id': cid, 'value': value, 'ticks': ticks}
    if wait is not None: d['wait'] = wait
    return d


def ctext(cid, line, text, wait=None):
    d = {'op': 'cardtext', 'id': cid, 'line': line, 'text': text}
    if wait is not None: d['wait'] = wait
    return d


def label(at, text, color=0xFFFFFF, lid=None, keep=False, wait=None):
    d = {'op': 'label', 'at': at, 'text': text, 'color': color}
    if lid: d['id'] = lid
    if keep: d['keep'] = True
    if wait is not None: d['wait'] = wait
    return d


def T(text, icon=None, color=DARKTXT):
    return ['text', text, icon, color]


def spin(sid, pos, speed, fields=('angle',)):
    return {'op': 'spin', 'id': sid, 'pos': pos, 'speed': speed, 'fields': list(fields)}


def paint(cells, color, step=None):
    d = {'op': 'paint', 'cells': cells, 'color': color}
    if step: d['step'] = step
    return d


def tag(cells, nbt, wait=None):
    d = {'op': 'tag', 'cells': cells, 'nbt': nbt}
    if wait is not None: d['wait'] = wait
    return d


def heat(cells, temps, step=8):
    """Thermodynamic conductors turning red one after another (the heat they hold, in their update tag)."""
    return [tag([c], '{temperature:%.1fd}' % t, wait=step) for c, t in zip(cells, temps)]


def dark(sky=0, wait=None):
    d = {'op': 'dark', 'sky': sky}
    if wait is not None: d['wait'] = wait
    return d


def light(pos, level=15, wait=None):
    d = {'op': 'light', 'pos': pos, 'level': level}
    if wait is not None: d['wait'] = wait
    return d


def gpanel(title, ins=(), outs=(), gauge=None, **k):
    """panel() with gauges given as [label, rgb, 'in'|'out'] and extra keys (energy, gen, note, at, src...)."""
    d = panel(title, ins, outs, gauge)
    d.update(k)
    return d


def player_use(item, slot='MAINHAND'):
    return {'op': 'player', 'pose': 'USE_ITEM', 'equip': [[slot, item]], 'move': False, 'rise': False, 'ui': False}


def solar_card(cid, title_item, at):
    return [gpanel(title_item, at=at, gen=True, genTo=0.6, active=False,
                   screenText=[[44, 27, '@moremekaponder.ui.solar_sky', SCREEN_GREEN, 88]])]


UI_STEPS = {
    # ------------------------------------------------------------ generators: power leaves from the right side
    'mekanismgenerators_wind_generator': {
        0: [B([2, 1, 4], WIND), spin('low', [2, 1, 4], 3.0), wait(20),
            L(CABLE, [[2, 1, 3], [2, 1, 2]]), B([2, 1, 1], SMELTER),
            gpanel(WIND, at=[2, 1, 4], gen=True, genTo=0.35, active=False, top=2.6,
                   screenText=[[52, 25, '@moremekaponder.ui.wind_note', SCREEN_GREEN, 74]]),
            stream([[2.5, 1.5, 4.0], [2.5, 1.5, 2.0]], color=ENERGY, rounds=2), active([2, 1, 1])],
        1: [blocks([[[5, 1, 1], STONE], [[5, 2, 1], STONE], [[5, 3, 1], STONE]], step=3),
            B([5, 4, 1], WIND), spin('high', [5, 4, 1], 9.0), wait(10),
            label([2.5, 7.7, 4.5], '@moremekaponder.ui.wind_low', 0x9AD0FF, wait=3),
            label([5.5, 10.7, 1.5], '@moremekaponder.ui.wind_high', 0xFFD24A, wait=3),
            card('cmp', '@moremekaponder.ui.wind_output', icon=WIND, bars=[[ENERGY, 0.0, None], [ENERGY, 0.0, None]],
                 rows=[['hbar', 0, '@moremekaponder.ui.wind_low_row'], ['hbar', 1, '@moremekaponder.ui.wind_high_row'],
                       T('@moremekaponder.ui.wind_height', None, NOTE)]),
            cbar('cmp', 0, 0.35, 30), cbar('cmp', 1, 0.9, 30), wait(40)],
    },
    'mekanismgenerators_solar_generator': {
        # a solar generator sends its power out of the bottom: it sits on the cable
        0: [L(CABLE, [[3, 1, 3]]), B([3, 2, 3], SOLAR), wait(10), L(CABLE, [[3, 1, 2], [3, 1, 1]]), B([3, 1, 0], SMELTER),
            label([3.5, 1.5, 2.3], '@moremekaponder.ui.bottom_out', ENERGY, wait=5),
            *solar_card('solar', SOLAR, [3, 2, 3]), stream([[3.5, 1.5, 3.5], [3.5, 1.5, 1.0]], color=ENERGY, rounds=2), active([3, 1, 0])],
        1: [B([3, 3, 3], STONE), active([3, 1, 0], False),
            gpanel(SOLAR, at=[3, 2, 3], genFrom=0.6, genTo=0.0, active=False,
                   screenText=[[44, 27, '@moremekaponder.ui.solar_blocked', 0xFF5050, 88]])],
    },
    'mekanismgenerators_advanced_solar_generator': {
        1: [L(CABLE, [[3, 1, 2], [3, 1, 1]]), B([3, 1, 0], SMELTER), *solar_card('solar', 'mekanismgenerators:advanced_solar_generator', [3, 1, 3]),
            stream([[3.5, 1.5, 3.0], [3.5, 1.5, 1.0]], color=ENERGY, rounds=2), active([3, 1, 0])],
    },
    'mekanismgenerators_heat_generator': {
        1: [B([4, 1, 3], 'minecraft:lava'), active([3, 1, 3]), label([4.5, 1.9, 3.5], '@moremekaponder.ui.lava_heat', HEAT, wait=5),
            indicate([3, 1, 3])],
        2: [L(CABLE, [[3, 1, 2], [3, 1, 1]]), B([3, 1, 0], SMELTER), stream([[3.5, 1.5, 3.0], [3.5, 1.5, 1.0]], color=ENERGY, rounds=2),
            active([3, 1, 0])],
    },
    'mekanismgenerators_bio_generator': {
        1: [B([3, 1, 0], CRUSHER)],
        2: [L(CABLE, [[3, 1, 2], [3, 1, 1]]), stream([[3.5, 1.5, 3.0], [3.5, 1.5, 1.0]], color=ENERGY, rounds=2)],
        3: [active([3, 1, 0], wait=2), {'op': 'sound', 'id': 'mekanism:tile.machine.crusher'},
            stream([[3.5, 1.5, 3.0], [3.5, 1.5, 1.0]], color=ENERGY, rounds=2), wait(20)],
    },
    'mekanism_basic_universal_cable': {
        # the heat generator's power comes out of its front
        0: [B([1, 1, 3], HEATGEN, facing='east', alt=CUBE), L(CABLE, [[2, 1, 3], [3, 1, 3], [4, 1, 3]]), B([5, 1, 3], SMELTER), active([1, 1, 3]),
            stream([[2.0, 1.5, 3.5], [5.0, 1.5, 3.5]], color=ENERGY, rounds=2), active([5, 1, 3])],
    },
    # ------------------------------------------------------------ heat: conductors glow as the heat moves along
    'mekanism_basic_thermodynamic_conductor': {
        0: [B([1, 1, 3], RHEATER), B([1, 1, 0], CUBE), L(CABLE, [[1, 1, 1], [1, 1, 2]]),
            L(COND, [[2, 1, 3], [3, 1, 3], [4, 1, 3]]), B([5, 1, 3], HEATGEN, alt=COND), active([1, 1, 3]),
            *heat([[2, 1, 3], [3, 1, 3], [4, 1, 3]], [1800, 1300, 900]),
            stream([[2.0, 1.5, 3.5], [5.0, 1.5, 3.5]], color=HEAT, rounds=2, speed=6), active([5, 1, 3]),
            label([5.5, 2.1, 3.5], '@moremekaponder.ui.heat_to_power', HEAT, wait=10)],
    },
    'mekanism_resistive_heater': {
        0: [B([3, 1, 3], RHEATER), *power_west(),
            gpanel(RHEATER, energy=True, screenText=[[52, 27, '@moremekaponder.ui.power_to_heat', SCREEN_GREEN, 72]]), active([3, 1, 3])],
        1: [L(COND, [[4, 1, 3], [5, 1, 3]]), B([6, 1, 3], HEATGEN, alt=COND), *heat([[4, 1, 3], [5, 1, 3]], [1600, 1000]),
            stream([[4.0, 1.5, 3.5], [6.0, 1.5, 3.5]], color=HEAT, rounds=3, speed=5), active([6, 1, 3])],
    },
    'mekanism_fuelwood_heater': {
        1: [L(COND, [[4, 1, 3], [5, 1, 3]]), B([6, 1, 3], HEATGEN, alt=COND), active([3, 1, 3]), *heat([[4, 1, 3], [5, 1, 3]], [1600, 1000]),
            stream([[4.0, 1.5, 3.5], [6.0, 1.5, 3.5]], color=HEAT, rounds=3, speed=5), active([6, 1, 3])],
    },
    # ------------------------------------------------------------ light: make it dark, then the glowing blocks light it up
    'mekanismadditions_white_plastic_glow': {
        0: [panel('minecraft:crafting_table', ins=[['mekanismadditions:white_plastic', 1], ['minecraft:glowstone_dust', 1]],
                  outs=[['mekanismadditions:white_plastic_glow', 1]])],
        1: [remove([[5, 1, 1]]), blocks([[[1, 1, 1], 'mekanismadditions:white_plastic'], [[1, 2, 1], 'mekanismadditions:white_plastic'],
                    [[5, 1, 5], 'mekanismadditions:white_plastic'], [[1, 1, 5], 'mekanismadditions:white_plastic'],
                    [[5, 1, 1], 'mekanismadditions:white_plastic']], step=2),
            dark(0, wait=25),
            card('dark', '@moremekaponder.ui.dark', icon='minecraft:black_concrete', rows=[T('@moremekaponder.ui.light_none', None, NOTE)]),
            wait(10), B([3, 1, 3], 'mekanismadditions:white_plastic_glow'), light([3, 1, 3], 15, wait=10),
            label([3.5, 2.1, 3.5], '@moremekaponder.ui.glow_plastic', 0xFFF0A0, wait=4),
            label([1.5, 3.1, 1.5], '@moremekaponder.ui.normal_plastic', 0xB0B0B0, wait=4),
            card('lit', '#item:mekanismadditions:white_plastic_glow', icon='mekanismadditions:white_plastic_glow',
                 bars=[[0xFFE070, 1.0, None]], rows=[['hbar', 0, '@moremekaponder.ui.light_level'], T('@moremekaponder.ui.light_note', None, NOTE)]),
            wait(50)],
    },
    'mekanismadditions_white_glow_panel': {
        0: [B([6, 1, 1], STONE), B([5, 3, 5], STONE), dark(0, wait=20),
            B([3, 1, 3], 'mekanismadditions:white_glow_panel', facing='up'), light([3, 1, 3], 15, wait=15),
            B([5, 1, 1], 'mekanismadditions:white_glow_panel', facing='west'), light([5, 1, 1], 15, wait=15),
            B([5, 2, 5], 'mekanismadditions:white_glow_panel', facing='down'), light([5, 2, 5], 15, wait=10),
            label([3.5, 1.5, 3.5], '@moremekaponder.ui.floor', 0xFFF0A0, wait=3),
            label([5.3, 1.9, 1.5], '@moremekaponder.ui.wall', 0xFFF0A0, wait=3),
            label([5.5, 3.3, 5.5], '@moremekaponder.ui.ceiling', 0xFFF0A0, wait=3),
            card('lit', '#item:mekanismadditions:white_glow_panel', icon='mekanismadditions:white_glow_panel',
                 bars=[[0xFFE070, 1.0, None]], rows=[['hbar', 0, '@moremekaponder.ui.light_level'], T('@moremekaponder.ui.glow_panel_note', None, NOTE)]),
            wait(40)],
    },
    'mekanismadditions_white_plastic': {
        0: [panel('minecraft:crafting_table', ins=[['mekanism:hdpe_sheet', 3], ['minecraft:white_dye', 1]],
                  outs=[['mekanismadditions:white_plastic', 1]])],
    },
    # ------------------------------------------------------------ turbine: the blades show up on the rotors
    'mekanismgenerators_turbine_blade': {
        0: [B([3, 1, 3], ROTOR), B([3, 2, 3], ROTOR), tag([[3, 1, 3]], '{position:0}', wait=1), tag([[3, 2, 3]], '{position:1}', wait=5),
            card('bl', '@moremekaponder.ui.blades', icon=BLADE, at=[3.5, 2.5, 3.5], slots=[None, None, None, None],
                 rows=[['icons', [0, 1, 2, 3]], T(['@moremekaponder.ui.blades_count', '0'], None, DARKTXT), T('@moremekaponder.ui.blades_rule', None, NOTE)]),
            click([3.5, 1.6, 3.0], item=BLADE), tag([[3, 1, 3]], '{blades:1,position:0}'), cslot('bl', 0, BLADE), ctext('bl', 0, ['@moremekaponder.ui.blades_count', '1'], wait=12),
            click([3.5, 1.6, 3.0], item=BLADE), tag([[3, 1, 3]], '{blades:2,position:0}'), cslot('bl', 1, BLADE), ctext('bl', 0, ['@moremekaponder.ui.blades_count', '2'], wait=12),
            click([3.5, 2.6, 3.0], item=BLADE), tag([[3, 2, 3]], '{blades:1,position:1}'), cslot('bl', 2, BLADE), ctext('bl', 0, ['@moremekaponder.ui.blades_count', '3'], wait=12),
            click([3.5, 2.6, 3.0], item=BLADE), tag([[3, 2, 3]], '{blades:2,position:1}'), cslot('bl', 3, BLADE), ctext('bl', 0, ['@moremekaponder.ui.blades_count', '4'], wait=12),
            indicate([3, 2, 3])],
    },
    # ------------------------------------------------------------ logistical sorter: filters, and painted transporters
    'mekanism_logistical_sorter': {
        0: [B([1, 1, 3], CHEST), B([2, 1, 3], SORTER, facing='east'),
            L(TRANS, [[3, 1, 3], [4, 1, 3], [5, 1, 3]]), B([6, 1, 3], CHEST),
            card('flt', '#item:' + SORTER, icon=SORTER, at=[2.5, 1.9, 3.5],
                 rows=[T('@moremekaponder.ui.filter_item'), T('#item:minecraft:iron_ore', 'minecraft:iron_ore', NOTE)])],
        1: [card('chest', '@moremekaponder.ui.chest_items', icon=CHEST, at=[1.5, 1.9, 3.5],
                 slots=[['minecraft:cobblestone', 16], ['minecraft:oak_log', 8], ['minecraft:iron_ore', 3]],
                 rows=[['icons', [0, 1, 2]], T('@moremekaponder.ui.takes_out', 'minecraft:iron_ore', NOTE)]),
            item('minecraft:cobblestone', [1.3, 2.1, 3.3], float_=True, wait=2), item('minecraft:oak_log', [1.7, 2.1, 3.7], float_=True, wait=2),
            item('minecraft:iron_ore', [1.5, 2.2, 3.5], float_=True, wait=10),
            carry('minecraft:iron_ore', [[1.9, 1.3, 3.5], [6.1, 1.3, 3.5]], count=3, speed=5, wait=5),
            cslot('chest', 2, None, wait=10)],
        2: [L(TRANS, [[4, 1, 2], [4, 1, 1]]), B([4, 1, 0], CHEST), wait(5),
            paint([[4, 1, 2], [4, 1, 1]], 12, step=6), paint([[5, 1, 3]], 1),
            label([4.5, 1.8, 1.5], '@moremekaponder.ui.red_transporter', RED, wait=3),
            label([5.5, 1.8, 3.5], '@moremekaponder.ui.blue_transporter', BLUE, wait=3),
            card('flt', '#item:' + SORTER, icon=SORTER, at=[2.5, 1.9, 3.5],
                 rows=[T('@moremekaponder.ui.filter_item'), T('@moremekaponder.ui.iron_to_red', 'minecraft:iron_ore', RED),
                       T('@moremekaponder.ui.cobble_to_blue', 'minecraft:cobblestone', BLUE), T('@moremekaponder.ui.paint_rule', None, NOTE)]),
            carry('minecraft:iron_ore', [[2.5, 1.3, 3.5], [4.5, 1.3, 3.5], [4.5, 1.3, 0.9]], count=3, speed=5, trail='red'),
            carry('minecraft:cobblestone', [[2.5, 1.3, 3.5], [6.1, 1.3, 3.5]], count=2, speed=5, trail='blue')],
    },
    # ------------------------------------------------------------ one recipe per panel (texts that show two)
    'mekanism_crusher': {
        1: [gpanel(CRUSHER, ins=[['minecraft:cobblestone', 1]], outs=[['minecraft:gravel', 1]], energy=True), CLEAR,
            gpanel(CRUSHER, ins=[['minecraft:gravel', 1]], outs=[['minecraft:sand', 1]], energy=True)],
    },
    'mekanism_enriched_carbon': {
        2: [gpanel(INFUSER_M, ins=[['minecraft:iron_ingot', 1]], outs=[['mekanism:enriched_iron', 1]],
                   gauge=[['@chemical.mekanism.carbon', CARBON_C, 'in']], energy=True), CLEAR,
            gpanel(INFUSER_M, ins=[['mekanism:enriched_iron', 1]], outs=[['mekanism:dust_steel', 1]],
                   gauge=[['@chemical.mekanism.carbon', CARBON_C, 'in']], energy=True)],
    },
    'mekanism_enriched_redstone': {
        2: [gpanel(INFUSER_M, ins=[['minecraft:copper_ingot', 1]], outs=[['mekanism:alloy_infused', 1]],
                   gauge=[['@chemical.mekanism.redstone', REDSTONE_C, 'in']], energy=True), CLEAR,
            gpanel(INFUSER_M, ins=[['mekanism:ingot_osmium', 1]], outs=[['mekanism:basic_control_circuit', 1]],
                   gauge=[['@chemical.mekanism.redstone', REDSTONE_C, 'in']], energy=True)],
    },
    'mekanism_purification_chamber': {
        2: [B([5, 1, 1], CRUSHER), B([6, 1, 1], CUBE), B([1, 1, 1], CHAMBER), B([0, 1, 1], CUBE),
            gpanel(CRUSHER, ins=[['mekanism:clump_iron', 1]], outs=[['mekanism:dirty_dust_iron', 1]], at=[5, 1, 1], energy=True), CLEAR,
            gpanel(CHAMBER, ins=[['mekanism:dirty_dust_iron', 1]], outs=[['mekanism:dust_iron', 1]], at=[1, 1, 1], energy=True)],
    },
    'mekanism_ingot_refined_obsidian': {
        1: [{'op': 'swap', 'id': INFUSER_M},
            gpanel(INFUSER_M, ins=[['mekanism:dust_obsidian', 1]], outs=[['mekanism:dust_refined_obsidian', 1]],
                   gauge=[['@chemical.mekanism.diamond', DIAMOND_C, 'in']], energy=True)],
    },
    'mekanism_ingot_refined_glowstone': {
        0: [B([3, 1, 3], 'mekanism:osmium_compressor'), *power_west(),
            gpanel('mekanism:osmium_compressor', ins=[['mekanism:ingot_osmium', 1]], gauge=[['@chemical.mekanism.osmium', OSMIUM_C, 'out']],
                   energy=True)],
    },
    'mekanism_antiprotonic_nucleosynthesizer': {
        1: [{'op': 'lightning'}, gpanel('mekanism:antiprotonic_nucleosynthesizer', ins=[['minecraft:coal', 1]], outs=[['minecraft:diamond', 1]],
                                       gauge=[['@chemical.mekanism.antimatter', ANTIMATTER_C, 'in']], energy=True)],
    },
    'mekanismgenerators_hohlraum': {
        0: [B([3, 1, 3], CTANK), gpanel(CTANK, ins=[['mekanismgenerators:hohlraum', 1]], outs=[['mekanismgenerators:hohlraum', 1]],
                                        gauge=[['@chemical.mekanismgenerators.fusion_fuel', FUSION_C, 'out']])],
    },
    'mekanism_rotary_condensentrator': {
        0: [B([3, 1, 3], 'mekanism:rotary_condensentrator'), *power_west(), B([6, 1, 3], CTANK), L(TUBE, [[5, 1, 3], [4, 1, 3]]),
            gpanel('mekanism:rotary_condensentrator', gauge=[['@chemical.mekanism.hydrogen', H2, 'in'], ['@block.mekanism.hydrogen', 0xB0D8FF, 'out']],
                   energy=True, src=[6.5, 1.5, 3.5])],
        1: [gpanel('mekanism:rotary_condensentrator', gauge=[['@block.mekanism.hydrogen', 0xB0D8FF, 'in'], ['@chemical.mekanism.hydrogen', H2, 'out']],
                   energy=True, note='@moremekaponder.ui.mode_decondense')],
    },
    'mekanism_basic_tier_installer': {
        1: [card('fac', '#item:' + ENRICH_FACTORY, icon=ENRICH_FACTORY, at=[3.5, 2.1, 3.5],
                 slots=[None, None, None, None, None, None], big=[3, 4, 5], bars=[[ENERGY, 1.0, '@moremekaponder.ui.energy']],
                 rows=[['process', [0], [0, 1, 2], True, [3, 4, 5], []], ['legend'], T('@moremekaponder.ui.three_at_once', None, NOTE)]),
            item('minecraft:iron_ore', [3.5, 2.7, 3.5], motion=[0, -0.05, 0], wait=10),
            cslot('fac', 0, 'minecraft:iron_ore'), cslot('fac', 1, 'minecraft:iron_ore'), cslot('fac', 2, 'minecraft:iron_ore', wait=6),
            active([3, 1, 3]), cprog('fac', 1.0, 40), cbar('fac', 0, 0.7, 40, wait=40), active([3, 1, 3], False),
            cprog('fac', 0.0, 0), cslot('fac', 0), cslot('fac', 1), cslot('fac', 2),
            cslot('fac', 3, 'mekanism:dust_iron', 2), cslot('fac', 4, 'mekanism:dust_iron', 2), cslot('fac', 5, 'mekanism:dust_iron', 2, wait=30)],
    },
    'mekanism_energy_tablet': {
        1: [{'op': 'swap', 'id': CHAMBER},
            gpanel(CHAMBER, ins=[['mekanism:energy_tablet', 1]], energySlot=True)],
    },
    'mekanism_formulaic_assemblicator': {
        0: [B([3, 1, 3], 'mekanism:formulaic_assemblicator'), *power_west(),
            card('fa', '#item:mekanism:formulaic_assemblicator', icon='mekanism:formulaic_assemblicator', at=[3.5, 2.1, 3.5],
                 slots=[None, None, None, None], big=[3],
                 rows=[['grid', [-1, -1, -1, 0, 1, 2, -1, -1, -1], 3], T('@moremekaponder.ui.formula', 'mekanism:crafting_formula', NOTE)]),
            cslot('fa', 0, 'minecraft:wheat', wait=4), cslot('fa', 1, 'minecraft:wheat', wait=4), cslot('fa', 2, 'minecraft:wheat', wait=4),
            cslot('fa', 3, 'minecraft:bread', wait=8), click([3.5, 2.0, 3.0]), cprog('fa', 1.0, 20, wait=24),
            ctext('fa', 0, '@moremekaponder.ui.formula_saved', wait=30)],
        2: [card('fa', '#item:mekanism:formulaic_assemblicator', icon='mekanism:formulaic_assemblicator', at=[3.5, 2.1, 3.5],
                 slots=[None, None, None, ['minecraft:bread', 1]], big=[3],
                 rows=[['grid', [-1, -1, -1, 0, 1, 2, -1, -1, -1], 3]]),
            item('minecraft:wheat', [3.5, 2.7, 3.5], motion=[0, -0.05, 0], wait=12),
            cslot('fa', 0, 'minecraft:wheat', wait=3), cslot('fa', 1, 'minecraft:wheat', wait=3), cslot('fa', 2, 'minecraft:wheat', wait=30)],
    },
    'mekanism_precision_sawmill': {
        1: [gpanel('mekanism:precision_sawmill', ins=[['minecraft:oak_log', 1]], outs=[['minecraft:oak_planks', 6], ['mekanism:sawdust', 1]],
                   energy=True, note='@moremekaponder.ui.sawdust_chance')],
    },
    'mekanism_oredictionificator': {
        0: [B([3, 1, 3], 'mekanism:oredictionificator'),
            card('od', '#item:mekanism:oredictionificator', icon='mekanism:oredictionificator', at=[3.5, 2.1, 3.5],
                 slots=[None, None], big=[1],
                 rows=[T('@moremekaponder.ui.filter'), T('#c:ingots/copper', 'minecraft:copper_ingot', NOTE), ['process', [], [0], True, [1], []],
                       T('@moremekaponder.ui.oredict_filter', None, NOTE)]),
            item('minecraft:copper_ingot', [3.5, 2.7, 3.5], motion=[0, -0.05, 0], wait=12), cslot('od', 0, 'minecraft:copper_ingot', wait=5),
            cprog('od', 1.0, 30, wait=30), cprog('od', 0.0, 0), cslot('od', 0), cslot('od', 1, 'minecraft:copper_ingot'),
            item('minecraft:copper_ingot', [3.5, 1.3, 2.85], motion=[0, 0.18, -0.12], wait=25)],
    },
    # ------------------------------------------------------------ gear and mobs: gauges for the thing being charged or used
    'mekanism_chargepad': {
        4: [card('robit', '#item:mekanism:robit', icon='mekanism:robit', at=[3.5, 1.8, 3.5], bars=[[ENERGY, 0.2, None]],
                 rows=[['hbar', 0, '@moremekaponder.ui.robit_energy']]),
            active([3, 1, 3]), cbar('robit', 0, 1.0, 50, wait=55)],
    },
    'mekanism_canteen': {
        1: [player_use('mekanism:canteen'),
            card('food', '#item:mekanism:canteen', icon='mekanism:canteen', bars=[[0xC88A3A, 0.3, None]],
                 rows=[['hbar', 0, '@moremekaponder.ui.hunger']]),
            cbar('food', 0, 1.0, 40, wait=45)],
    },
    'mekanism_mekasuit_helmet': {
        0: [actor(at=[1.5, 1, 1.5], yaw=135, equip=MEKASUIT, wait=10),
            card('suit', '@moremekaponder.ui.mekasuit', icon='mekanism:mekasuit_helmet', bars=[[0xE03030, 1.0, None], [ENERGY, 1.0, None]],
                 rows=[['hbar', 0, '@moremekaponder.ui.health'], ['hbar', 1, '@moremekaponder.ui.energy']]),
            {'op': 'flash', 'color': 0x40FF0000}, cbar('suit', 1, 0.75, 15),
            stream([[1.5, 2.0, 1.5], [1.5, 3.2, 1.5]], color=ENERGY, rounds=3), wait(10),
            {'op': 'flash', 'color': 0x40FF0000}, cbar('suit', 1, 0.5, 15), wait(30)],
    },
    'mekanism_seismic_reader': {
        1: [player_use('mekanism:seismic_reader'),
            card('seis', '#item:mekanism:seismic_reader', icon='mekanism:seismic_reader',
                 rows=[T('@moremekaponder.ui.seismic_scan'), T('#item:minecraft:grass_block', 'minecraft:grass_block'),
                       T('#item:minecraft:stone', 'minecraft:stone'), T('#item:minecraft:iron_ore', 'minecraft:iron_ore', NOTE),
                       T('#item:minecraft:deepslate', 'minecraft:deepslate'), T('#item:minecraft:diamond_ore', 'minecraft:diamond_ore', NOTE),
                       T('#item:minecraft:bedrock', 'minecraft:bedrock')]),
            wait(40)],
    },
    # ------------------------------------------------------------ tools and settings: what the screen or chat would say
    'mekanism_dictionary': {
        0: [B([3, 1, 3], 'minecraft:iron_ore'), click([3.5, 2.0, 3.5], item='mekanism:dictionary'), indicate([3, 1, 3]),
            {'op': 'tagcard', 'item': 'minecraft:iron_ore', 'title': '@moremekaponder.ui.tags', 'icon': 'mekanism:dictionary',
             'at': [3.5, 2.0, 3.5], 'wait': 40}],
    },
    'mekanism_configuration_card': {
        0: [B([3, 1, 3], CHAMBER), *power_west(), B([5, 1, 3], CHAMBER),
            click([3.5, 2.0, 3.5], sneak=True, item='mekanism:configuration_card'), indicate([3, 1, 3]),
            card('cc', '#item:mekanism:configuration_card', icon='mekanism:configuration_card', at=[3.5, 2.0, 3.5],
                 rows=[T('@moremekaponder.ui.copied', None, NOTE), T('#item:' + CHAMBER, CHAMBER)], wait=30)],
        1: [click([5.5, 2.0, 3.5], item='mekanism:configuration_card'), indicate([5, 1, 3]),
            card('cc', '#item:mekanism:configuration_card', icon='mekanism:configuration_card', at=[5.5, 2.0, 3.5],
                 rows=[T('@moremekaponder.ui.pasted', None, NOTE), T('#item:' + CHAMBER, CHAMBER)], wait=30)],
    },
    'mekanism_configurator': {
        2: [click([3.5, 2.0, 3.0], item=CONFIG), indicate([3, 1, 3]),
            card('sides', '@moremekaponder.ui.sides', icon=CONFIG, at=[3.5, 1.5, 3.0],
                 rows=[T('@moremekaponder.ui.side_before', None, 0x2A5DB0), T('@moremekaponder.ui.side_after', None, 0xC86400)], wait=30)],
    },
    'mekanism_electric_bow': {
        1: [card('bow', '#item:mekanism:electric_bow', icon='mekanism:electric_bow', rows=[T('@moremekaponder.ui.bow_fire', None, HEAT)]),
            actor(pose='STAND', wait=5), actor(pose='USE_ITEM', wait=15),
            carry('minecraft:arrow', [[2.0, 2.4, 4.0], [5.1, 1.6, 1.9]], speed=2, trail='flame'), indicate([5, 1, 1])],
    },
    'mekanism_diversion_transporter': {
        0: OVERRIDES['mekanism_diversion_transporter'][0][:-1] + [
            label([3.5, 1.7, 3.5], '@moremekaponder.ui.diversion_off', 0x60E060, wait=5),
            OVERRIDES['mekanism_diversion_transporter'][0][-1]],
        1: [{'op': 'lever', 'pos': [3, 1, 2], 'on': True},
            label([3.5, 1.7, 3.5], '@moremekaponder.ui.diversion_on', RED, wait=5),
            carry('minecraft:iron_ingot', [[0.9, 1.3, 3.5], [2.7, 1.3, 3.5], [0.9, 1.3, 3.5]], count=2, speed=5)],
    },
    'mekanism_basic_bin': {
        0: [B([3, 1, 3], 'mekanism:basic_bin'),
            card('bin', '#item:mekanism:basic_bin', icon='mekanism:basic_bin', at=[3.5, 2.0, 3.0], slots=[None],
                 rows=[['icons', [0]], T(['@moremekaponder.ui.bin_count', '0'])]),
            click([3.5, 2.0, 3.0], item='minecraft:cobblestone'),
            carry('minecraft:cobblestone', [[3.5, 1.9, 2.3], [3.5, 1.4, 2.95]], speed=8), indicate([3, 1, 3]),
            cslot('bin', 0, 'minecraft:cobblestone', 64), ctext('bin', 0, ['@moremekaponder.ui.bin_count', '64'], wait=30)],
        1: [card('bin', '#item:mekanism:basic_bin', icon='mekanism:basic_bin', at=[3.5, 2.0, 3.0], slots=[['minecraft:cobblestone', 64]],
                 rows=[['icons', [0]], T(['@moremekaponder.ui.bin_count', '64'])]),
            click([3.5, 2.0, 3.0], left=True), item('minecraft:cobblestone', [3.5, 1.3, 2.8], motion=[0, 0.15, -0.1], wait=5),
            cslot('bin', 0), ctext('bin', 0, ['@moremekaponder.ui.bin_count', '0'], wait=25)],
    },
    'mekanism_quantum_entangloporter': {
        0: OVERRIDES['mekanism_quantum_entangloporter'][0] + [
            card('qe', '@moremekaponder.ui.frequency', icon='mekanism:quantum_entangloporter',
                 rows=[T('@moremekaponder.ui.freq_name', 'mekanism:quantum_entangloporter', NOTE), T('@moremekaponder.ui.freq_same')], wait=30)],
    },
    'mekanism_portable_teleporter': {
        0: OVERRIDES['mekanism_portable_teleporter'][0] + [
            card('tp', '@moremekaponder.ui.frequency', icon='mekanism:teleporter', at=[5.5, 2.0, 4.0],
                 rows=[T('@moremekaponder.ui.freq_name', 'mekanism:teleporter', NOTE), T('@moremekaponder.ui.teleporter_link')], wait=30)],
    },
    'mekanism_network_reader': {
        0: OVERRIDES['mekanism_network_reader'][0] + [
            card('nr', '@moremekaponder.ui.reader_info', icon='mekanism:network_reader', at=[1.5, 1.6, 3.5],
                 rows=[T('#item:' + CABLE, CABLE), T('@moremekaponder.ui.reader_line1', None, NOTE), T('@moremekaponder.ui.reader_line2', None, NOTE)],
                 wait=40)],
    },
    'mekanism_digital_miner': {
        0: OVERRIDES['mekanism_digital_miner'][0] + [
            card('dm', '@moremekaponder.ui.miner_filter', icon=MINER, at=[3.5, 3.1, 3.5],
                 slots=[[o, 1] for o in ['minecraft:iron_ore', 'minecraft:gold_ore', 'minecraft:diamond_ore', 'minecraft:coal_ore', 'minecraft:redstone_ore']],
                 rows=[['icons', [0, 1, 2, 3, 4]]], wait=20)],
    },
    # ------------------------------------------------------------ crafting fixes
    'mekanismtools_tools': {
        2: [item('minecraft:stick', [3.5, 1.3, 2.5], wait=10),
            panel('minecraft:crafting_table', ins=[['mekanismtools:osmium_axe', 1], ['mekanismtools:osmium_pickaxe', 1],
                                                   ['mekanismtools:osmium_shovel', 1], ['minecraft:stick', 2]],
                  outs=[['mekanismtools:osmium_paxel', 1]])],
    },
    'mekanismgenerators_solar_panel': {
        0: [item('mekanismgenerators:solar_panel', [3.5, 1.6, 3.5], float_=True, wait=15),
            panel('minecraft:crafting_table', ins=[['mekanismgenerators:solar_panel', 3]], outs=[[SOLAR, 1]])],
    },
}

for _sid, _steps in UI_STEPS.items():
    OVERRIDES.setdefault(_sid, {}).update(_steps)


# ================================================================ 1.0.11: Mekanism Extras
X = 'mekanism_extras:'
X_COLORS = {'radiance': 0xC4C604, 'thermonuclear': 0x810C0C, 'shining': 0xFAD1FE, 'spectrum': 0xBF3314}
OSMIUM_COMPRESSOR = 'mekanism:osmium_compressor'
PRC_ID = 'mekanism:pressurized_reaction_chamber'
NUCLEO_ID = 'mekanism:antiprotonic_nucleosynthesizer'
ABS_FACTORY = X + 'absolute_enriching_factory'
ADV_PUMP = X + 'advanced_electric_pump'
CABLE_LINE = [[4, 1, 1], [5, 1, 1], [6, 1, 1]]


def xchem(name):
    return ['@chemical.mekanism_extras.' + name, X_COLORS[name]]


def alloy_scene(prev, alloy, chem, cable_from, cable_to):
    return {
        0: [B([3, 1, 3], INFUSER_M), *power_west(),
            gpanel(INFUSER_M, ins=[[prev, 1]], outs=[[alloy, 1]], gauge=[xchem(chem) + ['in']], energy=True)],
        1: [L(cable_from, CABLE_LINE), click([5.5, 1.6, 1.0], item=alloy),
            {'op': 'swapcells', 'id': cable_to, 'cells': CABLE_LINE, 'from': cable_from, 'held': alloy}],
    }


def infuse_step(item, chem):
    return [{'op': 'swap', 'id': INFUSER_M}, gpanel(INFUSER_M, ins=[[item, 1]], gauge=[xchem(chem) + ['out']], energy=True)]


def tube_east():
    return [B([6, 1, 3], CTANK), L(TUBE, [[5, 1, 3], [4, 1, 3]])]


EXTRAS_STEPS = {
    'mekanism_extras_alloy_radiance': alloy_scene('mekanism:alloy_atomic', X + 'alloy_radiance', 'radiance',
                                                  'mekanism:ultimate_universal_cable', X + 'absolute_universal_cable'),
    'mekanism_extras_alloy_thermonuclear': alloy_scene(X + 'alloy_radiance', X + 'alloy_thermonuclear', 'thermonuclear',
                                                       X + 'absolute_universal_cable', X + 'supreme_universal_cable'),
    'mekanism_extras_alloy_shining': alloy_scene(X + 'alloy_thermonuclear', X + 'alloy_shining', 'shining',
                                                 X + 'supreme_universal_cable', X + 'cosmic_universal_cable'),
    'mekanism_extras_alloy_spectrum': alloy_scene(X + 'alloy_shining', X + 'alloy_spectrum', 'spectrum',
                                                  X + 'cosmic_universal_cable', X + 'infinite_universal_cable'),
    'mekanism_extras_absolute_control_circuit': {
        0: [panel('minecraft:crafting_table', ins=[['mekanism:ultimate_control_circuit', 1], [X + 'alloy_radiance', 2]],
                  outs=[[X + 'absolute_control_circuit', 1]])],
        1: [B([3, 1, 3], INFUSER_M), *power_west(),
            gpanel(INFUSER_M, ins=[['mekanism:ultimate_control_circuit', 1]], outs=[[X + 'absolute_control_circuit', 1]],
                   gauge=[xchem('radiance') + ['in']], energy=True)],
    },
    'mekanism_extras_absolute_tier_installer': {
        0: [B([3, 1, 3], 'mekanism:ultimate_enriching_factory'), *power_west(),
            click([3.5, 2.0, 3.0], sneak=True, item=X + 'absolute_tier_installer'), {'op': 'swap', 'id': ABS_FACTORY}, indicate([3, 1, 3])],
        1: [click([3.5, 2.0, 3.0], sneak=True, item=X + 'supreme_tier_installer'), {'op': 'swap', 'id': X + 'supreme_enriching_factory'},
            indicate([3, 1, 3])],
    },
    'mekanism_extras_enriched_radiance': {
        0: [B([3, 1, 3], OSMIUM_COMPRESSOR), *power_west(), *tube_east(),
            gpanel(OSMIUM_COMPRESSOR, ins=[['minecraft:glowstone_dust', 1]], outs=[[X + 'dust_radiance', 1]],
                   gauge=[['@chemical.mekanism.uranium_oxide', 0xE1F573, 'in']], energy=True, src=[6.5, 1.5, 3.5])],
        1: [{'op': 'swap', 'id': CHAMBER}, gpanel(CHAMBER, ins=[[X + 'dust_radiance', 1]], outs=[[X + 'enriched_radiance', 1]], energy=True)],
        2: infuse_step(X + 'enriched_radiance', 'radiance'),
    },
    'mekanism_extras_enriched_thermonuclear': {
        0: [B([3, 1, 3], PRC_ID), *power_west(), *tube_east(), B([3, 1, 6], FTANK), L(PIPE, [[3, 1, 5], [3, 1, 4]]),
            gpanel(PRC_ID, ins=[[X + 'enriched_radiance', 1]], outs=[[X + 'enriched_thermonuclear', 1]],
                   gauge=[['@block.minecraft.lava', 0xFF7A1A, 'in'], ['@chemical.mekanism_extras.molten_thermonuclear', 0xD04020, 'in']],
                   energy=True)],
        1: infuse_step(X + 'enriched_thermonuclear', 'thermonuclear'),
    },
    'mekanism_extras_enriched_shining': {
        0: [B([3, 1, 3], NUCLEO_ID), *power_west(), {'op': 'lightning'},
            gpanel(NUCLEO_ID, ins=[[X + 'enriched_thermonuclear', 1]], outs=[[X + 'enriched_shining', 1]],
                   gauge=[['@chemical.mekanism.antimatter', ANTIMATTER_C, 'in']], energy=True)],
        1: infuse_step(X + 'enriched_shining', 'shining'),
    },
    'mekanism_extras_enriched_spectrum': {
        0: [B([3, 1, 3], OSMIUM_COMPRESSOR), *power_west(), *tube_east(),
            gpanel(OSMIUM_COMPRESSOR, ins=[[X + 'enriched_shining', 1]], outs=[[X + 'enriched_spectrum', 1]],
                   gauge=[['@chemical.mekanism.antimatter', ANTIMATTER_C, 'in']], energy=True, src=[6.5, 1.5, 3.5])],
        1: infuse_step(X + 'enriched_spectrum', 'spectrum'),
    },
    'mekanism_extras_naquadah_ore': {
        0: [floor('minecraft:bedrock', [[x, z] for x in range(7) for z in (5, 6)]),
            floor(X + 'naquadah_ore', [[1, 4], [3, 4], [5, 4]], step=3),
            label([3.5, 1.3, 4.5], '#item:mekanism_extras:naquadah_ore', 0x7FD36B, wait=5),
            label([3.5, 1.3, 6.0], '#item:minecraft:bedrock', 0xB0B0B0, wait=20)],
        1: [B([3, 1, 2], CHAMBER), B([0, 1, 2], CUBE), L(CABLE, [[1, 1, 2], [2, 1, 2]]),
            gpanel(CHAMBER, ins=[[X + 'naquadah_ore', 1]], outs=[[X + 'dust_naquadah', 2]], energy=True, at=[3, 1, 2])],
        2: [L(CABLE, [[2, 1, 1]]), B([3, 1, 1], SMELTER), gpanel(SMELTER, ins=[[X + 'dust_naquadah', 1]], outs=[[X + 'ingot_naquadah', 1]], energy=True, at=[3, 1, 1])],
    },
    'mekanism_extras_upgrade_stack': {
        0: [B([3, 1, 3], ABS_FACTORY), *power_west(), gpanel(ABS_FACTORY, ins=[[X + 'upgrade_stack', 1]], energy=True)],
    },
    'mekanism_extras_upgrade_creative': {
        0: [B([3, 1, 3], ABS_FACTORY), gpanel(ABS_FACTORY, ins=[[X + 'upgrade_creative', 1]]), CLEAR,
            gpanel(ABS_FACTORY, ins=[['minecraft:iron_ore', 1]], outs=[['mekanism:dust_iron', 2]], note='@moremekaponder.ui.creative_fast')],
    },
    'mekanism_extras_advanced_electric_pump': {
        0: [floor('minecraft:water', [[x, z] for x in (2, 3, 4) for z in (2, 3, 4)]), B([3, 1, 3], ADV_PUMP), *power_west(),
            active([3, 1, 3]), stream([[3.5, 0.9, 3.5], [3.5, 1.8, 3.5]], color=WATER, rounds=3),
            gpanel(ADV_PUMP, gauge=[['@block.minecraft.water', WATER, 'out']], energy=True, src=[3.5, 0.9, 3.5])],
        1: [panel('minecraft:crafting_table', ins=[['mekanism:electric_pump', 1], [X + 'alloy_thermonuclear', 2], ['minecraft:bucket', 3],
                                                   ['mekanism:block_osmium', 3]], outs=[[ADV_PUMP, 1]])],
    },
    'mekanism_extras_upgrade_ionic_membrane': {
        0: [floor('mekanism:uranium_hexafluoride', [[x, z] for x in (2, 3, 4) for z in (2, 3, 4)]), B([3, 1, 3], ADV_PUMP), *power_west(),
            gpanel(ADV_PUMP, ins=[[X + 'upgrade_ionic_membrane', 1]], gauge=[['@block.mekanism_extras.rich_uranium_fuel', 0x9ACD32, 'out']],
                   energy=True, src=[3.5, 0.9, 3.5])],
    },
}

for _sid, _steps in EXTRAS_STEPS.items():
    OVERRIDES.setdefault(_sid, {}).update(_steps)
