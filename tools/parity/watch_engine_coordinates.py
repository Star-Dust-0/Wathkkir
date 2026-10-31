"""Finite paired actual-APK watch engine coordinate grid. No production changes."""
import subprocess,pathlib,json,struct,time
out=pathlib.Path('evidence');out.mkdir(exist_ok=True)
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=240);return (r.stdout+r.stderr).decode(errors='replace')
def bits(v):return struct.unpack('>q',struct.pack('>d',v))[0]
adb('wait-for-device');adb('root');adb('wait-for-device')
for f in ['watch-original.apk','watch-rebuilt.apk','widgethost.apk']:
 s=adb('install','-r','-g',f);assert 'Success' in s,s
coords=[('cairo',30.0444,31.2357),('london',51.5074,-.1278),('newyork',40.7128,-74.006),('sydney',-33.8688,151.2093),('quito',-.1807,-78.4678),('tromso',69.6492,18.9553),('capetown',-33.9249,18.4241)]
states={}
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 states[name]={}
 for city,lat,lon in coords:
  adb('shell','am','force-stop',pkg);adb('shell','pm','clear',pkg);folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
  x=f'<map><int name="locmode" value="0"/><long name="lat" value="{bits(lat)}"/><long name="lon" value="{bits(lon)}"/></map>'
  adb('shell',f"echo '{x}' > {folder}/wear_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/wear_prefs.xml; chmod 660 {folder}/wear_prefs.xml; restorecon -R {folder}")
  (out/(name+'-'+city+'-fixture.xml')).write_text(adb('shell','cat',folder+'/wear_prefs.xml'))
  s=adb('shell','am','instrument','-w','-e','pkg',pkg,'-e','original',str(name=='original').lower(),'-e','lat',str(lat),'-e','lon',str(lon),'com.example.widgethost/.WatchEngineProbe');(out/(name+'-'+city+'-raw.txt')).write_text(s)
  line=next((x.split('=',1)[1] for x in s.splitlines() if x.startswith('INSTRUMENTATION_RESULT: engine=')),None)
  assert line,s
  states[name][city]=json.loads(line);assert len(states[name][city])==810
  (out/(name+'-'+city+'.json')).write_text(json.dumps(states[name][city],indent=2))
mismatches=[]
for city,lat,lon in coords:
 for a,b in zip(states['original'][city],states['comparison'][city]):
  if a!=b:mismatches.append({'city':city,'original':a,'comparison':b})
sensitive={name:{city:sum(a!=b for a,b in zip(d['cairo'],values)) for city,values in d.items()} for name,d in states.items()}
result={'cases':810*len(coords),'outputs':810*len(coords)*6,'mismatches':mismatches,'coordinate_sensitivity':sensitive}
(out/'coordinate-comparison.json').write_text(json.dumps(result,indent=2));print('cases',result['cases'],'mismatches',len(mismatches),'sensitivity',sensitive);assert not mismatches;assert all(n>0 for d in sensitive.values() for city,n in d.items() if city!='cairo')
