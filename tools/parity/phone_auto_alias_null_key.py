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
 launch();time.sleep(1);return tap('Menu' if lang=='en' else 'القائمة') and tap(label)
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
adb('shell','cmd','location','set-location-enabled','false');adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','settings','put','global','hide_error_dialogs','1')
def fajr(n):
 x=adb('shell','cat',f'/data/data/{pkg}/shared_prefs/fajr_prefs.xml');(out/(slug+'-'+n+'-prefs.xml')).write_text(x)
 try:return {n.get('name'):n.get('value',n.text) for n in ET.fromstring(x).iter() if n.get('name')}
 except:return {'parse_error':x}
def location():
 return route('Settings' if lang=='en' else 'الإعدادات') and choose('Change location' if lang=='en' else 'تغيير الموقع')
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang in ['en','ar']:
  slug=name+'-'+lang
  for other in ['com.wathakker.app','com.wathakker.app.compare']:adb('shell','am','force-stop',other)
  adb('shell','pm','clear',pkg)
  for permission in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',pkg,'android.permission.'+permission)
  folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
  x=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
  f='<map><float name="latitude" value="51.5074"/><float name="longitude" value="-0.1278"/><string name="city_name">Cairo</string><string name="city_name_ar">القاهرة</string><boolean name="use_current_location" value="true"/><int name="calculation_method" value="1"/></map>'
  for file,value in [('app_prefs',x),('fajr_prefs',f)]:adb('shell',f"echo '{value}' > {folder}/{file}.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/{file}.xml; chmod 660 {folder}/{file}.xml; restorecon -R {folder}")
  adb('shell','cmd','uimode','night','yes' if lang=='ar' else 'no');adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','cmd','alarm','set-timezone','Africa/Cairo');adb('shell','date','103010002026.00')
  if not check('location route',location()):shot('route-failed');continue
  check('automatic mode fixture',fajr('before').get('use_current_location')=='true');check('null place key fixture','place_key' not in fajr('before-key'))
  edit='Edit city name' if lang=='en' else 'تعديل اسم المدينة';save='Save' if lang=='en' else 'حفظ';cancel='Cancel' if lang=='en' else 'إلغاء'
  shot('auto-null-key-initial')
  assert check('actual auto alias editor press',tap(edit)),'No editor'
  assert check('auto alias text entry',field(0,'AutoAliasAudit')),'No text field'
  hide_keyboard();shot('auto-null-key-draft')
  assert check('actual auto alias save press',tap(save)),'No Save'
  time.sleep(3);shot('auto-null-key-save-return');p=fajr('after-save');key=p.get('place_key');aliases=json.loads(p.get('city_aliases_json','{}'))
  editor=any(n.get('class')=='android.widget.EditText' for n in ET.fromstring(tree()).iter('node'))
  result={'key':key,'alias_saved':key is not None and aliases.get(key)=='AutoAliasAudit','editor_remains':editor,'prefs':p}
  (out/(slug+'-automatic-alias-outcome.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2))
  if key is not None:
   check('derived structural g key',key.startswith('g|'));check('derived key saves alias',result['alias_saved']);check('successful save closes editor',not editor)
   assert check('automatic alias force-stop reopen route',location()),'No reopen'
   shot('auto-null-key-reopen');q=fajr('reopen');check('automatic mode remains selected',q.get('use_current_location')=='true');check('derived key retained reopen',q.get('place_key')==key);check('alias retained reopen',json.loads(q.get('city_aliases_json','{}')).get(key)=='AutoAliasAudit');check('alias visible reopen','AutoAliasAudit' in tree())
  else:
   check('unresolved geocoder preserves editor',editor);check('unresolved geocoder writes no alias',not aliases)
   check('cancel unresolved editor actual press',tap(cancel));shot('auto-null-key-geocoder-unavailable')
  (out/(slug+'-logcat.txt')).write_text(adb('logcat','-d'))
pairs=[]
for n,a in states.items():
 other='comparison-'+n[len('original-'):]
 if n.startswith('original-') and other in states:pairs.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'semantic-comparison.json').write_text(json.dumps(pairs,ensure_ascii=False,indent=2));print('checks',len(checks),'fails',sum(not c['passed'] for c in checks))
assert all(c['passed'] for c in checks)
