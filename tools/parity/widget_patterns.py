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
 s=adb('shell','am','instrument','-w','com.example.uiprobe/com.example.widgethost.HierarchyDump');(out/(slug+'-inspection-raw.txt')).write_text(s)
 x=next((x.split('=',1)[1] for x in s.splitlines() if x.startswith('INSTRUMENTATION_RESULT: hierarchy=')),None)
 if x is None:raise RuntimeError('Hierarchy probe failed: '+s)
 return x
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
for a in ['original.apk','comparison.apk','widgethost.apk','uiprobe.apk']:check('install '+a,'Success' in adb('install','-r','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','settings','put','global','hide_error_dialogs','1')
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang in ['en','ar']:
  slug=name+'-'+lang;adb('shell','pm','clear',pkg)
  for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',pkg,'android.permission.'+perm)
  adb('shell','cmd','uimode','night','yes' if lang=='ar' else 'no');folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
  x=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
  adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
  adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo');adb('shell','date','103112002026.00');launch();time.sleep(8)
  adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo');adb('shell','date','103112002026.00')
  def switch(label):
   root=ET.fromstring(tree());title=next((n for n in root.iter('node') if n.get('text')==label),None)
   if title is None:return False
   b=rect(title)
   candidates=[n for n in root.iter('node') if n.get('clickable')=='true' and n.get('class')=='android.view.View' and 0<rect(n)[2]-rect(n)[0]<200 and b[1] <= (rect(n)[1]+rect(n)[3])//2 <= b[3]+110]
   candidates.sort(key=lambda n:abs((rect(n)[1]+rect(n)[3]-b[1]-b[3])//2))
   (out/(slug+'-switch-targets.jsonl')).open('a').write(json.dumps(dict(label=label,title=b,candidates=[n.attrib for n in candidates]),ensure_ascii=False)+'\n')
   return bool(candidates) and touch(candidates[0])
  def alertprefs(n):
   x=adb('shell','cat',folder+'/prayer_alerts.xml');(out/(slug+'-'+n+'-prefs.xml')).write_text(x)
   try:return {n.get('name'):n.get('value',n.text) for n in ET.fromstring(x).iter() if n.get('name')}
   except:return {}
  def bell():
   launch();node=next((n for n in ET.fromstring(tree()).iter('node') if n.get('content-desc')=='bell_0'),None)
   if node is None:return False
   b=rect(node);cx=str((b[0]+b[2])//2);cy=str((b[1]+b[3])//2)
   adb('shell','input','swipe',cx,cy,cx,cy,'1200');time.sleep(4)
   return any(n.get('text') in ['At adhan time','عند الأذان'] for n in ET.fromstring(tree()).iter('node'))
  adb('shell','appwidget','grantbind','--package','com.example.widgethost','--user','0')
  def wp(n):
   x=adb('shell','cat',folder+'/fajr_prefs.xml');(out/(slug+'-'+n+'-prefs.xml')).write_text(x)
   try:return {n.get('name'):n.get('value',n.text) for n in ET.fromstring(x).iter() if n.get('name','').startswith('widget_')}
   except:return {}
  for kind in ['OneUI']:
   adb('logcat','-c');adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','mode','single','--es','kind',kind);time.sleep(5);ids=re.findall(r'WidgetRecoveryAudit.*bound kind='+kind+r' id=(\d+) ok=true',adb('logcat','-d'));check(kind+' bind',len(ids)==1)
   if not ids:continue
   wid=ids[0]
   def config():adb('shell','am','start','-W','-n',pkg+'/com.example.widget.WidgetConfigActivity','--ei','appWidgetId',wid);time.sleep(2)
   for idx,pattern in enumerate([0,1,8,9,10,11]):
    config();nodes=[n for n in ET.fromstring(tree()).iter('node') if n.get('clickable')=='true' and n.get('class')=='android.view.View' and 130<=rect(n)[2]-rect(n)[0]<=160 and 130<=rect(n)[3]-rect(n)[1]<=160];nodes.sort(key=lambda n:(rect(n)[1],-rect(n)[0] if lang=='ar' else rect(n)[0]));check('six pattern squares '+str(pattern),len(nodes)==6)
    if len(nodes)!=6:shot('pattern-target-fail-'+str(pattern));continue
    check('pattern choose '+str(pattern),touch(nodes[idx]));shot('pattern-'+str(pattern)+'-draft');check('pattern Apply '+str(pattern),tap('Apply' if lang=='en' else 'تطبيق'));check('pattern persisted '+str(pattern),wp('pattern-'+str(pattern)).get('widget_pattern_'+wid)==str(pattern));time.sleep(2);adb('shell','am','start','-W','--activity-reorder-to-front','-n','com.example.widgethost/.MainActivity');time.sleep(2);check('existing host foreground '+str(pattern),any(n.get('text')=='OneUI bind=true' for n in ET.fromstring(tree()).iter('node')));idsnow=re.findall(r'WidgetRecoveryAudit.*bound kind='+kind+r' id=(\d+) ok=true',adb('logcat','-d'));check('host not rebound '+str(pattern),idsnow==[wid]);adb('shell','date','103112002026.00');time.sleep(.4);shot('pattern-'+str(pattern)+'-host');config();shot('pattern-'+str(pattern)+'-reopen');adb('shell','input','keyevent','KEYCODE_BACK')
  (out/(slug+'-log.txt')).write_text(adb('logcat','-d'))
pairs=[]
for n,a in states.items():
 other='comparison-'+n[len('original-'):]
 if n.startswith('original-') and other in states:pairs.append(dict(scene=n[len('original-'):],same_tokens=a==states[other],original=a,comparison=states[other]))
(out/'semantic-comparison.json').write_text(json.dumps(pairs,ensure_ascii=False,indent=2));print('failures',sum(not x['passed'] for x in checks),'checks',len(checks))

assert all(x["passed"] for x in checks), "See checks.json"
