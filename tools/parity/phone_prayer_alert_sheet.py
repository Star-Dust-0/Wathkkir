"""Jumu'a alert-sheet + pre-adhan alarm parity probe: original vs comparison, isolated emulator only.
No network disclosure, release, real-device data mutation, or host account changes.
Evidence per app: prayer screen, Friday alert sheet (iqama row? toggle states?), scheduled
alarms (dumpsys), then a real early-reminder fire test by moving the clock to the Asr
early-reminder minus ~90s and watching for the notification.
"""
import subprocess,time,pathlib,json,re,xml.etree.ElementTree as ET,datetime
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a,timeout=50):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=timeout);return (r.stdout+r.stderr).decode(errors='replace')
def check(n,ok,detail=''):
 checks.append(dict(name=n,passed=bool(ok),detail=str(detail)[:600]));(out/'checks.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2));print(n,ok,str(detail)[:300],flush=True);return bool(ok)
def shot(name):
 (out/(name+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=25))
def tree():
 s=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump')
 return next((x.split('=',1)[1] for x in s.splitlines() if x.startswith('INSTRUMENTATION_RESULT: hierarchy=')),'<hierarchy/>')
def find(desc):
 for n in ET.fromstring(tree()).iter('node'):
  if n.get('content-desc')==desc or n.get('text')==desc:return n
 return None
def center(n):
 x0,y0,x1,y1=map(int,re.findall(r'\d+',n.get('bounds','')));return (x0+x1)//2,(y0+y1)//2
def tap_node(n):
 x,y=center(n);adb('shell','input','tap',str(x),str(y));time.sleep(1.2);return True
def longpress_node(n):
 x,y=center(n);adb('shell','input','swipe',str(x),str(y),str(x),str(y),'1400');time.sleep(2);return True
def alarm_epochs(pkg,dump):
 eps=[]
 for line in dump.splitlines():
  if pkg in line and 'Alarm{' in line:
   eps += [int(x) for x in re.findall(r'\b(1[6-9]\d{11})\b',line)]
 return sorted(set(eps))
def dump_alarms(pkg,tag):
 s=adb('shell','dumpsys','alarm',timeout=60)
 (out/(tag+'-alarms.txt')).write_text(s)
 return alarm_epochs(pkg,s)
def tap_text(label):
 n=find(label)
 if n is not None: tap_node(n); return True
 return False
def launch(pkg):
 adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
 adb('shell','am','force-stop',pkg)
 adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity')
 time.sleep(8)
 tap_text('Close app')  # dismiss a launcher ANR dialog if one appeared (emulator flake)
 time.sleep(2)

adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device')
for a in ['original.apk','comparison.apk','widgethost.apk']:
 check('install '+a,'Success' in adb('install','-r','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','settings','put','global','hide_error_dialogs','1')
adb('shell','setprop','persist.sys.timezone','Africa/Cairo');time.sleep(1)
check('timezone',adb('shell','getprop','persist.sys.timezone').strip())

for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 adb('shell','pm','clear',pkg)
 for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',pkg,'android.permission.'+perm)
 adb('shell','cmd','uimode','night','no')
 folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
 x='<map><string name="language">ar</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><string name="theme_mode">light</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
 adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
 # Seed alert settings directly: Dhuhr and Asr bells ON + 10-min pre-adhan for each.
 pa='<map><boolean name="migrated_v1" value="true"/><boolean name="enabled_2" value="true"/><int name="offset_2" value="10"/><boolean name="enabled_3" value="true"/><int name="offset_3" value="10"/></map>'
 adb('shell',f"echo '{pa}' > {folder}/prayer_alerts.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/prayer_alerts.xml; chmod 660 {folder}/prayer_alerts.xml; restorecon -R {folder}")
 launch(pkg)
 shot(name+'-prayer')
 tr=tree();(out/(name+'-prayer.xml')).write_text(tr)
 times=[n.get('text') for n in ET.fromstring(tr).iter('node') if n.get('text') and re.fullmatch(r'\d{2}:\d{2}',n.get('text'))]
 check(name+' displayed times',len(times)>=5,str(times))
 eps=dump_alarms(pkg,name+'-launch')
 check(name+' alarms after launch',len(eps)>=2,str([datetime.datetime.fromtimestamp(e/1000,datetime.timezone(datetime.timedelta(hours=2))).strftime('%H:%M') for e in eps]))
 # the Asr pair: two alarms 600000ms apart, soonest pair in the future
 now=int(time.time()*1000)
 fut=[e for e in eps if e>now+30000]
 pair=None
 for i in range(len(fut)-1):
  if fut[i+1]-fut[i]==600000:pair=(fut[i],fut[i+1]);break
 check(name+' asr early+adhan pair',pair is not None,str(pair))
 # open the Jumu'a sheet (long-press the Dhuhr-row bell)
 b=find('bell_2')
 check(name+' bell_2 found',b is not None)
 if b is not None:
  longpress_node(b)
  tr2=tree();(out/(name+'-sheet.xml')).write_text(tr2)
  shot(name+'-sheet')
  check(name+' sheet title','الجمعة' if 'الجمعة' in tr2 else ('الظهر' if 'الظهر' in tr2 else 'none')!='none','الجمعة' if 'الجمعة' in tr2 else ('الظهر' if 'الظهر' in tr2 else 'none'))
  check(name+' sheet shows iqama row on Friday','تنبيه الإقامة' in tr2)
  check(name+' sheet shows pre-adhan row','تنبيه قبل الأذان' in tr2)
  adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1.5)
 # fire test: move the clock to (early - 90s) and wait for the early reminder to fire
 if pair:
  early,adhan=pair
  target=early-90000
  lt=datetime.datetime.fromtimestamp(target/1000,datetime.timezone(datetime.timedelta(hours=2)))
  adb('shell','date',lt.strftime('%m%d%H%M%Y.%S'));time.sleep(3)
  check(name+' clock set',adb('shell','date').strip())
  eps2=dump_alarms(pkg,name+'-after-clock-set')
  check(name+' alarms still armed after clock set',len(eps2)>=2,str(len(eps2)))
  time.sleep(105)
  notif=adb('shell','dumpsys','notification',timeout=60)
  (out/(name+'-notifications.txt')).write_text(notif)
  posted=pkg in notif
  check(name+' early notification posted',posted,'entries for pkg: %d'%notif.count(pkg))
  shot(name+'-after-early')
  dump_alarms(pkg,name+'-after-early')
  adb('shell','date','111309502026.00')  # restore roughly-real time
print('done',flush=True)
