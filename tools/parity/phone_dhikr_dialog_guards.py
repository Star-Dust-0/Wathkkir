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
  add='Add dhikr' if lang=='en' else 'إضافة ذكر';save='Save' if lang=='en' else 'حفظ';cancel='Cancel' if lang=='en' else 'إلغاء';newtag='New tag' if lang=='en' else 'تصنيف جديد';addtag='Add' if lang=='en' else 'إضافة'
  baseline=db('guard-baseline');check('complete default dhikr seed',len(baseline.get('dhikr_table',[]))==5);check('complete default tag seed',len(baseline.get('tag_table',[]))==4)
  def openadd():
   check('actual Azkar route',azkar());check('actual Add dhikr',tap(add))
  def dismiss(mode):
   if mode=='cancel':return tap(cancel)
   if mode=='back':adb('shell','input','keyevent','KEYCODE_BACK')
   if mode=='outside':adb('shell','input','tap','540','220')
   time.sleep(.7);return True
  def save_enabled():
   root=ET.fromstring(tree());parents={c:p for p in root.iter() for c in p}
   for n in root.iter('node'):
    if n.get('text')!=save:continue
    while n.get('clickable')!='true' and n in parents:n=parents[n]
    return n.get('enabled')=='true'
   return None
  for mode in ['cancel','back','outside']:
   openadd();check('add '+mode+' title',field(0,'GuardDraft'));check('add '+mode+' content',field(1,'GuardBody'));hide_keyboard();shot('add-'+mode+'-draft');check('add '+mode+' dismiss',dismiss(mode));d=db('add-'+mode);check('add '+mode+' no dhikr write',d.get('dhikr_table')==baseline.get('dhikr_table'));check('add '+mode+' no ref write',d.get('dhikr_tag_cross_ref')==baseline.get('dhikr_tag_cross_ref'))
  openadd();check('empty disabled',save_enabled() is False);shot('add-blank')
  check('only title entry',field(0,'GuardTitle'));hide_keyboard();check('missing content disabled',save_enabled() is False);shot('add-title-only')
  check('blank title',field(0,' '));check('content nonblank',field(1,'GuardBody'));hide_keyboard();check('whitespace title disabled',save_enabled() is False);shot('add-whitespace-title');check('blank cancel',tap(cancel))
  # Nested tag Add is a separate callback. Observe persistence after parent Cancel rather than assuming transaction semantics.
  openadd();check('nested tag show',tap(newtag));shot('nested-tag-empty');check('nested blank Add press',tap(addtag));shot('nested-tag-blank-press');check('nested tag field available',field(2,'GuardNestedDraft'));hide_keyboard();shot('nested-tag-draft')
  def nested_cancel():
   nodes=[n for n in ET.fromstring(tree()).iter('node') if n.get('text')==cancel];return bool(nodes) and touch(nodes[0])
  check('nested Cancel text',nested_cancel());shot('nested-tag-canceled');check('parent remains',save_enabled() is False);check('parent cancel after nested cancel',tap(cancel));d=db('nested-cancel');check('nested canceled no tag write',d.get('tag_table')==baseline.get('tag_table'))
  openadd();check('nested create show',tap(newtag));check('nested create name',field(2,'GuardNestedSaved'));hide_keyboard();check('nested actual Add',tap(addtag));time.sleep(1);shot('nested-tag-created');check('parent Cancel after nested Add',tap(cancel));d=db('nested-created-parent-cancel');tagrows=[v for v in d.get('tag_table',[]) if v[1]=='GuardNestedSaved'];check('separate nested tag persists parent Cancel',len(tagrows)==1);check('parent canceled no dhikr write',d.get('dhikr_table')==baseline.get('dhikr_table'))
  # Create an actual fixture, then verify edit dismiss/blank/trim without synthetic DB row mutation.
  openadd();check('fixture title',field(0,'GuardSaved'));check('fixture content',field(1,'GuardBody'));hide_keyboard();check('fixture Save',tap(save));created=db('guard-created');rows=[v for v in created.get('dhikr_table',[]) if v[1]=='GuardSaved'];check('fixture saved once',len(rows)==1)
  def openedit():
   check('edit actual Azkar',azkar());check('edit search',field(0,'GuardSaved'));hide_keyboard();check('edit options',tap('Options' if lang=='en' else 'خيارات'));check('edit dialog',tap('Edit dhikr and tags' if lang=='en' else 'تعديل الذكر والتصنيفات'))
  for mode in ['cancel','back','outside']:
   openedit();check('edit '+mode+' draft title',field(0,'GuardEdited'));check('edit '+mode+' draft body',field(1,'GuardReplacement'));hide_keyboard();shot('edit-'+mode+'-draft');check('edit '+mode+' dismiss',dismiss(mode));d=db('edit-'+mode);check('edit '+mode+' unchanged dhikr',d.get('dhikr_table')==created.get('dhikr_table'));check('edit '+mode+' unchanged refs',d.get('dhikr_tag_cross_ref')==created.get('dhikr_tag_cross_ref'))
  openedit();check('edit whitespace body',field(1,' '));hide_keyboard();check('edit blank body disabled',save_enabled() is False);shot('edit-blank-body');check('edit blank Cancel',tap(cancel));d=db('edit-blank');check('edit blank no write',d.get('dhikr_table')==created.get('dhikr_table'))
  (out/(slug+'-log.txt')).write_text(adb('logcat','-d'))

pairs=[]
for n,a in states.items():
 other='comparison-'+n[len('original-'):]
 if n.startswith('original-') and other in states:pairs.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'semantic-comparison.json').write_text(json.dumps(pairs,ensure_ascii=False,indent=2));print('failures',sum(not x['passed'] for x in checks),'checks',len(checks))

assert all(c['passed'] for c in checks), 'See actual dialog guard evidence'
