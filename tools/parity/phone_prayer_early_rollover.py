"""Pre-adhan reminder day-rollover repro on the CURRENT build only (comparison.apk).
Question: does the 10-min early reminder post on the SECOND day it is scheduled?
Theory under test: the early reminder's PendingIntent is created once with
FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE; Android does not refresh extras on an immutable
PendingIntent, so EXTRA_AT stays frozen at day 1's adhan. On day 2 the receiver's
'at <= now' guard then silently suppresses the notification while the alarm itself fires.

Signal: after the early-alarm time passes on day 2 -
  early alarm gone from dumpsys + notification present  => fired + posted (no bug)
  early alarm gone from dumpsys + no notification       => fired + suppressed (bug confirmed)
  early alarm still armed                                => did not fire (inconclusive)
"""
import subprocess,time,pathlib,json,re,datetime
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a,timeout=50):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=timeout);return (r.stdout+r.stderr).decode(errors='replace')
def check(n,ok,detail=''):
 checks.append(dict(name=n,passed=bool(ok),detail=str(detail)[:600]));(out/'checks.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2));print(n,ok,str(detail)[:300],flush=True);return bool(ok)
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
def set_clock(epoch_ms):
 lt=datetime.datetime.fromtimestamp(epoch_ms/1000,datetime.timezone(datetime.timedelta(hours=2)))
 adb('shell','date',lt.strftime('%m%d%H%M%Y.%S'));time.sleep(6)
 return lt.strftime('%Y-%m-%d %H:%M:%S')
import xml.etree.ElementTree as ET
def tree():
 s=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump')
 return next((x.split('=',1)[1] for x in s.splitlines() if x.startswith('INSTRUMENTATION_RESULT: hierarchy=')),'<hierarchy/>')
def tap_text(label):
 for n in ET.fromstring(tree()).iter('node'):
  if n.get('text')==label:
   x0,y0,x1,y1=map(int,re.findall(r'\d+',n.get('bounds','')))
   adb('shell','input','tap',str((x0+x1)//2),str((y0+y1)//2));time.sleep(2);return True
 return False
def launch(pkg):
 adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
 adb('shell','am','force-stop',pkg)
 adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity')
 time.sleep(8)
 tap_text('Close app')  # dismiss a launcher ANR dialog if one appeared (emulator flake)
 time.sleep(2)

PKG='com.wathakker.app.compare'
adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device')
check('install','Success' in adb('install','-r','-g','comparison.apk'))
check('install widgethost','Success' in adb('install','-r','-g','widgethost.apk'))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','settings','put','global','hide_error_dialogs','1')
adb('shell','setprop','persist.sys.timezone','Africa/Cairo');time.sleep(1)
adb('shell','pm','clear',PKG)
for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',PKG,'android.permission.'+perm)
adb('shell','cmd','uimode','night','no')
folder=f'/data/data/{PKG}/shared_prefs';adb('shell','mkdir','-p',folder)
x='<map><string name="language">ar</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><string name="theme_mode">light</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{PKG}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
pa='<map><boolean name="migrated_v1" value="true"/><boolean name="enabled_3" value="true"/><int name="offset_3" value="10"/></map>'
adb('shell',f"echo '{pa}' > {folder}/prayer_alerts.xml; chown $(stat -c %u:%g /data/data/{PKG}) {folder}/prayer_alerts.xml; chmod 660 {folder}/prayer_alerts.xml; restorecon -R {folder}")

# Day 1: launch -> scheduleAll creates the early-reminder PendingIntent (first creation freezes its extras).
launch(PKG)
eps=dump_alarms(PKG,'day1-after-launch')
now=int(time.time()*1000)
fut=[e for e in eps if e>now+30000]
pair=None
for i in range(len(fut)-1):
 if fut[i+1]-fut[i]==600000:pair=(fut[i],fut[i+1]);break
check('day1 early+adhan pair armed',pair is not None,str(pair))
if pair:
 early1,adhan1=pair
 # Jump to day 2, 25 min before Asr. TIME_SET + relaunch reschedule for day 2.
 # If the immutable-PI theory holds, the alarm is re-armed at day2 Asr-10min but
 # the delivered EXTRA_AT stays day1's adhan (a past moment on day 2).
 adhan2=adhan1+86400000
 check('clock to day2 -25min',set_clock(adhan2-1500000))
 launch(PKG)
 eps2=dump_alarms(PKG,'day2-after-relaunch')
 now2=int(time.time()*1000)
 fut2=[e for e in eps2 if e>now2+30000]
 pair2=None
 for i in range(len(fut2)-1):
  if fut2[i+1]-fut2[i]==600000:pair2=(fut2[i],fut2[i+1]);break
 check('day2 early+adhan pair armed',pair2 is not None,str(pair2))
 if pair2:
  early2,adhan2b=pair2
  check('day2 early at adhan-10min',abs(early2-(adhan2-600000))<61000,'early=%d expected~%d'%(early2,adhan2-600000))
  # Now land 90s before the early reminder and wait for it.
  check('clock to day2 early-90s',set_clock(early2-90000))
  eps3=dump_alarms(PKG,'day2-before-fire')
  armed_before=[e for e in eps3 if abs(e-early2)<61000]
  check('early alarm armed just before fire',len(armed_before)>=1,str(armed_before))
  time.sleep(105)
  notif=adb('shell','dumpsys','notification',timeout=60)
  (out/'day2-notifications.txt').write_text(notif)
  posted=PKG in notif
  check('early notification posted on day2',posted,'entries for pkg: %d'%notif.count(PKG))
  eps4=dump_alarms(PKG,'day2-after-fire')
  still=[e for e in eps4 if abs(e-early2)<61000]
  check('early alarm consumed (fired, not re-armed)',len(still)==0,str(still))
print('done',flush=True)
