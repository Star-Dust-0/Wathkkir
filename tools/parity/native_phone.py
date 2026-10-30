"""Native-first phone host test. Original prose intentionally differs from numeric timers."""
import subprocess,time,pathlib,json,re
out=pathlib.Path('evidence');out.mkdir(exist_ok=True)
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=60);return (r.stdout+r.stderr).decode(errors='replace')
def shot(n):(out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device')
for apk in ['comparison.apk','widgethost.apk']:
 result=adb('install','-r','-g',apk);(out/(apk+'-install.txt')).write_text(result);assert 'Success' in result,result
pkg='com.wathakker.app.compare'
adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo')
adb('shell','date','103010002026.00');adb('shell','wm','size','1080x2400');adb('shell','wm','density','420')
adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(5)
adb('shell','appwidget','grantbind','--package','com.example.widgethost','--user','0');adb('logcat','-c')
for mode in ['barsA','barsB','oneui','iqama','pill']:
 adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','mode',mode);time.sleep(8);shot(mode+'-8sec');time.sleep(5);shot(mode+'-13sec')
log=adb('logcat','-d');(out/'logcat.txt').write_text(log)
lines=[x for x in log.splitlines() if 'NativeTimerAudit' in x];(out/'native-timers.txt').write_text('\n'.join(lines))
active=[x for x in lines if 'visible=true' in x and 'countdown=true' in x]
kinds=sorted(set(re.findall(r'kind=([^ ]+)', '\n'.join(active))))
expected=['Hero','NextPrayer','Clock','OneUI','Pill','Lock:Next','Lock:Countdown','Lock:CountdownClean']
progress={}
for x in active:
 m=re.search(r'kind=([^ ]+).*base=(\d+) text=(.*)$',x)
 if m:progress.setdefault((m[1],m[2]),set()).add(m[3])
advanced=sorted({kind for (kind,base),texts in progress.items() if len(texts)>1})
result={'advanced_native_kinds':advanced,'not_advanced':[x for x in expected if x not in advanced],'before_prayer_host_native_kinds':kinds,'provider_dump':adb('shell','dumpsys','appwidget'),'missing':[x for x in expected if x not in kinds],'caveat':'Numeric native timers intentionally replace historical prose; bar-only widgets and physical Samsung launcher behavior remain separate.'}
(out/'result.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2));assert not result['missing'] and not result['not_advanced']
