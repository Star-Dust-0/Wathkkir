"""Oct25 merge + Oct26 recovery acceptance: real hosts, paired pixels, taps, alarms."""
import subprocess,time,pathlib,json,re,xml.etree.ElementTree as ET
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=70);return (r.stdout+r.stderr).decode(errors='replace')
def check(item,ok,detail=''):checks.append(dict(item=item,passed=bool(ok),detail=detail));print(item,bool(ok),detail,flush=True)
def shot(n):
 state=adb('shell','dumpsys','window');(out/(n+'-window.txt')).write_text(state)
 check(n+' foreground',any('mCurrentFocus=' in x and 'com.example.widgethost' in x for x in state.splitlines()) and 'Application Not Responding' not in state)
 (out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
def host(pkg,mode,tap=None):
 args=['shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','mode',mode]
 if tap:args+=['--es','tapKind',tap]
 adb(*args)
def prefs(pkg,lang,extra=''):
 adb('shell','am','force-stop',pkg);folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
 app=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
 fajr='<map><float name="latitude" value="30.8619"/><float name="longitude" value="29.5845"/><string name="city_name">New Borg El Arab</string><string name="city_name_ar">برج العرب الجديدة</string><boolean name="use_current_location" value="false"/><int name="calculation_method" value="1"/>'+extra+'</map>'
 for f,x in [('app_prefs',app),('fajr_prefs',fajr)]:adb('shell',f"echo '{x}' > {folder}/{f}.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/{f}.xml; chmod 660 {folder}/{f}.xml; restorecon -R {folder}")
adb('wait-for-device');adb('root');time.sleep(4);adb('wait-for-device')
for a in ['original.apk','comparison.apk','widgethost.apk']:check(a+' installed','Success' in adb('install','-r','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420')
adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo')
adb('shell','am','force-stop','com.google.android.apps.nexuslauncher');adb('shell','input','keyevent','KEYCODE_BACK');adb('shell','appwidget','grantbind','--package','com.example.widgethost','--user','0')
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 prefs(pkg,'ar');adb('shell','cmd','uimode','night','yes')
 for kind in ['Hero','NextPrayer','PrayerBar','Clock','Pill','OneUI','Schedule','DayPrayer','Hijri','Gregorian']:
  adb('shell','date','103110002026.00');adb('logcat','-c');adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','mode','single','--es','kind',kind);time.sleep(7)
  log=adb('logcat','-d');ids=re.findall(r'WidgetRecoveryAudit.*bound kind='+kind+r' id=(\d+) ok=true',log);check(name+' '+kind+' bind',len(ids)==1)
  if not ids:continue
  i=int(ids[0]);shot(name+'-'+kind+'-bound')
  for width,height in [(160,90),(330,160),(240,110),(330,90)]:
   adb('logcat','-c');adb('shell','am','broadcast','-a','com.example.widgethost.RESIZE','--ei','width',str(width),'--ei','height',str(height));time.sleep(4);tag=name+'-'+kind+'-'+str(width)+'x'+str(height);shot(tag);log=adb('logcat','-d');(out/(tag+'-log.txt')).write_text(log);check(tag+' resize callback',bool(re.search(r'WidgetResizeAudit.*kind='+kind+r' id='+str(i)+r' outer='+str(width)+'x'+str(height),log)));stats=re.findall(r'WidgetPix.*'+kind+r' w=(\d+) h=(\d+) std=([\d.]+)',log);check(tag+' nonblank sized',any(int(w)>0 and int(h)>0 and float(std)>2 for w,h,std in stats),str(stats));check(tag+' no rendering failure',not any(t in log for t in ['FATAL EXCEPTION','TransactionTooLargeException','RemoteViews$ActionException']))
 adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','operation','delete');time.sleep(2)
 alarm=adb('shell','dumpsys','alarm');(out/(name+'-delete-alarms.txt')).write_text(alarm);active=alarm.split('Past-due non-wakeup alarms:')[0].split('Alarm Stats:')[0];check(name+' final deletion cancels pulses',not re.search(r'\*walarm\*:'+re.escape(pkg)+r'\.lockwidgets\.(TICK|BOUNDARY)',active))
(out/'checks.json').write_text(json.dumps(checks,indent=2));print('checks',len(checks),'fails',sum(not x['passed'] for x in checks));assert all(x['passed'] for x in checks)
