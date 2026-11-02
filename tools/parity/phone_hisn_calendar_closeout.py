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
 launch();return tap('Menu' if lang=='en' else 'القائمة') and tap(label)
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
  c=sqlite3.connect(p);d={t:c.execute('SELECT * FROM '+t+' ORDER BY 1').fetchall() for t in ['dhikr_table','tag_table','dhikr_tag_cross_ref','dhikr_history','hisn_favorite','hisn_read']};c.close();databases[slug+'-'+n]=d;(out/'databases.json').write_text(json.dumps(databases,ensure_ascii=False,indent=2));return d
 except Exception as e:check(n+' database readable',False,str(e));return {}
adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device');slug='setup'
for a in ['original.apk','comparison.apk','widgethost.apk']:check('install '+a,'Success' in adb('install','-r','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','settings','put','global','hide_error_dialogs','1')
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang in ['en','ar']:
  slug=name+'-'+lang;adb('shell','pm','clear',pkg)
  for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',pkg,'android.permission.'+perm)
  adb('shell','cmd','uimode','night','yes' if lang=='ar' else 'no');folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
  x=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
  adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
  launch();time.sleep(8)
  add='Add dhikr' if lang=='en' else 'إضافة ذكر';save='Save' if lang=='en' else 'حفظ';cancel='Cancel' if lang=='en' else 'إلغاء';options='Options' if lang=='en' else 'خيارات';edit='Edit dhikr and tags' if lang=='en' else 'تعديل الذكر والتصنيفات';delete='Delete' if lang=='en' else 'حذف';addtime='Add reminder time' if lang=='en' else 'إضافة وقت تنبيه';timeslabel='Reminder times' if lang=='en' else 'أوقات التنبيه';repeat='Repeat reminder' if lang=='en' else 'تكرار التنبيه';newtitle='New dhikr' if lang=='en' else 'ذكر جديد';edittitle='Edit dhikr' if lang=='en' else 'تعديل الذكر'
  def named(label):return any(n.get('text')==label for n in ET.fromstring(tree()).iter('node'))
  def enabled(label):
   root=ET.fromstring(tree());parents={c:p for p in root.iter() for c in p}
   for n in root.iter('node'):
    if n.get('text')!=label:continue
    while n.get('clickable')!='true' and n in parents:n=parents[n]
    return n.get('enabled')=='true'
   return None
  # B2: positive search must include a chapter that is not a favorite.
  check('Hisn tab',tap('Hisn' if lang=='en' else 'حصن المسلم'));time.sleep(5);check('Favorites filter',tap('Favorites' if lang=='en' else 'المفضلة'))
  query='الثوب';encoded=__import__('base64').b64encode(query.encode()).decode()
  result=adb('shell','am','instrument','-w','-e','base64',encoded,'com.example.widgethost.TextInput');check('actual Arabic query input','set=true' in result);hide_keyboard();time.sleep(1);shot('hisn-nonfavorite-positive');check('nonfavorite chapter returned',named('دُعَاءُ لُبْسِ الثَّوبِ'));check('nonfavorite chapter open',tap('دُعَاءُ لُبْسِ الثَّوبِ'));shot('hisn-nonfavorite-open');check('appbar Back actual',tap('Back' if lang=='en' else 'رجوع'));check('query retained after Back',named(query));shot('hisn-query-return')
  # Clear by actual accessibility text action, then list-star callback on known chapter.
  check('clear actual query','set=true' in adb('shell','am','instrument','-w','-e','base64','','com.example.widgethost.TextInput'));hide_keyboard();check('All filter',tap('All' if lang=='en' else 'الكل'))
  root=ET.fromstring(tree());parents={c:p for p in root.iter() for c in p};title=next((n for n in root.iter('node') if n.get('text')=='دُعَاءُ لُبْسِ الثَّوبِ'),None)
  def liststar():
   if title is None:return False
   n=parents.get(title)
   while n is not None:
    stars=[c for c in n.iter('node') if c.get('content-desc')==('Favorites' if lang=='en' else 'المفضلة')]
    if stars:return touch(stars[0])
    n=parents.get(n)
   return False
  check('actual chapter list-star',liststar());shot('hisn-list-star');d=db('hisn-list-star');check('list-star persisted chapter4',any(v[1]==4 and v[2]==0 for v in d.get('hisn_favorite',[])))
  # Year boundary is fixed by device wallclock, not current real-world date.
  adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo');adb('shell','date','123112002026.00')
  launch();check('Calendar tab',tap('Calendar' if lang=='en' else 'التقويم'));time.sleep(1);shot('calendar-december')
  def monthmove(nextmonth):
   root=ET.fromstring(tree());opts=[]
   for n in root.iter('node'):
    b=rect(n)
    if n.get('clickable')=='true' and len(b)==4 and 100<=b[2]-b[0]<=140 and 100<=b[3]-b[1]<=140 and 680<b[1]<1000:opts.append(n)
   if len(opts)!=2:return False
   opts.sort(key=lambda n:rect(n)[0]);right=nextmonth==(lang=='en');return touch(opts[-1 if right else 0])
  check('prev month actual',monthmove(False));check('November heading',any(('November 2026' if lang=='en' else 'نوفمبر 2026') in n.get('text','') for n in ET.fromstring(tree()).iter('node')));shot('calendar-november');check('next month actual back',monthmove(True));check('next year actual',monthmove(True));check('January2027 heading',any(('January 2027' if lang=='en' else 'يناير 2027') in n.get('text','') for n in ET.fromstring(tree()).iter('node')));shot('calendar-january')
  root=ET.fromstring(tree());cells=[n for n in root.iter('node') if n.get('clickable')=='true' and len(rect(n))==4 and 110<=rect(n)[2]-rect(n)[0]<=145 and 160<=rect(n)[3]-rect(n)[1]<=200];chosen=next((n for n in cells if any(c.get('text')=='5' for c in n.iter('node'))),None);check('actual day5 selected',chosen is not None and touch(chosen));shot('calendar-day5');swipe();shot('calendar-day5-details')
  launch();tap('Calendar' if lang=='en' else 'التقويم');check('Events tab',tap('Events' if lang=='en' else 'المناسبات'));shot('events-initial')
  def eventprefs(stage):
   raw=adb('shell','cat',f'/data/data/{pkg}/shared_prefs/fajr_prefs.xml');(out/(slug+'-'+stage+'-fajr.xml')).write_text(raw)
   try:return sorted(c.text for n in ET.fromstring(raw).iter('set') if n.get('name')=='event_reminder_ids' for c in n)
   except:return []
  def eventalarms(stage):
   raw=adb('shell','dumpsys','alarm');(out/(slug+'-'+stage+'-alarm.txt')).write_text(raw);blocks=re.split(r'\n(?=    (?:RTC_WAKEUP|RTC|ELAPSED_WAKEUP|ELAPSED) #\d+:)',raw);v=[b for b in blocks[1:] if re.match(r'    \S+ #\d+: Alarm\{[^\n]* '+re.escape(pkg)+r'\}',b) and 'tag=*walarm*:'+pkg+'/com.example.prayer.EventReminderReceiver' in b[:500]];(out/(slug+'-'+stage+'-event-active.json')).write_text(json.dumps(v));return v
  check('initial no event prefs',eventprefs('before')==[]);check('turn first reminder on',tap('Turn reminder on' if lang=='en' else 'تفعيل التذكير'));time.sleep(1);ids=eventprefs('on');live=eventalarms('on');check('actual one event enabled',len(ids)==1,str(ids));check('actual one event alarm',len(live)==1);shot('event-on')
  launch();tap('Calendar' if lang=='en' else 'التقويم');tap('Events' if lang=='en' else 'المناسبات');check('event pref survives reopen',eventprefs('reopen')==ids);live=eventalarms('reopen');check('event alarm rearmed on reopen',len(live)==1);shot('event-reopen')
  # Advance only isolated emulator clock to real AlarmManager delivery.
  dates=re.findall(r'origWhen=(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})',live[0]) if len(live)==1 else []
  check('event wallclock target parsed',bool(dates),str(live))
  if dates:
   dt=__import__('datetime').datetime.strptime(dates[0],'%Y-%m-%d %H:%M:%S').replace(tzinfo=__import__('zoneinfo').ZoneInfo('Africa/Cairo'));check('event target09local',dt.hour==9 and dt.minute==0);adb('shell','date','-s','@'+str(int(dt.timestamp())-5));adb('logcat','-c');time.sleep(10);raw=adb('shell','dumpsys','notification','--noredact');(out/(slug+'-event-delivered-notification.txt')).write_text(raw);check('actual event notification delivered',any(pkg in b and 'event_reminders_channel' in b for b in re.split(r'(?=NotificationRecord\()',raw)));eventalarms('after-delivery');(out/(slug+'-event-delivery-log.txt')).write_text(adb('logcat','-d'))
  check('turn enabled reminder off',tap('Reminder on' if lang=='en' else 'التذكير مفعّل'));check('event off prefs cleared',eventprefs('off')==[]);check('event off alarm canceled',len(eventalarms('off'))==0);shot('event-off')
pairs=[]
for n,a in states.items():
 other='comparison-'+n[len('original-'):]
 if n.startswith('original-') and other in states:pairs.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'semantic-comparison.json').write_text(json.dumps(pairs,ensure_ascii=False,indent=2));print('failures',sum(not x['passed'] for x in checks),'checks',len(checks))

assert all(c['passed'] for c in checks), 'See actual transaction evidence'
