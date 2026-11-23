"""Replays scenes.json the way SceneRunner does, without the game, to find layout problems:
overlapping blocks, things inside blocks, hidden subjects, unconnected transmitters."""
import json, math, sys, collections
import numpy as np
from PIL import Image, ImageDraw, ImageFont

SC = json.load(open(sys.argv[1] if len(sys.argv) > 1 else '/home/claude/moremekaponder/src/main/resources/assets/moremekaponder/scenes.json'))
B = json.load(open('/home/claude/mekx/bounds.json'))
CENTER = (3, 1, 3)

SMALL = ('universal_cable', 'thermodynamic_conductor')
LARGE = ('mechanical_pipe', 'pressurized_tube', 'logistical_transporter', 'diversion_transporter', 'restrictive_transporter')
def is_tx(bid): return any(k in bid for k in SMALL + LARGE)
def tx_family(bid):
    for k in ('universal_cable', 'thermodynamic_conductor', 'mechanical_pipe', 'pressurized_tube'):
        if k in bid: return k
    return 'transporter'

def shapes(bid, pos):
    """list of AABBs (x0,y0,z0,x1,y1,z1) in world units for a block at pos (facing north)."""
    x, y, z = pos
    def box(a, b): return (x + a[0], y + a[1], z + a[2], x + b[0], y + b[1], z + b[2])
    if is_tx(bid):
        r = 0.19 if any(k in bid for k in SMALL) else 0.25
        return [box((0.5 - r, 0.5 - r, 0.5 - r), (0.5 + r, 0.5 + r, 0.5 + r))]
    if bid.endswith('wind_generator'):
        return [box((0.3, 0, 0.3), (0.7, 5, 0.7)), box((-2, 3, -0.2), (3, 8, 0.0))]
    if bid.endswith('advanced_solar_generator'):
        return [box((0.35, 0, 0.35), (0.65, 2, 0.65)), box((-1, 2, -1), (2, 2.3, 2))]
    if bid.endswith('digital_miner'):
        return [box((-1, 0, -1), (2, 2, 2))]
    if bid.endswith('turbine_rotor'):
        return [box((0.4, 0, 0.4), (0.6, 1, 0.6))]
    if bid == 'minecraft:chest':
        return [box((1/16, 0, 1/16), (15/16, 14/16, 15/16))]
    if bid in ('minecraft:lever',):
        return [box((0.3, 0, 0.3), (0.7, 0.4, 0.7))]
    b = B.get(bid)
    if b:
        lo = [max(-1.5, v / 16) for v in b[0]]; hi = [min(2.5, v / 16) for v in b[1]]
        lo[1] = max(0, lo[1])
        return [box(lo, hi)]
    return [box((0, 0, 0), (1, 1, 1))]

ENT = {'item': (0.25, 0.25), 'player': (0.6, 1.8), 'pig': (0.9, 0.9), 'chicken': (0.4, 0.7)}
def ent_box(kind, p):
    w, h = ENT.get(kind, (0.5, 0.5))
    return (p[0] - w / 2, p[1], p[2] - w / 2, p[0] + w / 2, p[1] + h, p[2] + w / 2)

def inter(a, b, eps=0.02):
    return all(a[i] < b[i + 3] - eps and b[i] < a[i + 3] - eps for i in range(3))

