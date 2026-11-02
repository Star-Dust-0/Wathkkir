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
  check('create Azkar',azkar());check('Add',tap(add));field(0,'NestedSaved');field(1,'NestedBody');hide_keyboard();check('create Save',tap(save));d=db('created');rows=[v for v in d.get('dhikr_table',[]) if v[1]=='NestedSaved'];check('fixture persisted',len(rows)==1);ident=rows[0][0] if rows else -1
  def fixture():
   check('fixture Azkar',azkar());check('fixture search',field(0,'NestedSaved'));hide_keyboard()
  def openedit():
   fixture();check('edit options',tap(options));check('edit dialog',tap(edit))
  def refs(d):return sorted(v[1] for v in d.get('dhikr_tag_cross_ref',[]) if v[0]==ident)
  target=next((v[0] for v in d.get('tag_table',[]) if v[1]=='أدعية عامة'),-1);check('tag id available',target!=-1)
  openedit();check('edit select tag',tap('أدعية عامة'));shot('edit-tag-selected');check('edit tag Cancel',tap(cancel));d=db('tag-cancel');check('tag Cancel no relation',refs(d)==[])
  openedit();check('edit select tag saved',tap('أدعية عامة'));field(0,'  NestedSaved  ');field(1,'  NestedEdited  ');hide_keyboard();check('edit Save',tap(save));d=db('tag-save');check('tag Save relation',refs(d)==[target]);check('edit trim persisted',any(v[0]==ident and v[1]=='NestedSaved' and v[2]=='NestedEdited' for v in d.get('dhikr_table',[])))
  openedit();shot('edit-tag-reopen');check('edit remove draft',tap('أدعية عامة'));check('remove Cancel',tap(cancel));d=db('tag-remove-cancel');check('remove Cancel keeps relation',refs(d)==[target])
  openedit();check('edit remove saved',tap('أدعية عامة'));check('remove Save',tap(save));d=db('tag-remove-save');check('remove Save no relation',refs(d)==[])
  def opentimes():
   fixture();check('times options',tap(options));check('times dialog',tap(timeslabel))
  def timerow():
   root=ET.fromstring(tree())
   for n in root.iter('node'):
    if re.fullmatch(r'\d{1,2}:\d{2}.*',n.get('text','')) and touch(n):return True
   return False
  def wheels():
   root=ET.fromstring(tree());cols=[n for n in root.iter('node') if n.get('scrollable')=='true' and len(rect(n))==4 and 100<rect(n)[2]-rect(n)[0]<300 and 400<rect(n)[3]-rect(n)[1]<900]
   return sorted(cols,key=lambda n:rect(n)[0])
  def center(n):
   b=rect(n);cy=(b[1]+b[3])/2;vals=[]
   for c in n.iter('node'):
    t=c.get('text','');cb=rect(c)
    if t and len(cb)==4:vals.append((abs((cb[1]+cb[3])/2-cy),t,cb))
   return min(vals)[1:] if vals else ('',[])
  def selectcol(index,target,values):
   for attempt in range(36):
    cols=wheels()
    if index>=len(cols):return False
    n=cols[index];current,cb=center(n)
    if current==target:return True
    if current not in values:return False
    diff=(values.index(target)-values.index(current))%len(values)
    direction=(1 if values.index(target)>values.index(current) else -1) if index==2 else (1 if diff<=len(values)/2 else -1)
    b=rect(n);x=(b[0]+b[2])//2;y=(b[1]+b[3])//2;step=round((b[3]-b[1])/5)
    adb('shell','input','swipe',str(x),str(y),str(x),str(y-direction*step),'600');time.sleep(1)
   return False
  def pick(h,m,ampm=None):
   cols=wheels();check('actual wheel columns',len(cols)==(3 if ampm else 2),str([(rect(n),center(n)) for n in cols]))
   hours=[str(i) for i in range(1,13)] if ampm else [f'{i:02d}' for i in range(24)]
   check('hour wheel '+h,selectcol(0,h,hours));check('minute wheel '+m,selectcol(1,m,[f'{i:02d}' for i in range(60)]))
   if ampm:check('ampm wheel '+ampm,selectcol(2,ampm,['AM','PM'] if lang=='en' else ['ص','م']))
  def currenttime(d):return next((v[4] for v in d.get('dhikr_table',[]) if v[0]==ident),None)
  for is24 in [False,True]:
   mode='24' if is24 else '12';adb('shell','settings','put','system','time_12_24',mode)
   opentimes();check(mode+' open nested',timerow());shot(mode+'-nested-initial')
   pick('23' if is24 else '11','59',None if is24 else ('PM' if lang=='en' else 'م'));shot(mode+'-nested2359')
   check(mode+' nested Cancel',tap(cancel));shot(mode+'-nested-cancel-parent');check(mode+' parent Cancel',tap(cancel));d=db(mode+'-nested-cancel');check(mode+' nested Cancel keeps09',currenttime(d)=='09:00')
   opentimes();check(mode+' nested open again',timerow());pick('23' if is24 else '11','59',None if is24 else ('PM' if lang=='en' else 'م'));check(mode+' nested Save draft',tap(save));shot(mode+'-2359-parent-draft');check(mode+' parent Cancel rolls back nestedSave',tap(cancel));d=db(mode+'-parent-cancel');check(mode+' parent Cancel keeps09',currenttime(d)=='09:00')
   opentimes();check(mode+' final nested open',timerow());pick('23' if is24 else '11','59',None if is24 else ('PM' if lang=='en' else 'م'));check(mode+' nested Save final',tap(save));check(mode+' parent Save2359',tap(save));d=db(mode+'-save2359');check(mode+' stored2359',currenttime(d)=='23:59')
   opentimes();check(mode+' midnight nested open',timerow());pick('00' if is24 else '12','00',None if is24 else ('AM' if lang=='en' else 'ص'));shot(mode+'-midnight-wheel');check(mode+' midnight nested Save',tap(save));check(mode+' midnight parent Save',tap(save));d=db(mode+'-midnight');check(mode+' stored0000',currenttime(d)=='00:00')
   # Return fixture to09 for independent next mode, through UI not DB edits.
   opentimes();check(mode+' reset nested open',timerow());pick('09' if is24 else '9','00',None if is24 else ('AM' if lang=='en' else 'ص'));tap(save);tap(save);d=db(mode+'-reset09');check(mode+' reset09',currenttime(d)=='09:00')
  (out/(slug+'-log.txt')).write_text(adb('logcat','-d'))
pairs=[]
for n,a in states.items():
 other='comparison-'+n[len('original-'):]
 if n.startswith('original-') and other in states:pairs.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'semantic-comparison.json').write_text(json.dumps(pairs,ensure_ascii=False,indent=2));print('failures',sum(not x['passed'] for x in checks),'checks',len(checks))

assert all(c['passed'] for c in checks), 'See actual transaction evidence'
