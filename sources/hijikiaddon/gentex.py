import sys, os, random
from PIL import Image
OUT=sys.argv[1]
PAL={
 'iri':    dict(base=(196,24,32), light=(255,96,96), dark=(110,8,16), line=(40,0,4)),
 'yowaiyo':dict(base=(243,214,120), light=(255,244,190), dark=(190,150,60), line=(80,56,16)),
}
STICK=dict(base=(122,86,48), light=(160,118,70), dark=(84,58,30), line=(40,26,12))

def line(x0,y0,x1,y1):
    pts=[];n=max(abs(x1-x0),abs(y1-y0))
    for i in range(n+1):
        pts.append((round(x0+(x1-x0)*i/n),round(y0+(y1-y0)*i/n)))
    return pts

def render(cells, mats):
    # cells: dict (x,y)->key ; mats: key->palette
    im=Image.new('RGBA',(16,16),(0,0,0,0)); px=im.load()
    for (x,y),k in cells.items():
        p=mats[k]
        nb=lambda dx,dy:(x+dx,y+dy) in cells
        c=p['base']
        if not nb(-1,0) or not nb(0,-1): c=p['light']
        if not nb(1,0) or not nb(0,1): c=p['dark']
        px[x,y]=c+(255,)
    # outline
    for (x,y),k in list(cells.items()):
        for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)):
            q=(x+dx,y+dy)
            if q not in cells and 0<=q[0]<16 and 0<=q[1]<16 and px[q][3]==0:
                px[q]=mats[k]['line']+(255,)
    return im

def handle(cells, x0=2,y0=13,x1=8,y1=7):
    for p in line(x0,y0,x1,y1): cells[p]='s'

def sword():
    c={}
    for p in line(4,11,13,2): c[p]='m'
    for p in line(5,11,13,3): c[p]='m'
    for p in line(4,10,12,2): c[p]='m'
    for p in line(2,9,6,13): c[p]='g'
    for p in line(1,14,3,12): c[p]='s'
    return c
import math
def pick_head(c):
    for y in range(16):
        for x in range(16):
            d=math.hypot(x-2,y-13); a=math.degrees(math.atan2(13-y,x-2))
            if 10.3<=d<=12.0 and 8<=a<=82: c[(x,y)]='m'
AXE=["","......###.......",".....#####......","....######......","....######......",".....####.......","......##........"]
def axe_head(c,dx=0,dy=0):
    for y,r in enumerate(AXE):
        for x,ch in enumerate(r):
            if ch=='#': c[(x+dx,y+dy)]='m'
def pickaxe():
    c={}; handle(c,2,13,9,6); pick_head(c); return c
def blade(c, x0,y0,x1,y1, t0, t1, width):
    hx,hy=x1-x0,y1-y0; L=math.hypot(hx,hy); ux,uy=hx/L,hy/L; px_,py_=uy,-ux  # side
    if px_>0: px_,py_=-px_,-py_
    for y in range(16):
        for x in range(16):
            rx,ry=x-x0,y-y0; t=(rx*ux+ry*uy)/L; d=rx*px_+ry*py_
            w=width*(1-abs((t-(t0+t1)/2)/((t1-t0)/2))**3)
            if t0<=t<=t1 and 0.3<=d<=w: c[(x,y)]='m'
def axe():
    c={}; handle(c,3,14,11,6); blade(c,3,14,11,6,0.45,1.08,6.5); return c
def paxel():
    c={}; handle(c,2,13,9,6); pick_head(c); blade(c,2,13,9,6,0.45,0.95,4.0)
    return c

def mask(rows):
    c={}
    for y,r in enumerate(rows):
        for x,ch in enumerate(r):
            if ch=='#': c[(x,y)]='m'
    return c
HELMET=["","","","....########....","...##########...","..############..","..############..","..###......###..","..##........##..","..##........##.."]
CHEST=["","..####....####..",".######..######.",".##############.",".##############.","..############..","....########....","....########....","....########....","....########....","....########....","....########....","....########....",".....######....."]
LEGS=["","","...##########...","...##########...","...####..####...","...###....###...","...###....###...","...###....###...","...###....###...","...###....###...","...###....###...","...###....###..."]
BOOTS=["","","","","","","","","...###....###...","...###....###...","...###....###...","..####....####..",".#####....#####.",".#####....#####."]

os.makedirs(OUT+'/item',exist_ok=True); os.makedirs(OUT+'/models/armor',exist_ok=True)
for m,p in PAL.items():
    mats={'m':p,'s':STICK,'g':dict(base=p['dark'],light=p['base'],dark=p['line'],line=p['line'])}
    for name,cells in [('sword',sword()),('pickaxe',pickaxe()),('axe',axe()),('paxel',paxel()),
                       ('helmet',mask(HELMET)),('chestplate',mask(CHEST)),('leggings',mask(LEGS)),('boots',mask(BOOTS))]:
        render(cells,mats).save(f'{OUT}/item/{m}_{name}.png')
    # armor layers 64x32
    rnd=random.Random(m)
    def layer(regions):
        im=Image.new('RGBA',(64,32),(0,0,0,0)); px=im.load()
        for (x0,y0,x1,y1) in regions:
            for y in range(y0,y1):
                for x in range(x0,x1):
                    edge = x in (x0,x1-1) or y in (y0,y1-1)
                    c = p['dark'] if edge else (p['light'] if rnd.random()<0.12 else p['base'])
                    px[x,y]=c+(255,)
        return im
    head=(0,0,32,16); body=(16,16,40,32); arms=(40,16,56,32); legs=(0,16,16,32)
    layer([head,body,arms,legs]).save(f'{OUT}/models/armor/{m}_layer_1.png')
    layer([body,legs]).save(f'{OUT}/models/armor/{m}_layer_2.png')
# preview sheet
names=['sword','pickaxe','axe','paxel','helmet','chestplate','leggings','boots']
sheet=Image.new('RGBA',(8*80,2*80),(60,60,60,255))
for j,m in enumerate(PAL):
    for i,n in enumerate(names):
        sheet.alpha_composite(Image.open(f'{OUT}/item/{m}_{n}.png').resize((64,64),Image.NEAREST),(i*80+8,j*80+8))
sheet.save(OUT+'/preview.png')
