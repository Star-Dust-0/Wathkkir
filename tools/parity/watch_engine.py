"""Paired actual APK engine outputs, no UI/timezone-inference substitute."""
import subprocess,pathlib,json
out=pathlib.Path('evidence');out.mkdir(exist_ok=True)
def adb(*a):return subprocess.run(['adb',*a],capture_output=True,timeout=180).stdout.decode(errors='replace')
adb('wait-for-device');adb('root');adb('wait-for-device')
for f in ['watch-original.apk','watch-rebuilt.apk','widgethost.apk']:
 s=adb('install','-r','-g',f);assert 'Success' in s,s
states={}
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 adb('shell','am','force-stop',pkg);adb('shell','pm','clear',pkg);(out/(name+'-package.txt')).write_text(adb('shell','pm','path',pkg))
 s=adb('shell','am','instrument','-w','-e','pkg',pkg,'-e','original',str(name=='original').lower(),'com.example.widgethost/.WatchEngineProbe');(out/(name+'-engine-raw.txt')).write_text(s)
 line=next((x.split('=',1)[1] for x in s.splitlines() if x.startswith('INSTRUMENTATION_RESULT: engine=')),None)
 assert line,s
 states[name]=json.loads(line);(out/(name+'-engine.json')).write_text(json.dumps(states[name],indent=2))
assert len(states['original'])==len(states['comparison'])==810
mismatch=[{'original':a,'comparison':b} for a,b in zip(states['original'],states['comparison']) if a!=b]
(out/'engine-comparison.json').write_text(json.dumps({'cases':len(states['original']),'mismatches':mismatch},indent=2));print('cases',len(states['original']),'mismatch',len(mismatch));assert not mismatch
