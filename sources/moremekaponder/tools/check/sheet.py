import sys, json
sys.argv = [sys.argv[0]] + sys.argv[1:]
from sim import *
from PIL import ImageFont
F = ImageFont.load_default()
def sheet(sc, path):
    w, frames = run(sc)
    tiles = []
    for (st, ops, blocks, ents, subj, laser) in frames:
        img, _ = render(blocks, ents)
        dr = ImageDraw.Draw(img)
        if laser:
            dr.line([proj(laser[0]), proj(laser[1])], fill=(255, 40, 40), width=3)
        dr.text((6, 6), f'step {st}: ' + ','.join(ops), fill=(255, 255, 255), font=F)
        y = 20
        for p, b in sorted(blocks.items()):
            pass
        for tag in subj[:4]:
            dr.text((6, y), str(tag), fill=(255, 255, 0), font=F); y += 12
        tiles.append(img.resize((W // 2 * 1, H // 2 * 1)))
    if not tiles: return
    cols = 3; rows = (len(tiles) + cols - 1) // cols
    out = Image.new('RGB', (cols * (W // 2), rows * (H // 2) + 16), (0, 0, 0))
    ImageDraw.Draw(out).text((4, 2), sc['id'], fill=(255, 255, 255), font=F)
    for i, t in enumerate(tiles):
        out.paste(t, ((i % cols) * (W // 2), 16 + (i // cols) * (H // 2)))
    out.save(path)
if __name__ == '__main__':
    import os
    names = sys.argv[1:] or [s['id'] for s in SC]
    for sc in SC:
        if sc['id'] in names: sheet(sc, f'/home/claude/mmpsim/out/{sc["id"]}.png')
