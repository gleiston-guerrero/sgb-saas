#!/usr/bin/env python3
import re, subprocess

def load_missing():
    missing=[]
    with open('docs/mediciones/hashes-verification-report.md', encoding='utf-8') as f:
        for line in f:
            m=re.match(r'-\s*([0-9a-f]{6,40}):\s*MISSING', line)
            if m: missing.append(m.group(1))
    return missing

def main():
    missing=load_missing()
    print('Missing:', missing)
    with open('docs/mediciones/DATA-PROVENANCE.md', encoding='utf-8') as f:
        lines=f.readlines()
    changed=False
    for i,line in enumerate(lines):
        for h in missing:
            if h in line:
                files=re.findall(r"docs/[\w\-\./]+|[\w\-]+\.svg|[\w\-]+\.pdf", line)
                candidates=set()
                for fn in files:
                    try:
                        out=subprocess.check_output(['git','log','-n','1','--pretty=format:%H','--',fn], encoding='utf-8')
                        if out.strip(): candidates.add(out.strip())
                    except subprocess.CalledProcessError:
                        pass
                if candidates:
                    cand=sorted(candidates)[0]
                    lines[i]=line.replace(h, f"{h} (hash inválido; commit equivalente: {cand})")
                    changed=True
                    print('Replaced',h,'with',cand)
                else:
                    if '(hash inválido' not in line:
                        lines[i]=line.replace(h, h + ' (hash inválido; sin candidato local)')
                        changed=True
                        print('Annotated',h,'no candidate')
    if changed:
        with open('docs/mediciones/DATA-PROVENANCE.md','w', encoding='utf-8') as f:
            f.writelines(lines)
        print('DATA-PROVENANCE.md updated')
    else:
        print('No changes')

if __name__=='__main__':
    main()
