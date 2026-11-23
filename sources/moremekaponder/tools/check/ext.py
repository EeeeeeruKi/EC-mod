import json,os,re,sys,glob
ROOT='/home/claude/mekx/assets'
def load_model(ref, depth=0):
    ns,path=(ref.split(':')+[''])[:2] if ':' in ref else ('minecraft',ref)
    f=f'{ROOT}/{ns}/models/{path}.json'
    if not os.path.isfile(f): return None
    m=json.load(open(f))
    return m
def elements_bounds(els):
    lo=[99,99,99]; hi=[-99,-99,-99]
    for e in els:
        for i in range(3):
            lo[i]=min(lo[i],e['from'][i]); hi[i]=max(hi[i],e['to'][i])
    return (lo,hi,'el')
def model_bounds(ref, depth=0):
    m=load_model(ref)
    if m is None: return None
    if m.get('loader')=='neoforge:composite' and 'children' in m:
        lo=[99,99,99]; hi=[-99,-99,-99]; ok=False
        for c in m['children'].values():
            r=None
            if 'elements' in c: r=elements_bounds(c['elements'])
            elif 'parent' in c: r=model_bounds(c['parent'],depth+1)
            if r and isinstance(r[0],list):
                ok=True
                for i in range(3): lo[i]=min(lo[i],r[0][i]); hi[i]=max(hi[i],r[1][i])
        if ok: return (lo,hi,'comp')
    if 'elements' in m:
        lo=[99,99,99]; hi=[-99,-99,-99]
        for e in m['elements']:
            for i in range(3):
                lo[i]=min(lo[i],e['from'][i]); hi[i]=max(hi[i],e['to'][i])
        return (lo,hi,'el')
    if m.get('loader') and 'model' in m and isinstance(m['model'],str) and m['model'].endswith('.obj'):
        ns,p=m['model'].split(':'); f=f'{ROOT}/{ns}/{p}'
        if os.path.isfile(f):
            vs=[list(map(float,l.split()[1:4])) for l in open(f) if l.startswith('v ')]
            lo=[min(v[i] for v in vs)*16 for i in range(3)]; hi=[max(v[i] for v in vs)*16 for i in range(3)]
            return (lo,hi,'obj')
        return ('objmissing',f)
    if 'parent' in m and depth<6:
        r=model_bounds(m['parent'],depth+1)
        return r
    return None
def block_model(bid):
    ns,n=bid.split(':'); f=f'{ROOT}/{ns}/blockstates/{n}.json'
    if not os.path.isfile(f): return None
    b=json.load(open(f))
    if 'variants' in b:
        for k,v in b['variants'].items():
            if isinstance(v,list): v=v[0]
            if 'north' in k or len(b['variants'])==1 or True:
                return v['model']
    if 'multipart' in b:
        return b['multipart'][0]['apply']['model'] if isinstance(b['multipart'][0]['apply'],dict) else b['multipart'][0]['apply'][0]['model']
if __name__=='__main__':
    for bid in sys.argv[1:]:
        m=block_model(bid); print(bid, m, model_bounds(m) if m else None)
