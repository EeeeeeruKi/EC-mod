"""Machine screen layouts for the info cards, copied from Mekanism's own GUI code (element and slot positions).

Coordinates are GUI pixels from the window's top-left corner. Slots are given by their frame (item position - 1).
Roles tie a part to what the scene shows: in0/in1 input items, out0/out1 output items, item (the item being filled or
charged), energy_item (the energy slot), gin0/gin1 gauges that are used up, gout0/gout1 gauges that are filled,
energy (the machine's power bar), gen (a generator's stored energy).
"""
import json


def slot(x, y, kind='normal', role=None):
    d = {'t': 'slot', 'x': x - 1, 'y': y - 1, 'k': kind}
    if role: d['role'] = role
    return d


def gauge(x, y, type_='standard', fill='chemical', frame='normal', role=None):
    d = {'t': 'gauge', 'x': x, 'y': y, 'type': type_, 'fill': fill, 'frame': frame}
    if role: d['role'] = role
    return d


def power(x, y, vertical=True, role='energy'):
    return {'t': 'power', 'x': x, 'y': y, 'v': vertical, 'role': role}


def progress(x, y, type_):
    return {'t': 'progress', 'x': x, 'y': y, 'type': type_}


def screen(x, y, w, h):
    return {'t': 'screen', 'x': x, 'y': y, 'w': w, 'h': h}


def image(path, x, y, w, h, tw=None, th=None):
    return {'t': 'image', 'tex': 'mekanism:gui/' + path + '.png', 'x': x, 'y': y, 'w': w, 'h': h, 'tw': tw or w, 'th': th or h}


UP = lambda x, y: image('up_arrow', x, y, 8, 10)
DOWN = lambda x, y: image('down_arrow', x, y, 8, 9)

ELECTRIC = {'w': 176, 'h': 76, 'parts': [
    slot(64, 17, 'input', 'in0'), slot(64, 53, 'power', 'energy_item'), slot(116, 35, 'output', 'out0'),
    UP(68, 38), progress(86, 38, 'bar'), power(164, 16)]}
ADVANCED = {'w': 176, 'h': 76, 'parts': [
    slot(64, 17, 'input', 'in0'), slot(64, 53, 'extra', 'in1'), slot(116, 35, 'output', 'out0'), slot(39, 35, 'power', 'energy_item'),
    gauge(68, 36, 'bar6x12', 'chemical', role='gin0'), progress(86, 38, 'bar'), power(164, 16)]}
COMBINER = {'w': 176, 'h': 76, 'parts': [
    slot(64, 17, 'input', 'in0'), slot(64, 53, 'extra', 'in1'), slot(116, 35, 'output', 'out0'), slot(39, 35, 'power', 'energy_item'),
    UP(68, 38), progress(86, 38, 'bar'), power(164, 15)]}
SAWMILL = {'w': 176, 'h': 76, 'parts': [
    slot(56, 17, 'input', 'in0'), slot(56, 53, 'power', 'energy_item'),
    {'t': 'slot', 'x': 111, 'y': 30, 'k': 'output_wide'}, slot(116, 35, 'none', 'out0'), slot(132, 35, 'none', 'out1'),
    UP(60, 38), progress(78, 38, 'bar'), power(164, 15)]}
INFUSER = {'w': 176, 'h': 76, 'parts': [
    slot(17, 35, 'extra', 'in1'), slot(51, 43, 'input', 'in0'), slot(109, 43, 'output', 'out0'), slot(143, 35, 'power', 'energy_item'),
    gauge(7, 15, 'bar4x52', 'chemical', role='gin0'), progress(72, 47, 'right'), power(164, 15)]}
OXIDIZER = {'w': 176, 'h': 84, 'parts': [
    slot(26, 36, 'input', 'in0'), progress(64, 40, 'large_right'), gauge(131, 13, frame='blue', role='gout0'),
    slot(152, 55, 'output'), slot(152, 14, 'power', 'energy_item'), power(115, 75, False)]}
DISSOLUTION = {'w': 176, 'h': 84, 'parts': [
    gauge(7, 4, frame='orange', role='gin0'), slot(8, 65, 'extra'), slot(28, 36, 'input', 'in0'), progress(64, 40, 'large_right'),
    gauge(131, 13, frame='blue', role='gout0'), slot(152, 55, 'output'), slot(152, 14, 'power', 'energy_item'), power(115, 75, False)]}
CHEM_INFUSER = {'w': 176, 'h': 84, 'parts': [
    gauge(25, 13, frame='red', role='gin0'), gauge(79, 4, frame='blue', role='gout0'), gauge(133, 13, frame='orange', role='gin1'),
    progress(47, 39, 'small_right'), progress(101, 39, 'small_left'), slot(6, 56, 'input'), slot(154, 56, 'input'),
    slot(80, 65, 'output'), slot(154, 14, 'power', 'energy_item'), power(115, 75, False)]}