class World:
    def __init__(s):
        s.blocks = {}      # pos -> id
        s.ents = {}        # name -> (kind, pos, label)
        s.issues = []
        s.step = 0
        s.placed = False
        s.crafter = None
        s.item_spot = 0
        s.player = None
        s.mob = None
        s.laser = None
        s.drops = []
        s.subjects = []
        s.n = 0
    def note(s, kind, msg): s.issues.append((s.step, kind, msg))
    def place(s, pos, bid, why='place'):
        pos = tuple(pos)
        if not (0 <= pos[0] <= 6 and 0 <= pos[2] <= 6 and pos[1] >= 1):
            s.note('bounds', f'{bid} at {pos} is off the plate')
        old = s.blocks.get(pos)
        if old and old != bid and why == 'place':
            s.note('overwrite', f'{bid} replaces {old} at {pos}')
        s.blocks[pos] = bid
        for p2, b2 in s.blocks.items():
            if p2 == pos: continue
            if any(inter(a, b) for a in shapes(bid, pos) for b in shapes(b2, p2)):
                s.note('overlap', f'{bid} at {pos} overlaps {b2} at {p2}')
        for name, (k, p, lab) in s.ents.items():
            if any(inter(ent_box(k, p), a) for a in shapes(bid, pos)):
                s.note('ent-in-block', f'{bid} at {pos} placed over {lab} ({name}) at {tuple(round(c,2) for c in p)}')
        if pos == CENTER: s.placed = True
        s.subjects.append(('b', pos))
    def destroy(s, pos):
        s.blocks.pop(tuple(pos), None)
    def spawn(s, name, kind, p, lab, check=True):
        s.ents[name] = (kind, tuple(p), lab)
        if check:
            for p2, b2 in s.blocks.items():
                if any(inter(ent_box(kind, p), a) for a in shapes(b2, p2)):
                    s.note('ent-in-block', f'{lab} ({kind}) at {tuple(round(c,2) for c in p)} is inside {b2} at {p2}')
        s.subjects.append(('e', name))
    def path_check(s, kind, a, b, lab):
        for t in np.linspace(0, 1, 9)[1:]:
            p = tuple(a[i] + (b[i] - a[i]) * t for i in range(3))
            for p2, b2 in s.blocks.items():
                if any(inter(ent_box(kind, p), x) for x in shapes(b2, p2)):
                    s.note('walk-through', f'{lab} walks through {b2} at {p2}')
                    return

def run(scene):
    w = World()
    frames = []
    if scene['main'] is None:
        w.spawn('hero', 'item', (0.5, 1.3, 0.5), scene['item'])
    for st in scene['steps']:
        w.step += 1
        if 't' in st:
            continue
        for d in w.drops: w.ents.pop(d, None)
        w.drops = []
        w.subjects = []
        for o in st['a']:
            op(w, o)
        frames.append((w.step, [o['op'] for o in st['a']], dict(w.blocks), dict(w.ents), list(w.subjects), w.laser))
    return w, frames

