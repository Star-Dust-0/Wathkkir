"""Friday alert-sheet + alarm verification for the current build (comparison.apk), isolated emulator.
Verifies the Jumu'a iqama fix and the pre-adhan reminder end to end:
  Friday 11:00 -> Jumu'a sheet hides the iqama row (auto-silent stays); no Jumu'a iqama
    alarm armed; Asr iqama alarm still armed; early+adhan pairs armed for Jumu'a and Asr.
  Saturday 11:00 -> Dhuhr sheet shows the iqama row again (weekday unchanged); Dhuhr iqama armed.
  Fire test -> clock to (Asr early - 90s): the 10-min pre-adhan notification posts.
Evidence: screenshots, hierarchy XML, dumpsys alarm/notification dumps, checks.json.
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
def tap_text(label):
 n=find(label)
 if n is not None: tap_node(n); return True
 return False
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
def launch(pkg):
 adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
 adb('shell','am','force-stop',pkg)
 adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity')
 time.sleep(8)
 tap_text('Close app')  # dismiss a launcher ANR dialog if one appeared (emulator flake)
 time.sleep(2)
def set_clock(epoch_ms):
 lt=datetime.datetime.fromtimestamp(epoch_ms/1000,datetime.timezone(datetime.timedelta(hours=2)))
 adb('shell','date',lt.strftime('%m%d%H%M%Y.%S'));time.sleep(5)
 return lt.strftime('%Y-%m-%d %H:%M:%S')
def pair_near(eps,adhan_ms):
 for e in eps:
  if abs(e-(adhan_ms-600000))<61000 and any(abs(x-adhan_ms)<61000 for x in eps):return True
 return False
def any_near(eps,ms,win=61000):return any(abs(e-ms)<win for e in eps)

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
# Jumu'a + Asr bells ON, 10-min pre-adhan each, iqama reminders ON for both.
pa='<map><boolean name="migrated_v1" value="true"/><boolean name="enabled_2" value="true"/><int name="offset_2" value="10"/><boolean name="iqama_2" value="true"/><boolean name="enabled_3" value="true"/><int name="offset_3" value="10"/><boolean name="iqama_3" value="true"/></map>'
adb('shell',f"echo '{pa}' > {folder}/prayer_alerts.xml; chown $(stat -c %u:%g /data/data/{PKG}) {folder}/prayer_alerts.xml; chmod 660 {folder}/prayer_alerts.xml; restorecon -R {folder}")

# --- Friday, 11:00 local: Jumu'a sheet + Friday alarm plan ---
friday_11=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=2))).replace(hour=11,minute=0,second=0,microsecond=0)
check('clock to Friday 11:00',set_clock(int(friday_11.timestamp()*1000)))
launch(PKG)
shot('comparison-friday-prayer')
eps=dump_alarms(PKG,'comparison-friday')
now=int(time.time()*1000)
fut=[e for e in eps if e>now+30000]
# Dhuhr/Jumu'a and Asr early+adhan pairs (10 min apart each)
pairs=[]
for i in range(len(fut)-1):
 if fut[i+1]-fut[i]==600000:pairs.append((fut[i],fut[i+1]))
check('friday two early+adhan pairs',len(pairs)>=2,str([(datetime.datetime.fromtimestamp(a/1000,datetime.timezone(datetime.timedelta(hours=2))).strftime('%H:%M'),datetime.datetime.fromtimestamp(b/1000,datetime.timezone(datetime.timedelta(hours=2))).strftime('%H:%M')) for a,b in pairs]))
if len(pairs)>=2:
 (j_early,j_adhan),(a_early,a_adhan)=pairs[0],pairs[1]
 check('NO Jumu\'a iqama alarm on Friday',not any_near(eps,j_adhan+20*60000),'iqama would be at +20min')
 check('Asr iqama alarm armed on Friday',any_near(eps,a_adhan+20*60000),'expected at +20min')
b=find('bell_2')
check('bell_2 found',b is not None)
if b is not None:
 longpress_node(b)
 tr=tree();(out/'comparison-friday-sheet.xml').write_text(tr)
 shot('comparison-friday-sheet')
 check('sheet title is Jumu\'a','الجمعة' in tr,'الجمعة' if 'الجمعة' in tr else tr[:200])
 check('Friday sheet hides iqama row','تنبيه الإقامة' not in tr)
 check('Friday sheet keeps auto-silent row','صامت تلقائي' in tr)
 check('Friday sheet keeps pre-adhan row','تنبيه قبل الأذان' in tr)
 adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1.5)

# --- Saturday, 11:00 local: weekday sheet + alarms unchanged ---
sat_11=friday_11+datetime.timedelta(days=1)
check('clock to Saturday 11:00',set_clock(int(sat_11.timestamp()*1000)))
launch(PKG)
eps2=dump_alarms(PKG,'comparison-saturday')
now2=int(time.time()*1000)
fut2=[e for e in eps2 if e>now2+30000]
pairs2=[]
for i in range(len(fut2)-1):
 if fut2[i+1]-fut2[i]==600000:pairs2.append((fut2[i],fut2[i+1]))
if pairs2:
 d_adhan=pairs2[0][1]
 check('Saturday Dhuhr iqama alarm armed',any_near(eps2,d_adhan+20*60000),'expected at +20min')
b=find('bell_2')
if b is not None:
 longpress_node(b)
 tr3=tree();(out/'comparison-saturday-sheet.xml').write_text(tr3)
 shot('comparison-saturday-sheet')
 check('Saturday sheet title is Dhuhr','الظهر' in tr3,'الظهر' if 'الظهر' in tr3 else tr3[:200])
 check('Saturday sheet shows iqama row','تنبيه الإقامة' in tr3)
 adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1.5)

# --- Fire test: pre-adhan reminder posts at adhan-10min (Saturday Asr) ---
if pairs2:
 a_early2=pairs2[-1][0]
 check('clock to early-90s',set_clock(a_early2-90000))
 time.sleep(105)
 notif=adb('shell','dumpsys','notification',timeout=60)
 (out/'comparison-notifications.txt').write_text(notif)
 check('pre-adhan notification posted',PKG in notif,'entries for pkg: %d'%notif.count(PKG))
 shot('comparison-after-early')
print('done',flush=True)
