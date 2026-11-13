"""Weekday alert-sheet check for the current build (comparison.apk), isolated emulator.
Sets the emulator clock to the coming SATURDAY 11:00 local BEFORE the first app launch,
then opens the Dhuhr alert sheet: the iqama row must be present (Friday-only hiding must
not leak into weekdays) and the Dhuhr iqama alarm must be armed at adhan+20min.
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
def tap_node(n):
 x0,y0,x1,y1=map(int,re.findall(r'\d+',n.get('bounds','')))
 adb('shell','input','tap',str((x0+x1)//2),str((y0+y1)//2));time.sleep(1.2);return True
def longpress_node(n):
 x0,y0,x1,y1=map(int,re.findall(r'\d+',n.get('bounds','')))
 adb('shell','input','swipe',str(x0),str(y0),str(x0),str(y0),'1400');time.sleep(2);return True
def tap_text(label):
 n=find(label)
 if n is not None: tap_node(n); return True
 return False
def launch(pkg):
 adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
 adb('shell','am','force-stop',pkg)
 adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity')
 time.sleep(8)
 tap_text('Close app')
 time.sleep(2)
EET=datetime.timezone(datetime.timedelta(hours=2))
def set_clock(epoch_ms):
 lt=datetime.datetime.fromtimestamp(epoch_ms/1000,EET)
 for _ in range(3):
  adb('shell','date',lt.strftime('%m%d%H%M%Y.%S'));time.sleep(4)
  now=int(adb('shell','date','+%s').strip() or '0')*1000
  if abs(now-epoch_ms)<120000:return lt.strftime('%Y-%m-%d %H:%M:%S')
 return 'FAILED want '+lt.strftime('%Y-%m-%d %H:%M:%S')
def alarm_epochs(pkg,dump):
 eps=[]
 for line in dump.splitlines():
  if pkg in line and 'Alarm{' in line:
   eps += [int(x) for x in re.findall(r'\b(1[6-9]\d{11})\b',line)]
 return sorted(set(eps))

PKG='com.wathakker.app.compare'
adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device')
for a in ['comparison.apk','widgethost.apk']:
 check('install '+a,'Success' in adb('install','-r','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','settings','put','global','hide_error_dialogs','1')
adb('shell','setprop','persist.sys.timezone','Africa/Cairo');time.sleep(1)
adb('shell','pm','clear',PKG)
for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',PKG,'android.permission.'+perm)
adb('shell','cmd','uimode','night','no')
folder=f'/data/data/{PKG}/shared_prefs';adb('shell','mkdir','-p',folder)
x='<map><string name="language">ar</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><string name="theme_mode">light</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{PKG}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
pa='<map><boolean name="migrated_v1" value="true"/><boolean name="enabled_2" value="true"/><int name="offset_2" value="10"/><boolean name="iqama_2" value="true"/></map>'
adb('shell',f"echo '{pa}' > {folder}/prayer_alerts.xml; chown $(stat -c %u:%g /data/data/{PKG}) {folder}/prayer_alerts.xml; chmod 660 {folder}/prayer_alerts.xml; restorecon -R {folder}")

# Coming Saturday 11:00 local (today is Friday; +1 day), set BEFORE first launch.
sat=datetime.datetime.now(EET).replace(hour=11,minute=0,second=0,microsecond=0)+datetime.timedelta(days=1)
r=set_clock(int(sat.timestamp()*1000))
check('clock reads Saturday 11:00',r.startswith('2026-11-14'),r)
launch(PKG)
shot('comparison-weekday-prayer')
eps=alarm_epochs(PKG,adb('shell','dumpsys','alarm',timeout=60))
(out/'comparison-weekday-alarms.txt').write_text(adb('shell','dumpsys','alarm',timeout=60))
emu_now=int(adb('shell','date','+%s').strip())*1000
fut=[e for e in eps if e>emu_now+30000]
pair=None
for i in range(len(fut)-1):
 if fut[i+1]-fut[i]==600000:pair=(fut[i],fut[i+1]);break
check('weekday Dhuhr early+adhan pair',pair is not None,str(pair))
if pair:
 check('weekday Dhuhr iqama alarm armed',any(abs(e-(pair[1]+20*60000))<61000 for e in eps),'expected adhan+20min')
b=find('bell_2')
check('bell_2 found',b is not None)
if b is not None:
 longpress_node(b)
 tr=tree();(out/'comparison-weekday-sheet.xml').write_text(tr)
 shot('comparison-weekday-sheet')
 check('weekday sheet title is Dhuhr','الظهر' in tr,'الظهر' if 'الظهر' in tr else ('الجمعة' if 'الجمعة' in tr else tr[:150]))
 check('weekday sheet shows iqama row','تنبيه الإقامة' in tr)
print('done',flush=True)