WASHER = {'w': 176, 'h': 84, 'parts': [
    gauge(7, 13, fill='fluid', frame='aqua', role='gin1'), gauge(28, 13, frame='red', role='gin0'), progress(64, 39, 'large_right'),
    gauge(131, 13, frame='blue', role='gout0'), slot(152, 56, 'output'), slot(152, 14, 'power', 'energy_item'), power(115, 75, False)]}
CRYSTALLIZER = {'w': 176, 'h': 84, 'parts': [
    gauge(7, 4, frame='red', role='gin0'), slot(8, 65, 'extra'), screen(31, 13, 115, 42), progress(53, 61, 'large_right'),
    slot(129, 57, 'output', 'out0'), slot(152, 5, 'power', 'energy_item'), power(157, 23)]}
CENTRIFUGE = {'w': 176, 'h': 84, 'parts': [
    gauge(25, 13, frame='red', role='gin0'), progress(64, 39, 'large_right'), gauge(133, 13, frame='blue', role='gout0'),
    slot(5, 56, 'input'), slot(155, 56, 'output'), slot(155, 14, 'power', 'energy_item'), power(115, 75, False)]}
SNA = {'w': 176, 'h': 84, 'parts': [
    gauge(25, 13, frame='red', role='gin0'), progress(64, 39, 'large_right'), gauge(133, 13, frame='blue', role='gout0'),
    slot(5, 56, 'input'), slot(155, 56, 'output')]}
SEPARATOR = {'w': 176, 'h': 84, 'parts': [
    gauge(5, 10, fill='fluid', frame='red', role='gin0'), slot(26, 35, 'input'), gauge(58, 18, 'small', frame='blue', role='gout0'),
    gauge(100, 18, 'small', frame='orange', role='gout1'), progress(80, 30, 'bi'), slot(59, 52, 'output'), slot(101, 52, 'output'),
    slot(143, 35, 'power', 'energy_item'), power(164, 15)]}
PRC = {'w': 176, 'h': 84, 'parts': [
    gauge(5, 15, fill='fluid', frame='aqua', role='gin1'), gauge(28, 15, frame='red', role='gin0'), slot(54, 40, 'input', 'in0'),
    progress(77, 43, 'right'), slot(116, 40, 'output', 'out0'), gauge(140, 45, 'small', frame='blue', role='gout0'),
    slot(141, 22, 'power', 'energy_item'), power(163, 21)]}
ROTARY = {'w': 176, 'h': 84, 'parts': [
    gauge(25, 13, frame='red', role='gin0'), progress(64, 39, 'large_right'), gauge(133, 13, fill='fluid', frame='blue', role='gout0'),
    slot(5, 56, 'input'), slot(155, 25, 'input'), slot(155, 56, 'output'), slot(155, 5, 'power', 'energy_item'),
    DOWN(159, 44), power(115, 75, False)]}
LIQUIFIER = {'w': 176, 'h': 84, 'parts': [
    slot(26, 36, 'input', 'in0'), progress(54, 40, 'large_right'), gauge(133, 13, fill='fluid', frame='blue', role='gout0'),
    slot(155, 25, 'input', 'item'), slot(155, 56, 'output'), slot(155, 5, 'power', 'energy_item'), DOWN(159, 44), power(115, 75, False)]}
PAINTING = {'w': 176, 'h': 76, 'parts': [
    gauge(25, 13, frame='red', role='gin0'), slot(6, 56, 'extra'), slot(45, 35, 'input', 'in0'), progress(64, 39, 'large_right'),
    slot(116, 35, 'output', 'out0'), slot(144, 35, 'power', 'energy_item'), power(164, 15)]}
NUCLEO = {'w': 196, 'h': 92, 'parts': [
    screen(45, 18, 104, 68), gauge(5, 18, 'small_med', frame='red', role='gin0'), gauge(172, 18, 'small_med', 'energy', role='energy'),
    slot(26, 40, 'input', 'in0'), slot(152, 40, 'output', 'out0'), slot(6, 69, 'extra'), slot(173, 69, 'power', 'energy_item')]}
PUMP = {'w': 176, 'h': 76, 'parts': [
    gauge(6, 13, fill='fluid', frame='normal', role='gout0'), slot(28, 20, 'input'), DOWN(32, 39), slot(28, 51, 'output'),
    screen(54, 23, 80, 42), slot(143, 35, 'power', 'energy_item'), power(164, 15)]}
ENERGY_CUBE = {'w': 176, 'h': 76, 'parts': [
    slot(17, 35, 'power'), gauge(55, 18, 'wide', 'energy', role='energy'), slot(143, 35, 'power', 'item')]}