def op(w, o):
    t = o['op']
    if t == 'block': w.place(o['pos'], o['id'])
    elif t == 'line':
        for c in o['cells']: w.place(c, o['id'])
    elif t == 'swap':
        if w.placed: w.destroy(CENTER)
        w.place(CENTER, o['id'], 'swap')
    elif t == 'swapcells':
        for c in o['cells']: w.place(c, o['id'], 'swap')
    elif t == 'machineon': w.subjects.append(('b', tuple(o['pos'])))
    elif t == 'breakat': w.destroy(o['pos'])
    elif t == 'panel':
        target = CENTER
        title = o.get('title')
        if title == 'minecraft:crafting_table':
            if w.crafter is None:
                w.crafter = (5, 1, 1); w.place(w.crafter, 'minecraft:crafting_table')
            target = w.crafter
        elif o.get('at'):
            target = tuple(o['at'])
        elif not w.placed and title:
            w.place(CENTER, title)
        if target not in w.blocks:
            w.note('panel-no-machine', f'panel for {title} at {target} but nothing is there')
        w.subjects.append(('b', target))
        for k, (rid, n) in enumerate(o['out']):
            name = f'out{w.n}'; w.n += 1
            p = tuple(o['front']) if o.get('front') else (target[0] + 0.5, target[1], target[2] - 0.75)
            w.spawn(name, 'item', p, rid); w.drops.append(name)
        for rid, n in o['in']:
            name = f'in{w.n}'; w.n += 1
            w.spawn(name, 'item', (target[0] + 0.5, o.get('top', target[1] + 1.1), target[2] + 0.5), rid, check=True)
            w.ents.pop(name)
    elif t == 'gauge': w.subjects.append(('b', CENTER))
    elif t == 'laser':
        w.laser = ((3.5, 1.5, 3.0), (3.5, 1.5, 0.5))
        for p2, b2 in w.blocks.items():
            if p2[0] == 3 and p2[1] == 1 and 0 <= p2[2] <= 2:
                w.note('laser-blocked', f'laser beam goes through {b2} at {p2}')
    elif t == 'laseroff': w.laser = None
    elif t == 'explode':
        if o['break']:
            for p in list(w.blocks):
                if math.dist((p[0] + .5, p[1] + .5, p[2] + .5), (3.5, 1.5, 3.5)) <= o['r']: w.destroy(p)
    elif t == 'player':
        feet = tuple(o['at']) if 'at' in o else (0.5, 1, 5.5)
        if w.player is None:
            w.player = feet; w.spawn('player', 'player', feet, 'player')
        elif feet != w.player:
            w.path_check('player', w.player, feet, 'player'); w.player = feet
            w.spawn('player', 'player', feet, 'player')
        else:
            w.subjects.append(('e', 'player'))
        if o.get('move'):
            to = (feet[0] + 2, feet[1], feet[2]); w.path_check('player', feet, to, 'player'); w.player = to
            w.spawn('player', 'player', to, 'player')
        elif o.get('rise'):
            to = (feet[0], feet[1] + 2.5, feet[2]); w.player = to; w.spawn('player', 'player', to, 'player')
    elif t == 'mine':
        for c, bid in o['place']: w.place(c, bid)
        stand = tuple(o['stand'])
        if w.player is None:
            w.spawn('player', 'player', stand, 'player'); w.player = stand
        else:
            if w.player != stand: w.path_check('player', w.player, stand, 'player')
            w.player = stand; w.spawn('player', 'player', stand, 'player')
        for c in o['targets']:
            if o['mode'] in ('each', 'fast') and o['yaw'] in (0, 180):
                to = (c[0] + 0.5, stand[1], stand[2]); w.path_check('player', w.player, to, 'player'); w.player = to
                w.spawn('player', 'player', to, 'player')
            w.destroy(c)
        if o.get('drop'):
            name = f'drop{w.n}'; w.n += 1; w.spawn(name, 'item', (o['targets'][0][0] + .5, 1, o['targets'][0][2] + .5), o['drop']); w.drops.append(name)
            if tuple(o['targets'][0]) == CENTER: w.placed = False
    elif t == 'mob':
        at = tuple(o['pos']) if 'pos' in o else (5.5, 1, 1.5)
        kind = 'item' if (':' in o['type'] and not o['type'].startswith('minecraft:')) else o['type'].split(':')[-1]
        if kind == 'item': at = (at[0], at[1] + 0.3, at[2])
        w.mob = (kind, at); w.spawn('mob', kind, at, o['type'])
    elif t == 'mobact':
        if w.mob:
            kind, at = w.mob
            if o['kind'] == 'kill': w.ents.pop('mob', None); w.mob = None
            elif o['kind'] == 'float': w.spawn('mob', kind, (at[0], at[1] + 1.6, at[2]), 'mob')
            else:
                to = tuple(o['to']) if 'to' in o else (3.5, at[1], 3.5); to = (to[0], at[1], to[2]); w.path_check(kind, at, to, 'mob'); w.spawn('mob', kind, to, 'mob(moved)')
    elif t == 'show':
        spots = [(1.5, 1.3, 1.5), (2.5, 1.3, 1.5), (1.5, 1.3, 0.5), (2.5, 1.3, 0.5), (0.5, 1.3, 1.5), (4.5, 1.3, 0.5)]
        for i in o['ids']:
            spot = spots[w.item_spot % len(spots)]; w.item_spot += 1
            if is_block_item(i):
                w.place((math.floor(spot[0]), 1, math.floor(spot[2])), i)
            else:
                name = f'show{w.n}'; w.n += 1; w.spawn(name, 'item', spot, i); w.drops.append(name)
    elif t == 'blocks':
        for pos, bid in o['list']: w.place(pos, bid)
    elif t == 'floor': pass
    elif t == 'remove':
        for c in o['cells']:
            w.destroy(c)
            if tuple(c) == CENTER: w.placed = False
    elif t == 'destroy':
        for c in o['cells']:
            w.destroy(c)
            if tuple(c) == CENTER: w.placed = False
    elif t == 'carry':
        if o.get('keep'):
            path = o['movers'][0]['path'] if 'movers' in o else o['path']
            name = f'carry{w.n}'; w.n += 1; w.spawn(name, 'item', tuple(path[-1]), o.get('item', 'item'), check=False); w.drops.append(name)
    elif t == 'item':
        name = f'item{w.n}'; w.n += 1; w.spawn(name, 'item', tuple(o['at']), o['id']); w.drops.append(name)
    elif t == 'actor':
        if o.get('hide'):
            w.ents.pop('player', None); w.player = None; return
        at = tuple(o['at']) if 'at' in o else (w.player or (0.5, 1, 5.5))
        if w.player is not None and at != w.player and not o.get('jump'):
            w.path_check('player', w.player, at, 'player')
        w.player = at; w.spawn('player', 'player', at, 'player')
        for pt in o.get('path', []):
            q = tuple(pt[:3]); w.path_check('player', w.player, q, 'player'); w.player = q
            w.spawn('player', 'player', q, 'player')
    elif t == 'beam':
        w.laser = (tuple(o['from']), tuple(o['to']))
    elif t == 'beamsoff': w.laser = None
    elif t in ('active', 'lever', 'indicate', 'wait', 'flash', 'sound', 'stream'):
        if 'pos' in o and tuple(o['pos']) in w.blocks: w.subjects.append(('b', tuple(o['pos'])))
    elif t in ('click', 'pulse', 'lightning'):
        if w.placed: w.subjects.append(('b', CENTER))

