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
  if not tap('Menu' if lang=='en' else 'القائمة'):continue
  time.sleep(2)
  if not tap(label):continue
  time.sleep(2)
  texts=[n.get('text','') for n in ET.fromstring(tree()).iter('node')]
  expected=['Enable Fajr alarm','تفعيل منبه صلاة الفجر','طريقة تحديد الموقع']
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
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang in ['en','ar']:
  slug=name+'-'+lang;adb('shell','pm','clear',pkg)
  for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',pkg,'android.permission.'+perm)
  adb('shell','cmd','uimode','night','yes' if lang=='ar' else 'no');folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
  x=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
  adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
  launch();time.sleep(8)
  adb('shell','date','103112002026.00')
  label='Fajr alarm' if lang=='en' else 'منبه صلاة الفجر'
  assert check('Fajr settings verified destination',route(label)), 'Missing Fajr route'
  def fp(n):
   x=adb('shell','cat',folder+'/fajr_prefs.xml');(out/(slug+'-'+n+'-prefs.xml')).write_text(x)
   try:return {n.get('name'):n.get('value',n.text) for n in ET.fromstring(x).iter() if n.get('name')}
   except:return {}
  before=fp('before-choices');check('initial vibration default',before.get('is_vibration_enabled','true')=='true');shot('before-choices')
  headings={'duration':('Ring time before auto-stop (minutes)' if lang=='en' else 'مدة الرنين قبل الإيقاف التلقائي (بالدقائق)'), 'snooze':('Snooze length (minutes)' if lang=='en' else 'مدة الغفوة (بالدقائق)')}
  def bottom():
   for _ in range(5):swipe()
  def select_chip(family,value):
   bottom()
   for attempt in range(4):
    root=ET.fromstring(tree());h=next((n for n in root.iter('node') if n.get('text')==headings[family]),None)
    if h is None:shot('missing-'+family+'-'+str(value));return False
    y=rect(h)[3];maxy=rect(next(n for n in root.iter('node') if n.get('text')==headings['snooze']))[1] if family=='duration' else 2250
    matches=[n for n in root.iter('node') if n.get('text')==str(value) and y<rect(n)[1]<maxy and rect(n)[0]>5 and rect(n)[2]<1075]
    if matches and touch(matches[0]):return True
    # LazyRow is horizontal; RTL advances in the opposite physical direction.
    adb('shell','input','swipe','200' if lang=='ar' else '900',str(y+90),'900' if lang=='ar' else '200',str(y+90),'300');time.sleep(.5)
   return False
  for family,key,values in [('duration','max_duration_minutes',[1,2,3,5,10,15]),('snooze','snooze_minutes',[5,10,15,20,30])]:
   for value in values:
    assert check(family+' choose '+str(value),select_chip(family,value)), 'Missing scoped chip'
    check(family+' persisted '+str(value),fp(family+'-'+str(value)).get(key)==str(value));shot(family+'-'+str(value))
   assert check(family+' settings verified reopen',route(label)), 'Reopen failed'
   check(family+' final persists reopen',fp(family+'-reopen').get(key)==str(values[-1]));bottom();shot(family+'-reopen')
  (out/(slug+'-log.txt')).write_text(adb('logcat','-d'));adb('shell','am','force-stop',pkg)
pairs=[]
for n,a in states.items():
 other='comparison-'+n[len('original-'):]
 if n.startswith('original-') and other in states:pairs.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'semantic-comparison.json').write_text(json.dumps(pairs,ensure_ascii=False,indent=2));print('failures',sum(not x['passed'] for x in checks),'checks',len(checks))

assert all(x['passed'] for x in checks), 'See checks.json'
