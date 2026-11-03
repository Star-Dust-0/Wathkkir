"""Bounded actual Android provider IPC on rooted Wear emulator.
Registered instrumentation host adopts narrowly scoped shell permissions; not a native watchface,
DataLayer pairing, or physical OEM result. No service class instantiated by reflection.
"""
import subprocess,time,pathlib,json,re,base64
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=240);return (r.stdout+r.stderr).decode(errors='replace')
def check(n,v,detail=''):
 checks.append(dict(name=n,passed=bool(v),detail=detail));(out/'checks.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2));print(n,v,detail,flush=True)
def shot(n):
 (out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']));(out/(n+'-window.txt')).write_text(adb('shell','dumpsys','window'))
adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device')
for f in ['watch-original.apk','watch-rebuilt.apk','widgethost.apk','tilehost.apk']:check('install '+f,'Success' in adb('install','-r','-g',f))
adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','cmd','alarm','set-timezone','UTC');adb('shell','date','103012002026.00')

for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for other in ['com.wathakker.app','com.wathakker.app.compare']:adb('shell','am','force-stop',other)
 adb('shell','pm','clear',pkg);
 for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',pkg,'android.permission.'+perm)
 adb('shell','am','start','-W','-n',pkg+'/com.wathakker.wear.MainActivity');time.sleep(5)
 # Explicit fixture prefs, not transport. EN, fixed city/coordinates matching engine fixtures.
 folder=f'/data/data/{pkg}/shared_prefs';xml='<map><int name="lang" value="2"/><int name="theme" value="0"/><boolean name="perm_asked" value="true"/></map>'
 adb('shell','am','force-stop',pkg);adb('shell',f"echo '{xml}' > {folder}/wear_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/wear_prefs.xml; chmod 660 {folder}/wear_prefs.xml; restorecon -R {folder}")
 # Clear force-stop via launcher, then bind live services from registered instrumentation context.
 adb('shell','am','start','-W','-n',pkg+'/com.wathakker.wear.MainActivity');time.sleep(3);adb('logcat','-c')
 manifest=adb('shell','dumpsys','package',pkg);(out/(name+'-package.txt')).write_text(manifest);check(name+' tile provider discoverable','androidx.wear.tiles.action.BIND_TILE_PROVIDER:' in manifest)
 text=adb('shell','am','instrument','--user','0','-w','-e','pkg',pkg,'com.example.tilehost/com.example.widgethost.ProviderServiceProbe');(out/(name+'-actual-service.txt')).write_text(text);print(text,flush=True);check(name+' registered app context','PROBE host_package=com.example.tilehost' in text and 'PROBE host_context=android.app.ContextImpl' in text);check(name+' service probe complete','PROBE complete=true' in text,text[-1000:]);shot(name+'-complication-tap')
 (out/(name+'-alarm.txt')).write_text(adb('shell','dumpsys','alarm'));(out/(name+'-service.txt')).write_text(adb('shell','dumpsys','activity','services',pkg));(out/(name+'-logcat.txt')).write_text(adb('logcat','-d'))
 for cls,typ in [('PrayerComplicationService',5),('DateGregComplication',3),('DateHijriComplication',3),('DateBothComplication',4),('NextTimeComplication',3),('UpcomingCountdownComplication',3)]:
  check(name+' '+cls+' actual requested type',f'PROBE {cls}_type={typ}' in text);check(name+' '+cls+' actual tap sent',f'PROBE {cls}_tap_sent=true' in text)
 layouts=re.findall(r'^PROBE tile_layout=(.+)$',text,re.M)
 if layouts:
  adb('shell','am','force-stop','com.example.tilehost');adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard');adb('shell','am','start','-W','-n','com.example.tilehost/com.example.widgethost.TileReferenceActivity','--es','layout',layouts[0]);time.sleep(5);shot(name+'-actual-service-tile');(out/(name+'-tile-host-log.txt')).write_text(adb('logcat','-d'))
assert all(x['passed'] for x in checks),'Preserve actual failed service evidence'
