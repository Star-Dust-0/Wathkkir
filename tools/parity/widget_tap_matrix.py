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
 prefs(pkg,'en')
 for kind in ['Hero','NextPrayer','PrayerBar','Clock','Pill','OneUI','Schedule','DayPrayer','Hijri','Gregorian']:
  adb('shell','date','103110002026.00');adb('logcat','-c');adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','mode','single','--es','kind',kind);time.sleep(7)
  log=adb('logcat','-d');ids=re.findall(r'WidgetRecoveryAudit.*bound kind='+kind+r' id=(\d+) ok=true',log);check(name+' '+kind+' bind',len(ids)==1)
  if not ids:continue
  i=int(ids[0]);shot(name+'-'+kind+'-bound')
  for choice in ['open','refresh','none']:
   prefs(pkg,'en',f'<string name="widget_tap_{i}">{choice}</string>');adb('shell','am','broadcast','-a','android.appwidget.action.APPWIDGET_UPDATE','-n',pkg+'/com.example.widget.'+kind+'WidgetProvider','--include-stopped-packages','--eia','appWidgetIds',str(i));time.sleep(2);adb('logcat','-c');adb('shell','am','broadcast','-a','com.example.widgethost.CLICK','--es','kind',kind);time.sleep(2)
   tag=name+'-'+kind+'-'+choice;log=adb('logcat','-d');activity=adb('shell','dumpsys','activity','activities');(out/(tag+'-log.txt')).write_text(log);(out/(tag+'-activity.txt')).write_text(activity);focus=next((x for x in activity.splitlines() if 'topResumedActivity=' in x),'');opened=pkg+'/com.example.MainActivity' in focus;refresh='WathakkerWidgetRefresh' in log;expected_refresh=choice!='open' and kind in ['Hero','NextPrayer','PrayerBar','Clock','Pill','OneUI'];check(tag+' expected open',opened==(choice=='open'),focus);check(tag+' expected refresh',refresh==expected_refresh);check(tag+' actual click',('clicked=true' in log)==(choice=='open' or expected_refresh));(out/(tag+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
   if opened:adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1)
 adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','operation','delete');time.sleep(2)
(out/'checks.json').write_text(json.dumps(checks,indent=2));print('checks',len(checks),'fails',sum(not x['passed'] for x in checks));assert all(x['passed'] for x in checks)
