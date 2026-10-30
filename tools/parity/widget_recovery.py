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
 for lang in ['en','ar']:
  prefs(pkg,lang)
  for mode in ['recover-homeA','recover-homeB','recover-dates','recover-lockA','recover-lockB']:
   adb('shell','date','103010002026.00');adb('logcat','-c');host(pkg,mode);time.sleep(13);tag=name+'-'+lang+'-'+mode;shot(tag)
   log=adb('logcat','-d');(out/(tag+'-log.txt')).write_text(log)
   expected=4 if mode!='recover-dates' else 2
   binds=re.findall(r'WidgetRecoveryAudit.*bound kind=([^ ]+).*ok=true',log)
   check(tag+' all bound',len(set(binds))==expected,','.join(sorted(set(binds))))
   pixels=re.findall(r'WidgetPix.*?([^ ]+) w=\d+ h=\d+ std=([\d.]+)',log)
   check(tag+' nonblank',len(set(k for k,v in pixels if float(v)>2))==expected,str(pixels))
   alarm=adb('shell','dumpsys','alarm');(out/(tag+'-alarm.txt')).write_text(alarm)
   check(tag+' nonwakeup pulse',bool(re.search(r'(?s)RTC #[^\n]*\n(?:(?!RTC(?:_WAKEUP)? #).){0,1200}'+re.escape(pkg)+r'\.lockwidgets\.TICK',alarm)), 'see alarm dump')
   check(tag+' wakeup boundary',bool(re.search(r'(?s)RTC_WAKEUP #[^\n]*\n(?:(?!RTC(?:_WAKEUP)? #).){0,1200}'+re.escape(pkg)+r'\.lockwidgets\.BOUNDARY',alarm)), 'see alarm dump')
 # Tap tests use actual RemoteViews click handlers, no direct receiver invocation.
 prefs(pkg,'en');adb('logcat','-c');host(pkg,'recover-homeB');time.sleep(8)
 log=adb('logcat','-d');ids={k:int(i) for k,i in re.findall(r'WidgetRecoveryAudit.*bound kind=([^ ]+) id=(\d+) ok=true',log)}
 # persist choices using freshly allocated widget IDs, then trigger a legitimate provider update.
 for choice in ['none','refresh']:
  prefs(pkg,'en',''.join(f'<string name="widget_tap_{i}">{choice}</string>' for i in ids.values()))
  for k,i in ids.items():adb('shell','am','broadcast','-a','android.appwidget.action.APPWIDGET_UPDATE','-n',pkg+'/com.example.widget.'+k+'WidgetProvider','--eia','appWidgetIds',str(i))
  time.sleep(6);adb('logcat','-c')
  # Pill is the third widget, after schedule + DayPrayer. Center inside its image at 420dpi.
  adb('shell','am','broadcast','-a','com.example.widgethost.CLICK','--es','kind','Pill');time.sleep(2);shot(name+'-tap-'+choice)
  log=adb('logcat','-d');(out/(name+'-tap-'+choice+'.txt')).write_text(log)
  check(name+' '+choice+' migrated/native refresh','WathakkerWidgetRefresh' in log and 'text=' in log)
 adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','operation','delete');time.sleep(5)
 alarm=adb('shell','dumpsys','alarm');(out/(name+'-deleted-alarm.txt')).write_text(alarm)
 # Only active alarm entries, not historical alarm statistics.
 active=alarm.split('Past-due non-wakeup alarms:')[0].split('Alarm Stats:')[0]
 check(name+' delete cancels both',not re.search(r'\*walarm\*:'+re.escape(pkg)+r'\.lockwidgets\.(TICK|BOUNDARY)',active))
(out/'checks.json').write_text(json.dumps(checks,indent=2));print(json.dumps(checks,indent=2));assert all(x['passed'] for x in checks)
