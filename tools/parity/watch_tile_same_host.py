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
# Render both actual serialized outputs with one identical verification host/library.
import base64
for name in ['original','comparison']:
 text=(out/(name+'-tile-probe.txt')).read_text();raw=base64.b64decode(next(x.split('=',1)[1] for x in text.splitlines() if x.startswith('INSTRUMENTATION_RESULT: bytes=')))
 def vi(n):
  b=bytearray()
  while n>127:b.append((n&127)|128);n>>=7
  b.append(n);return bytes(b)
 layout=base64.b64encode(b'\x0a'+vi(len(raw))+raw).decode()
 adb('shell','am','force-stop','com.example.widgethost');adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard')
 adb('shell','am','start','-W','-n','com.example.widgethost/.TileReferenceActivity','--es','layout',layout);time.sleep(5)
 (out/(name+'-same-host.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
 (out/(name+'-same-host-log.txt')).write_text(adb('logcat','-d'))
