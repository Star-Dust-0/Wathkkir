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
def counts(pkg):
 x=adb('shell','cat',f'/data/data/{pkg}/shared_prefs/lock_tasbih.xml')
 try:return {n.get('name'):n.get('value',n.text) for n in ET.fromstring(x).iter() if n.get('name')}
 except:return {}
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang in ['en','ar']:
  prefs(pkg,lang);adb('shell','cmd','uimode','night','yes' if lang=='ar' else 'no');adb('shell','date','103110002026.00');adb('logcat','-c');host(pkg,'recover-lockA');time.sleep(5);tag=name+'-'+lang;log=adb('logcat','-d');ids=re.findall(r'WidgetRecoveryAudit.*bound kind=Lock:Tasbih id=(\d+) ok=true',log);check(tag+' Tasbih bound',len(ids)==1)
  if not ids:continue
  wid=ids[0];shot(tag+'-count-0')
  for n in range(1,101):
   response=adb('shell','am','broadcast','-a','com.example.widgethost.CLICK','--es','kind','Lock:Tasbih')
   if n in [1,32,33,34,65,66,98,99,100]:
    time.sleep(.6);check(tag+' actual count '+str(n),counts(pkg).get('count_'+wid)==str(n));shot(tag+'-count-'+str(n))
  adb('shell','am','force-stop',pkg);adb('shell','am','broadcast','-a','android.appwidget.action.APPWIDGET_UPDATE','-n',pkg+'/com.example.lockwidgets.LockWidgets$Tasbih','--include-stopped-packages','--eia','appWidgetIds',wid);time.sleep(2);check(tag+' count survives process restart',counts(pkg).get('count_'+wid)=='100');shot(tag+'-restarted');adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','operation','delete');time.sleep(2);check(tag+' deletion removes count','count_'+wid not in counts(pkg));(out/(tag+'-log.txt')).write_text(adb('logcat','-d'))
(out/'checks.json').write_text(json.dumps(checks,indent=2));print('checks',len(checks),'fails',sum(not x['passed'] for x in checks));assert all(x['passed'] for x in checks)
