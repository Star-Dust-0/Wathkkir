"""Finite paired actual-APK watch engine coordinate grid. No production changes."""
import subprocess,pathlib,json,struct,time
out=pathlib.Path('evidence');out.mkdir(exist_ok=True)
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=240);return (r.stdout+r.stderr).decode(errors='replace')
def bits(v):return struct.unpack('>q',struct.pack('>d',v))[0]
adb('wait-for-device');adb('root');adb('wait-for-device')
for f in ['watch-original.apk','watch-rebuilt.apk','widgethost.apk']:
 s=adb('install','-r','-g',f);assert 'Success' in s,s
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 text=adb('shell','am','instrument','-w','-e','pkg',pkg,'-e','original',str(name=='original').lower(),'com.example.widgethost/.TileLayoutProbe');(out/(name+'-tile-probe.txt')).write_text(text);print(text,flush=True);assert 'INSTRUMENTATION_RESULT: error=' not in text;assert 'INSTRUMENTATION_RESULT: proto=' in text
