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
  adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo');adb('shell','date','103112002026.00')
  def switch(label):
   root=ET.fromstring(tree());title=next((n for n in root.iter('node') if n.get('text')==label),None)
   if title is None:return False
   b=rect(title);targets=[n for n in root.iter('node') if n.get('clickable')=='true' and 0<rect(n)[2]-rect(n)[0]<200 and abs((rect(n)[1]+rect(n)[3]-b[1]-b[3])//2)<40]
   return len(targets)==1 and touch(targets[0])
  def alertprefs(n):
   x=adb('shell','cat',folder+'/prayer_alerts.xml');(out/(slug+'-'+n+'-prefs.xml')).write_text(x)
   try:return {n.get('name'):n.get('value',n.text) for n in ET.fromstring(x).iter() if n.get('name')}
   except:return {}
  def bell():
   launch();node=next((n for n in ET.fromstring(tree()).iter('node') if n.get('content-desc')=='bell_0'),None)
   if node is None:return False
   b=rect(node);cx=str((b[0]+b[2])//2);cy=str((b[1]+b[3])//2)
   adb('shell','input','swipe',cx,cy,cx,cy,'1200');time.sleep(1)
   return any(n.get('text') in ['At adhan time','عند الأذان'] for n in ET.fromstring(tree()).iter('node'))
  launch();check('short bell enable',tap('bell_0'));check('enabled persisted',alertprefs('short-enable').get('enabled_0')=='true')
  early='Early reminder' if lang=='en' else 'تنبيه قبل الأذان';cancel='Cancel' if lang=='en' else 'إلغاء';apply='Apply' if lang=='en' else 'تطبيق'
  check('Fajr bell',bell());before=alertprefs('before');shot('bell-initial');check('early enable draft',switch(early));draft=alertprefs('draft');check('draft offset10',draft.get('offset_0')=='10');shot('early-draft');check('cancel draft',tap(cancel));after=alertprefs('cancel');check('cancel offset restored',after.get('offset_0')==before.get('offset_0'));check('reopen bell',bell());shot('after-cancel');check('early enable saved',switch(early));check('apply',tap(apply));check('offset10 persisted',alertprefs('applied').get('offset_0')=='10');check('reopen applied',bell());shot('applied-reopen');check('early disable draft',switch(early));check('draft offset0',alertprefs('disable-draft').get('offset_0')=='0');adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1);check('back restores10',alertprefs('back').get('offset_0')=='10');check('reopen again',bell());check('early disable saved',switch(early));check('apply off',tap(apply));check('offset0 persisted',alertprefs('off').get('offset_0')=='0');bell();shot('off-reopen');tap(cancel)
  (out/(slug+'-log.txt')).write_text(adb('logcat','-d'))
pairs=[]
for n,a in states.items():
 other='comparison-'+n[len('original-'):]
 if n.startswith('original-') and other in states:pairs.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'semantic-comparison.json').write_text(json.dumps(pairs,ensure_ascii=False,indent=2));print('failures',sum(not x['passed'] for x in checks),'checks',len(checks))

assert all(x["passed"] for x in checks), "See checks.json"
