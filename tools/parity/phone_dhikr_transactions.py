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
  c=sqlite3.connect(p);d={t:c.execute('SELECT * FROM '+t+' ORDER BY 1').fetchall() for t in ['dhikr_table','tag_table','dhikr_tag_cross_ref','dhikr_history']};c.close();databases[slug+'-'+n]=d;(out/'databases.json').write_text(json.dumps(databases,ensure_ascii=False,indent=2));return d
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
  # A1: actual dismissal predicate before any force-stop, then persisted data.
  for mode in ['back','outside']:
   check(mode+' Azkar',azkar());check(mode+' Add',tap(add));field(0,'TxnDraft');field(1,'TxnBody');hide_keyboard()
   if mode=='back':adb('shell','input','keyevent','KEYCODE_BACK')
   else:adb('shell','input','tap','540','220')
   time.sleep(1);shot('add-'+mode+'-after');check(mode+' Add dialog actually gone',not named(newtitle));d=db('add-'+mode+'-closed');check(mode+' Add no saved draft',not any(v[1]=='TxnDraft' for v in d.get('dhikr_table',[])))
  check('create Azkar',azkar());check('create Add',tap(add));check('trim title input',field(0,'  TxnSaved  '));check('trim body input',field(1,'  TxnBody  '));hide_keyboard();check('tag selected',tap('أدعية عامة'));shot('create-tag-before-save');check('actual create Save',tap(save));d=db('created');rows=[v for v in d.get('dhikr_table',[]) if v[1]=='TxnSaved'];check('title and content trimmed',len(rows)==1 and rows[0][2]=='TxnBody');ident=rows[0][0] if rows else -1;tags=[v for v in d.get('tag_table',[]) if v[1]=='أدعية عامة'];check('shipped resumed create omits selected relation',not any(v[0]==ident for v in d.get('dhikr_tag_cross_ref',[])))
  def fixture(cold=True):
   if cold:check('fixture Azkar',azkar())
   check('fixture search',field(0,'TxnSaved'));hide_keyboard()
  def openedit():fixture();check('actual options',tap(options));check('actual edit',tap(edit))
  for mode in ['back','outside']:
   openedit();field(0,'TxnChanged');hide_keyboard()
   if mode=='back':adb('shell','input','keyevent','KEYCODE_BACK')
   else:adb('shell','input','tap','540','220')
   time.sleep(1);shot('edit-'+mode+'-after');check(mode+' Edit dialog actually gone',not named(edittitle));d=db('edit-'+mode+'-closed');check(mode+' Edit unchanged title',any(v[0]==ident and v[1]=='TxnSaved' for v in d.get('dhikr_table',[])))
  def opentimes(cold=True):
   fixture(cold);check('times options',tap(options));check('actual times dialog',tap(timeslabel));check('repeat title',named(repeat))
  opentimes();check('delete only time',tap(delete));shot('times-empty');check('last-time empty Save disabled',enabled(save) is False);check('empty parent Cancel',tap(cancel));d=db('times-empty-cancel');check('empty Cancel preserves09',any(v[0]==ident and v[4]=='09:00' for v in d.get('dhikr_table',[])))
  opentimes();check('add12 first',tap(addtime));check('add12 duplicate',tap(addtime));shot('times-duplicate-draft');check('duplicate parent Cancel',tap(cancel));d=db('times-duplicate-cancel');check('duplicate Cancel preserves09',any(v[0]==ident and v[4]=='09:00' for v in d.get('dhikr_table',[])))
  opentimes();check('add12 saved',tap(addtime));check('times actual Save',tap(save));time.sleep(1);shot('times-saved-live')
  def alarms(stage):
   raw=adb('shell','dumpsys','alarm');(out/(slug+'-'+stage+'-alarms.txt')).write_text(raw);sections=re.split(r'\n(?=    (?:RTC_WAKEUP|RTC|ELAPSED_WAKEUP|ELAPSED) #\d+:)',raw);lines=[z for z in sections[1:] if re.match(r'    \S+ #\d+: Alarm\{[^\n]* '+re.escape(pkg)+r'\}',z) and 'tag=*walarm*:'+pkg+'/com.example.alarm.AlarmReceiver' in z[:500]];(out/(slug+'-'+stage+'-alarm-lines.json')).write_text(json.dumps(lines));return lines
  live=alarms('two-time-save');check('actual pending dhikr alarms after Save',len(live)>=2)
  # Do not force-stop between saved schedule, actual disable/enable/delete and alarm inspection.
  def toggle():
   root=ET.fromstring(tree());titles=[n for n in root.iter('node') if n.get('text')=='TxnSaved' and n.get('class')!='android.widget.EditText'];
   if not titles:return False
   tb=rect(titles[0]);cy=(tb[1]+tb[3])//2;nodes=[n for n in root.iter('node') if n.get('clickable')=='true' and len(rect(n))==4 and 100<=rect(n)[2]-rect(n)[0]<=160 and rect(n)[1]<=cy<=rect(n)[3]];return bool(nodes) and touch(nodes[0])
  check('actual enabled switch off',toggle());time.sleep(1);shot('disabled-live');off=alarms('disabled');check('disable removes pending dhikr alarms',len(off)<len(live));check('actual enabled switch on',toggle());time.sleep(1);on=alarms('reenabled');check('reenable restores pending dhikr alarms',len(on)>=len(live));shot('reenabled-live')
  check('actual delete options',tap(options));check('actual delete',tap(delete));time.sleep(1);shot('deleted-live');gone=alarms('deleted');check('delete removes pending dhikr alarms',len(gone)<len(on));d=db('deleted');check('actual delete removes fixture',not any(v[0]==ident for v in d.get('dhikr_table',[])));check('actual delete removes relations',not any(v[0]==ident for v in d.get('dhikr_tag_cross_ref',[])))
  (out/(slug+'-log.txt')).write_text(adb('logcat','-d'))

pairs=[]
for n,a in states.items():
 other='comparison-'+n[len('original-'):]
 if n.startswith('original-') and other in states:pairs.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'semantic-comparison.json').write_text(json.dumps(pairs,ensure_ascii=False,indent=2));print('failures',sum(not x['passed'] for x in checks),'checks',len(checks))

assert all(c['passed'] for c in checks), 'See actual transaction evidence'
