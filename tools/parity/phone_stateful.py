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
 adb('shell','am','force-stop',pkg);adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(3)
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
def audit():
 global slug,pkg,lang
 adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device');slug='setup'
 for a in ['original.apk','comparison.apk','widgethost.apk']:check('install '+a,'Success' in adb('install','-r','-g',a))
 for k,v in [('auto_time','0'),('auto_time_zone','0'),('window_animation_scale','1'),('transition_animation_scale','1'),('animator_duration_scale','1')]:adb('shell','settings','put','global',k,v)
 adb('shell','setprop','persist.sys.timezone','Africa/Cairo');adb('shell','wm','size','1080x2400');adb('shell','wm','density','420')
 for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
  for lang in ['en','ar']:
   slug=name+'-'+lang;adb('shell','pm','clear',pkg);adb('shell','cmd','uimode','night','yes' if lang=='ar' else 'no');adb('shell','date','103010002026.00');folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
   x=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
   adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
   launch();time.sleep(7);baseline=db('baseline');check('seed data initialized',len(baseline.get('dhikr_table',[]))==5)
   add='Add dhikr' if lang=='en' else 'إضافة ذكر';save='Save' if lang=='en' else 'حفظ';cancel='Cancel' if lang=='en' else 'إلغاء'
   if not check('azkar open',azkar()):shot('azkar-failed');continue
   check('add opened',tap(add));shot('add-empty');check('empty cannot save',not tap(save));check('cancel empty',tap(cancel));d=db('empty-cancel');check('empty cancel no write',d==baseline)
   azkar();tap(add);check('title entry',field(0,'ParityDraft'));check('content entry',field(1,'ParityBody'));hide_keyboard();shot('add-filled');check('draft cancel',tap(cancel));d=db('draft-cancel');check('draft cancel no write',d==baseline)
   azkar();tap(add);field(0,'ParitySaved');field(1,'ParityBody');hide_keyboard();shot('add-before-save');check('valid save',tap(save));d=db('created');rows=d.get('dhikr_table',[]);created=[r for r in rows if r[1]=='ParitySaved'];check('created once persisted',len(created)==1 and len(rows)==len(baseline.get('dhikr_table',[]))+1)
   if not created:continue
   azkar();shot('created-reopen')
   # Search narrows all subsequent row buttons to the fixture item.
   search_nodes=[n for n in ET.fromstring(tree()).iter('node') if n.get('class')=='android.widget.EditText']
   check('search input',bool(search_nodes) and field(0,'ParitySaved'));hide_keyboard();shot('search-match');check('options menu',tap('Options' if lang=='en' else 'خيارات'));shot('item-options')
   check('edit dialog',tap('Edit dhikr and tags' if lang=='en' else 'تعديل الذكر والتصنيفات'));shot('edit');check('edit content',field(1,'ParityReplacement'));hide_keyboard();check('edit cancel',tap(cancel));d=db('edit-cancel');check('edit cancel data intact',d==databases.get(slug+'-created'))
   # Reopen, search and change fixture item, then check DB, not only visible text.
   def fixture():
    azkar();ok=field(0,'ParitySaved');hide_keyboard();return ok
   fixture();check('fixture favorite press',tap('Favorites' if lang=='en' else 'المفضلة'));d=db('favorite');f=[r for r in d.get('dhikr_table',[]) if r[1]=='ParitySaved'];check('favorite persisted',len(f)==1 and f[0][3]==1)
   fixture();tap('Options' if lang=='en' else 'خيارات');tap('Edit dhikr and tags' if lang=='en' else 'تعديل الذكر والتصنيفات');field(1,'ParityReplacement');hide_keyboard();check('edit save',tap(save));d=db('edited');f=[r for r in d.get('dhikr_table',[]) if r[1]=='ParitySaved'];check('edited content persisted',len(f)==1 and f[0][2]=='ParityReplacement')
   fixture();tap('Options' if lang=='en' else 'خيارات');check('reminder times opened',tap('Reminder times' if lang=='en' else 'أوقات التنبيه'));shot('times-open');check('add reminder time',choose('Add reminder time' if lang=='en' else 'إضافة وقت تنبيه'));shot('times-added');check('reminder cancel',tap(cancel));d=db('times-cancel');check('reminder cancel data intact',d==databases.get(slug+'-edited'))
   fixture();tap('Options' if lang=='en' else 'خيارات');tap('Reminder times' if lang=='en' else 'أوقات التنبيه');choose('Add reminder time' if lang=='en' else 'إضافة وقت تنبيه');check('reminder save',tap(save));d=db('times-saved');f=[r for r in d.get('dhikr_table',[]) if r[1]=='ParitySaved'];check('second reminder persisted',len(f)==1 and f[0][4]=='09:00,12:00')
   # Tag create/cancel/edit/delete and affected dhikr preservation.
   tags='Tags' if lang=='en' else 'التصنيفات';addtag='Add tag' if lang=='en' else 'إضافة تصنيف'
   route(tags);check('tag add opened',tap(addtag));shot('tag-add-empty');check('tag name entry',field(0,'ParityTagDraft'));hide_keyboard();check('tag cancel',tap(cancel));d=db('tag-cancel');check('tag draft not saved',not any(r[1]=='ParityTagDraft' for r in d.get('tag_table',[])))
   route(tags);tap(addtag);field(0,'ParityTagSaved');hide_keyboard();shot('tag-before-save');check('tag save',tap(save));d=db('tag-created');check('tag persisted once',sum(r[1]=='ParityTagSaved' for r in d.get('tag_table',[]))==1);route(tags);shot('tags-reopen')
   # Every global staged picker gets Cancel and Apply plus restart-pref evidence.
   settings='Settings' if lang=='en' else 'الإعدادات';apply='Apply' if lang=='en' else 'تطبيق'
   for idx,(entry,choice,key,value) in enumerate([('App font' if lang=='en' else 'خط التطبيق','App font light' if lang=='en' else 'خط التطبيق الخفيف','app_font','app_light'),('Appearance' if lang=='en' else 'المظهر','Dark' if lang=='en' else 'داكن','theme_mode','dark')]):
    route(settings);before=prefs();check(entry+' opened',choose(entry));check(entry+' option',tap(choice));shot('picker'+str(idx)+'-pending');check(entry+' cancel',tap(cancel));check(entry+' cancel no pref write',prefs().get(key)==before.get(key));route(settings);check(entry+' reopened',choose(entry));tap(choice);check(entry+' apply',tap(apply));launch();p=prefs();(out/(slug+'-picker'+str(idx)+'-prefs.json')).write_text(json.dumps(p,indent=2));check(entry+' applied persisted',p.get(key)==value,str(p.get(key)));route(settings);choose(entry);shot('picker'+str(idx)+'-applied-reopen');tap(cancel)
   (out/(slug+'-log.txt')).write_text(adb('logcat','-d'))
try:audit()
finally:
 pairs=[]
 for n,a in states.items():
  other='comparison-'+n[len('original-'):]
  if n.startswith('original-') and other in states:pairs.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
 (out/'semantic-comparison.json').write_text(json.dumps(pairs,ensure_ascii=False,indent=2));print('failures',sum(not x['passed'] for x in checks),'checks',len(checks),flush=True)