ITEMS_NOT_BLOCKS = set()
def is_block_item(i):
    if i.startswith('minecraft:'):
        return i.split(':')[1] in ('stone', 'dirt', 'oak_log', 'iron_ore', 'chest', 'cobblestone', 'tnt', 'lever', 'crafting_table', 'obsidian', 'sand', 'gravel')
    return i in B or is_tx(i)

# ---------------------------------------------------------------- rendering
def Rx(a):
    a = math.radians(a); c, s = math.cos(a), math.sin(a); return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
def Ry(a):
    a = math.radians(a); c, s = math.cos(a), math.sin(a); return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
M = Rx(-35) @ Ry(145) @ np.diag([1, -1, 1])
VIEW = np.linalg.inv(M) @ np.array([0, 0, 1.0])   # toward the viewer
S = 42
OFF = np.array([3.5, 1.2, 3.5])
W, H = 560, 470
def proj(p):
    v = M @ (np.array(p) - OFF) * S
    return (W / 2 + v[0], H / 2 + v[1])

def faces(bx):
    x0, y0, z0, x1, y1, z1 = bx
    return [[(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],   # top
            [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)],   # north
            [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)]]   # west

def color(bid):
    if 'universal_cable' in bid: return (230, 140, 40)
    if 'pressurized_tube' in bid: return (230, 230, 120)
    if 'mechanical_pipe' in bid: return (70, 120, 230)
    if 'transporter' in bid: return (90, 200, 90)
    if 'conductor' in bid: return (200, 90, 60)
    if 'energy_cube' in bid: return (200, 60, 60)
    if 'chest' in bid or 'crafting' in bid: return (150, 100, 50)
    if bid.startswith('minecraft:'): return (130, 130, 130)
    return (110, 130, 160)

def boxes_of(blocks, ents):
    out = []
    for p, b in blocks.items():
        for bx in shapes(b, p): out.append((bx, color(b), ('b', p)))
    for name, (k, p, lab) in ents.items():
        c = (40, 220, 220) if k == 'player' else (230, 80, 200) if k == 'item' else (240, 160, 180)
        out.append((ent_box(k, p), c, ('e', name)))
    return out