CHEM_TANK = {'w': 176, 'h': 76, 'parts': [
    slot(16, 16, 'output', 'item'), slot(16, 48, 'input'), gauge(42, 16, 'hbar116x10', 'chemical', role='gout0'), screen(42, 37, 118, 28)]}
FLUID_TANK = {'w': 176, 'h': 76, 'parts': [
    gauge(48, 18, 'wide', 'fluid', role='gout0'), slot(146, 19, 'input', 'item'), slot(146, 51, 'output')]}
HEAT_GEN = {'w': 176, 'h': 76, 'parts': [
    slot(17, 35, 'input', 'in0'), gauge(55, 18, 'wide', 'lava', role='gout0'), slot(143, 35, 'power', 'energy_item'), power(164, 15, role='gen')]}
BIO_GEN = {'w': 176, 'h': 76, 'parts': [
    gauge(7, 15, 'bar4x52', 'fluid', role='gout0'), slot(17, 35, 'input', 'in0'), screen(48, 23, 80, 40),
    slot(143, 35, 'power', 'energy_item'), power(164, 15, role='gen')]}
GAS_GEN = {'w': 176, 'h': 76, 'parts': [
    slot(17, 35, 'input'), gauge(55, 18, 'wide', role='gin0'), slot(143, 35, 'power', 'energy_item'), power(164, 15, role='gen')]}
FUELWOOD = {'w': 176, 'h': 76, 'parts': [
    slot(15, 29, 'input', 'in0'), screen(48, 23, 80, 28), image('progress/flame', 144, 31, 13, 13, 26, 13)]}
RESISTIVE = {'w': 176, 'h': 76, 'parts': [
    slot(15, 35, 'power', 'energy_item'), screen(48, 23, 80, 42), power(164, 15)]}

WIND = {'w': 176, 'h': 76, 'parts': [screen(48, 21, 80, 44), slot(143, 35, 'power', 'energy_item'), power(164, 15, role='gen')]}
SOLAR = {'w': 176, 'h': 76, 'parts': [screen(40, 23, 96, 40), slot(143, 35, 'power', 'energy_item'), power(164, 15, role='gen')]}

SCREENS = {}
for mid in ['enrichment_chamber', 'crusher', 'energized_smelter']: SCREENS['mekanism:' + mid] = ELECTRIC
for mid in ['osmium_compressor', 'purification_chamber', 'chemical_injection_chamber']: SCREENS['mekanism:' + mid] = ADVANCED
SCREENS.update({
    'mekanism:combiner': COMBINER, 'mekanism:precision_sawmill': SAWMILL, 'mekanism:metallurgic_infuser': INFUSER,
    'mekanism:chemical_oxidizer': OXIDIZER, 'mekanism:pigment_extractor': OXIDIZER,
    'mekanism:chemical_dissolution_chamber': DISSOLUTION, 'mekanism:chemical_infuser': CHEM_INFUSER, 'mekanism:pigment_mixer': CHEM_INFUSER,
    'mekanism:chemical_washer': WASHER, 'mekanism:chemical_crystallizer': CRYSTALLIZER, 'mekanism:isotopic_centrifuge': CENTRIFUGE,
    'mekanism:solar_neutron_activator': SNA, 'mekanism:electrolytic_separator': SEPARATOR, 'mekanism:pressurized_reaction_chamber': PRC,
    'mekanism:rotary_condensentrator': ROTARY, 'mekanism:nutritional_liquifier': LIQUIFIER, 'mekanism:painting_machine': PAINTING,
    'mekanism:antiprotonic_nucleosynthesizer': NUCLEO, 'mekanism:electric_pump': PUMP, 'mekanism_extras:advanced_electric_pump': PUMP, 'mekanism:fluidic_plenisher': PUMP,
    'mekanism:fuelwood_heater': FUELWOOD, 'mekanism:resistive_heater': RESISTIVE,
    'mekanismgenerators:wind_generator': WIND, 'mekanismgenerators:solar_generator': SOLAR,
    'mekanismgenerators:advanced_solar_generator': SOLAR,
    'mekanismgenerators:heat_generator': HEAT_GEN, 'mekanismgenerators:bio_generator': BIO_GEN, 'mekanismgenerators:gas_burning_generator': GAS_GEN,
})
for t in ['basic', 'advanced', 'elite', 'ultimate', 'creative']:
    SCREENS[f'mekanism:{t}_energy_cube'] = ENERGY_CUBE
    SCREENS[f'mekanism:{t}_chemical_tank'] = CHEM_TANK
    SCREENS[f'mekanism:{t}_fluid_tank'] = FLUID_TANK


def write(out_dir):
    json.dump(SCREENS, open(out_dir + '/assets/moremekaponder/screens.json', 'w'), separators=(',', ':'))
