"""Original/recovered stateful phone UI audit in an isolated emulator only.
No network disclosure, release, real-device data mutation, or host account changes.
Evidence, including failed setup, is saved per checkpoint. Counts are not parity.
"""
import subprocess,time,pathlib,json,re,sqlite3,xml.etree.ElementTree as ET
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[];states={};databases={}
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=50);return (r.stdout+r.stderr).decode(errors='replace')
def check(n,ok,detail=''):
 checks.append(dict(name=slug+' '+n,passed=bool(ok),detail=detail));(out/'checks.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2));print(slug,n,ok,detail,flush=True);return bool(ok)
def tree():
 s=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump');return next((x.split('=',1)[1] for x in s.splitlines() if x.startswith('INSTRUMENTATION_RESULT: hierarchy=')),'<hierarchy/>')
def shot(n):
 n=slug+'-'+n;x=tree();(out/(n+'.xml')).write_text(x);(out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=25));states[n]=[(z.get('text',''),z.get('content-desc',''),z.get('checked',''),z.get('enabled','')) for z in ET.fromstring(x).iter('node') if z.get('text') or z.get('content-desc')];(out/'states.json').write_text(json.dumps(states,ensure_ascii=False,indent=2))
def rect(n):return list(map(int,re.findall(r'\d+',n.get('bounds',''))))
def touch(n):
 b=rect(n)
 if len(b)!=4 or b[0]>=b[2] or b[1]>=b[3] or b[1]<70 or b[3]>2340:return False
 adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(.7);return True
def tap(label):
 root=ET.fromstring(tree());parents={c:p for p in root.iter() for c in p}
 for n in root.iter('node'):
  if label not in [n.get('text'),n.get('content-desc')]:continue
  t=n
  while t.get('clickable')!='true' and t in parents:t=parents[t]
  if t.get('clickable')=='true' and t.get('enabled')=='true' and touch(n):return True
 return False
def swipe(up=True):adb('shell','input','swipe','540','1760' if up else '680','540','680' if up else '1760','350');time.sleep(.6)
def choose(label):
 for _ in range(5):
  if tap(label):return True
  swipe()
 return False
def launch():
 adb('shell','am','force-stop','com.google.android.apps.nexuslauncher');tap('Close app');adb('shell','am','force-stop',pkg);adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(3)
 if tap('Close app'):
  adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(2)
def azkar():
 launch();return tap('Azkar' if lang=='en' else 'التذكيرات')
def route(label):
 # Wait out menu transitions and verify destination content, not the clicked label.
 for attempt in range(3):
  launch();time.sleep(1.5)
  # MainActivity may automatically ask notification permission on startup.
  # Preserve that prompt as evidence, deny it if present, then route to Fajr.
  if any('permissioncontroller' in x and 'mCurrentFocus=' in x for x in adb('shell','dumpsys','window').splitlines()):
   shot('startup-notification-request-'+str(attempt))
   deny=tap('Don’t allow') or tap("Don't allow")
   check('startup notification deny '+str(attempt),deny);time.sleep(1)
  if not tap('Menu' if lang=='en' else 'القائمة'):
   shot('route-menu-miss-'+str(attempt));continue
  time.sleep(2)
  if not tap(label):
   shot('route-label-miss-'+str(attempt));continue
  time.sleep(2)
  texts=[n.get('text','') for n in ET.fromstring(tree()).iter('node')]
  expected=['Alarm on','Alarm off','المنبه مفعّل','المنبه معطّل']
  if any(x in texts for x in expected):return True
  shot('route-destination-miss-'+str(attempt))
 return False
def field(index,value):
 nodes=[n for n in ET.fromstring(tree()).iter('node') if n.get('class')=='android.widget.EditText']
 if index>=len(nodes) or not touch(nodes[index]):return False
 adb('shell','input','keyevent','KEYCODE_MOVE_END');adb('shell','input','keyevent',*(['KEYCODE_DEL']*max(1,len(nodes[index].get('text','')))));adb('shell','input','text',value);time.sleep(.5);return True
def hide_keyboard():
 s=adb('shell','dumpsys','input_method')
 if 'mInputShown=true' in s or 'mIsInputViewShown=true' in s:adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(.5)
def prefs():
 x=adb('shell','cat',f'/data/data/{pkg}/shared_prefs/app_prefs.xml')
 try:return {n.get('name'):n.get('value',n.text) for n in ET.fromstring(x).iter() if n.get('name')}
 except:return {'parse_error':x}
def db(n):
 # Closing app checkpoints WAL before pull, and makes restart persistence explicit.
 adb('shell','am','force-stop',pkg);folder=out/(slug+'-'+n+'-db');folder.mkdir(exist_ok=True);adb('pull',f'/data/data/{pkg}/databases/.',str(folder));p=folder/'wazakkir_database'
 if not p.exists():check(n+' database available',False);return {}
 try:
  c=sqlite3.connect(p);d={t:c.execute('SELECT * FROM '+t+' ORDER BY 1').fetchall() for t in ['dhikr_table','tag_table','dhikr_tag_cross_ref','dhikr_history']};c.close();databases[slug+'-'+n]=d;(out/'databases.json').write_text(json.dumps(databases,ensure_ascii=False,indent=2));return d
 except Exception as e:check(n+' database readable',False,str(e));return {}
adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device');slug='setup'
for a in ['original.apk','comparison.apk','widgethost.apk']:check('install '+a,'Success' in adb('install','-r','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','settings','put','global','hide_error_dialogs','1')
adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0')
locale_labels={'fr': {'Menu': 'Menu', 'FajrAlarm': 'Alarme de Fajr', 'Snooze': 'Répéter', 'StopAlarm': "Arrêter l'alarme"}, 'de': {'Menu': 'Menü', 'FajrAlarm': 'Fajr-Wecker', 'Snooze': 'Schlummern', 'StopAlarm': 'Alarm stoppen'}, 'es': {'Menu': 'Menú', 'FajrAlarm': 'Alarma de Fajr', 'Snooze': 'Posponer', 'StopAlarm': 'Detener alarma'}, 'it': {'Menu': 'Menu', 'FajrAlarm': 'Sveglia di Fajr', 'Snooze': 'Posticipa', 'StopAlarm': 'Ferma sveglia'}, 'tr': {'Menu': 'Menü', 'FajrAlarm': 'Sabah namazı alarmı', 'Snooze': 'Ertele', 'StopAlarm': 'Alarmı durdur'}, 'ur': {'Menu': 'مینو', 'FajrAlarm': 'فجر کا الارم', 'Snooze': 'غفوہ', 'StopAlarm': 'الارم بند کریں'}, 'id': {'Menu': 'Menu', 'FajrAlarm': 'Alarm Subuh', 'Snooze': 'Tunda', 'StopAlarm': 'Hentikan alarm'}, 'ru': {'Menu': 'Меню', 'FajrAlarm': 'Будильник на фаджр', 'Snooze': 'Отложить', 'StopAlarm': 'Остановить будильник'}, 'pt': {'Menu': 'Menu', 'FajrAlarm': 'Alarme do Fajr', 'Snooze': 'Soneca', 'StopAlarm': 'Parar alarme'}}
notices={}
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang,labels in locale_labels.items():
  slug=name+'-'+lang
  for other in ['com.wathakker.app','com.wathakker.app.compare']:adb('shell','am','force-stop',other)
  adb('shell','pm','clear',pkg)
  for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',pkg,'android.permission.'+perm)
  adb('shell','cmd','uimode','night','yes' if lang=='ur' else 'no');folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
  x=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
  adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
  # Explicit initial fixture only. Delivery itself is real AlarmManager, not broadcast injection.
  value='<map><int name="snooze_count" value="0"/><int name="snooze_minutes" value="1"/><boolean name="is_alarm_enabled" value="true"/><int name="alarm_volume" value="0"/><boolean name="use_current_location" value="false"/></map>'
  adb('shell',f"echo '{value}' > {folder}/fajr_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/fajr_prefs.xml; chmod 660 {folder}/fajr_prefs.xml; restorecon -R {folder}")
  launch();time.sleep(6);adb('shell','date','103112002026.00')
  # Non-AR Menu labels localize, Fajr destination row itself uses EN unless AR.
  opened=False
  for attempt in range(3):
   launch();time.sleep(1)
   if tap(labels['Menu']):
    time.sleep(2)
    if tap(labels['FajrAlarm']):
     time.sleep(2)
     if any(n.get('text') in ['Alarm on','Alarm off'] for n in ET.fromstring(tree()).iter('node')):opened=True;break
   shot('route-miss-'+str(attempt))
  if not check('verified Fajr settings destination',opened):continue
  clicked=False
  for _ in range(8):
   if tap('Test the alarm now'):clicked=True;break
   swipe()
  if not check('actual Test',clicked):continue
  time.sleep(1);shot('test-alarm')
  active=lambda:any('FajrAlarmActivity' in z and pkg in z for z in adb('shell','dumpsys','activity','activities').splitlines() if 'topResumedActivity=' in z)
  check('Test alarm foreground',active());check('actual localized Snooze',tap(labels['Snooze']));time.sleep(2)
  alarms=adb('shell','dumpsys','alarm');(out/(slug+'-snooze-alarms.txt')).write_text(alarms)
  check('snooze pending intent','ACTION_SNOOZE_TRIGGER' in alarms)
  time.sleep(60);check('actual receiver delivery foreground',active());shot('receiver-alarm')
  dump=adb('shell','dumpsys','notification','--noredact');(out/(slug+'-notifications.txt')).write_text(dump)
  # Both other app processes force-stopped and package cleared per locale. Extract this app's active record.
  records=re.split(r'(?=NotificationRecord\()',dump)
  record=next((z for z in records if 'pkg='+pkg+' ' in z and 'id=88001 ' in z),'')
  title=re.search(r'android.title=String \((.*)\)',record);body=re.search(r'android.text=String \((.*)\)',record)
  notices[slug]={'title':title.group(1) if title else None,'body':body.group(1) if body else None}
  (out/'native-notices.json').write_text(json.dumps(notices,ensure_ascii=False,indent=2));check('native title/body extracted',bool(title and body),str(notices[slug]))
  adb('shell','cmd','statusbar','expand-notifications');time.sleep(1);shot('native-notification-shade');adb('shell','cmd','statusbar','collapse');time.sleep(1)
  check('actual localized Dismiss',tap(labels['StopAlarm']));time.sleep(1);check('dismissed foreground',not active())
  (out/(slug+'-log.txt')).write_text(adb('logcat','-d'));adb('shell','am','force-stop',pkg)
pairs=[]
for n,a in states.items():
 other='comparison-'+n[len('original-'):]
 if n.startswith('original-') and other in states:pairs.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'semantic-comparison.json').write_text(json.dumps(pairs,ensure_ascii=False,indent=2))
for lang in locale_labels:
 slug='paired-'+lang;a=notices.get('original-'+lang);b=notices.get('comparison-'+lang);check('native receiver localized title/body exact pair',bool(a and b and a==b),str({'original':a,'comparison':b}))
print('checks',len(checks),'failures',sum(not x['passed'] for x in checks));assert all(x['passed'] for x in checks),'See checks.json'
