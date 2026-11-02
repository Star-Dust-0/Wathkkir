"""Finite paired actual-APK watch engine coordinate grid. No production changes."""
import subprocess,pathlib,json,struct,time
out=pathlib.Path('evidence');out.mkdir(exist_ok=True)
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=240);return (r.stdout+r.stderr).decode(errors='replace')
def bits(v):return struct.unpack('>q',struct.pack('>d',v))[0]
adb('wait-for-device');adb('root');adb('wait-for-device')
for f in ['watch-original.apk','watch-rebuilt.apk','widgethost.apk','tilehost.apk']:
 s=adb('install','-r','-g',f);assert 'Success' in s,s

import base64
# Explicit globals/state fixtures, NOT preferences/lifecycle integration or dynamic-mode parity.
cases=[(f'{lang}-t{theme}-{state}',lang,theme,state) for lang in ['ar','en'] for theme in range(4) for state in ['start','middle','iqama-end']]
def vi(n):
 b=bytearray()
 while n>127:b.append((n&127)|128);n>>=7
 b.append(n);return bytes(b)
time.sleep(35)
adb('shell','dumpsys','battery','unplug');adb('shell','svc','power','stayon','true');adb('shell','wm','dismiss-keyguard')
results=[]
for case,lang,theme,state in cases:
 before,after,iqama,h24={'start':(0,3600000,False,False),'middle':(1800000,1800000,False,True),'iqama-end':(1200000,0,True,False)}[state]
 for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
  prefix=case+'-'+name
  text=adb('shell','am','instrument','-w','-e','pkg',pkg,'-e','original',str(name=='original').lower(),'-e','en',str(lang=='en').lower(),'-e','theme',str(theme),'-e','before',str(before),'-e','after',str(after),'-e','iqama',str(iqama).lower(),'-e','h24',str(h24).lower(),'com.example.widgethost/.TileLayoutProbe')
  (out/(prefix+'-probe.txt')).write_text(text)
  assert 'INSTRUMENTATION_RESULT: error=' not in text,text
  raw=base64.b64decode(next(x.split('=',1)[1] for x in text.splitlines() if x.startswith('INSTRUMENTATION_RESULT: bytes=')))
  layout=base64.b64encode(b'\x0a'+vi(len(raw))+raw).decode()
  adb('shell','am','force-stop','com.example.tilehost')
  valid=False
  for attempt in range(4):
   adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard')
   launch=adb('shell','am','start','-W','-n','com.example.tilehost/com.example.widgethost.TileReferenceActivity','--es','layout',layout)
   (out/(prefix+'-launch-'+str(attempt)+'.txt')).write_text(launch);time.sleep(3)
   tree=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump');(out/(prefix+'-hierarchy-'+str(attempt)+'.txt')).write_text(tree)
   (out/(prefix+'-attempt-'+str(attempt)+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
   expected=('Iqama Fajr' if lang=='en' else 'إقامة الفجر') if iqama else ('Dhuhr' if lang=='en' else 'الظهر')
   if expected in tree:valid=True;break
   time.sleep(3)
  results.append({'case':case,'app':name,'valid':valid,'expected':expected});(out/'matrix-valid.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
  assert valid,'No actual tile rendered for '+prefix
  (out/(prefix+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
  (out/(prefix+'-log.txt')).write_text(adb('logcat','-d','-s','TileReference'))
  print(prefix,'rendered',flush=True)
