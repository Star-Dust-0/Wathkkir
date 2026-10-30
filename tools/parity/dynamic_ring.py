"""Single-build reference-host test, WearOS6. Does not claim Samsung watchface proof."""
import subprocess,time,pathlib,json,re
out=pathlib.Path('evidence');out.mkdir(exist_ok=True)
def adb(*a):return subprocess.run(['adb',*a],capture_output=True,text=True,timeout=60).stdout
adb('wait-for-device');time.sleep(35)
(out/'device.txt').write_text(adb('shell','getprop'))
(out/'install.txt').write_text(adb('install','-g','watch-rebuilt.apk'))
adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard');adb('shell','svc','power','stayon','true');adb('logcat','-c')
(out/'launch.txt').write_text(adb('shell','am','start','-W','-n','com.wathakker.app/com.wathakker.wear.DynamicHostActivity'))
for sec in [5,35,65,125]:
 time.sleep(sec-(0 if sec==5 else {35:5,65:35,125:65}[sec]))
 (out/f'ring-{sec}.png').write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
log=adb('logcat','-d');(out/'logcat.txt').write_text(log)
values=[float(x) for x in re.findall(r'evaluated elapsed=([0-9.]+)',log)]
assert 'sdk=36 dynamic=true' in log,'No dynamic API36 data'
assert len(values)>3 and values[-1]>values[0]+1,'Ring failed to advance without provider rebuild'
assert log.count('provider_builds=1')==1,'Provider unexpectedly rebuilt'
(out/'result.json').write_text(json.dumps({'host':'debug AndroidX reference host, not Samsung watchface','sdk':36,'provider_builds':1,'first':values[0],'last':values[-1],'samples':len(values)},indent=2))
