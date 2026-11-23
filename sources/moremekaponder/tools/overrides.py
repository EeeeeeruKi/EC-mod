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
        1: [panel(CTANK, ins=[['mekanism:jetpack', 1]], gauge=[['水素', H2]])],
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
            panel(PUMP, ins=[['mekanism:upgrade_filter', 1]], gauge=[['重水', 0x2A3FA0]], src=[3.5, 0.9, 3.5])],
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