def render(blocks, ents, ids=False, only=None):
    img = Image.new('RGB', (W, H), (30, 30, 36) if not ids else (0, 0, 0))
    dr = ImageDraw.Draw(img)
    if not ids:
        for x in range(8):
            dr.line([proj((x, 1, 0)), proj((x, 1, 7))], fill=(70, 70, 80))
        for z in range(8):
            dr.line([proj((0, 1, z)), proj((7, 1, z))], fill=(70, 70, 80))
    bxs = boxes_of(blocks, ents)
    key = {}
    bxs.sort(key=lambda t: np.dot(VIEW, [(t[0][0] + t[0][3]) / 2, (t[0][1] + t[0][4]) / 2, (t[0][2] + t[0][5]) / 2]))
    for i, (bx, col, tag) in enumerate(bxs):
        if only is not None and tag != only: continue
        idc = (i + 1, 0, 0) if ids else None
        key[i + 1] = tag
        for fi, f in enumerate(faces(bx)):
            sh = [1.0, 0.8, 0.62][fi]
            c = idc if ids else tuple(int(v * sh) for v in col)
            dr.polygon([proj(p) for p in f], fill=c, outline=None if ids else (20, 20, 20))
    return img, key

def visibility(blocks, ents, tag):
    full, key = render(blocks, ents, ids=True)
    alone, _ = render(blocks, ents, ids=True, only=tag)
    a = np.array(full)[:, :, 0]; b = np.array(alone)[:, :, 0]
    idx = [k for k, v in key.items() if v == tag]
    tot = int((b > 0).sum())
    if tot == 0: return 1.0
    vis = int(np.isin(a, idx).sum())
    return vis / tot

if __name__ == '__main__':
    import os
    os.makedirs('/home/claude/mmpsim/out', exist_ok=True)
    report = collections.defaultdict(list)
    for sc in SC:
        w, frames = run(sc)
        for st, kind, msg in w.issues: report[sc['id']].append((st, kind, msg))
        for (st, ops, blocks, ents, subj, laser) in frames:
            seen = set()
            for tag in subj:
                if tag in seen: continue
                seen.add(tag)
                if tag[0] == 'e' and tag[1] not in ents: continue
                if tag[0] == 'b' and tag[1] not in blocks: continue
                v = visibility(blocks, ents, tag)
                if v < 0.55:
                    lab = blocks[tag[1]] if tag[0] == 'b' else ents[tag[1]][2]
                    report[sc['id']].append((st, 'hidden', f'{lab} {tag[1]} only {int(v*100)}% visible'))
        # transmitter connectivity at the end of each frame
        for (st, ops, blocks, ents, subj, laser) in frames:
            for p, b in blocks.items():
                if not is_tx(b): continue
                nb = 0
                for dx, dy, dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
                    q = (p[0]+dx, p[1]+dy, p[2]+dz)
                    if q in blocks and (not is_tx(blocks[q]) or tx_family(blocks[q]) == tx_family(b)): nb += 1
                    elif any(any(inter((q[0]+.3,q[1]+.3,q[2]+.3,q[0]+.7,q[1]+.7,q[2]+.7), a) for a in shapes(b2, p2)) for p2, b2 in blocks.items() if not is_tx(b2)): nb += 1
                if nb < 2:
                    report[sc['id']].append((st, 'dangling', f'{b} at {p} has {nb} connection(s)'))
    # dedupe
    out = {}
    for k, v in report.items():
        seen = []; [seen.append(x) for x in v if x not in seen]; out[k] = seen
    json.dump(out, open('/home/claude/mmpsim/report.json', 'w'), ensure_ascii=False, indent=1)
    c = collections.Counter(kind for v in out.values() for _, kind, _ in v)
    print(len(out), 'scenes with notes', c)
