"""Bounded tablet layout evidence after final source restoration."""
import subprocess,time,pathlib,json,re,xml.etree.ElementTree as ET
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a):return subprocess.run(['adb',*a],capture_output=True,text=True,timeout=60).stdout
def tree():
 x=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump')
 return next((s.split('=',1)[1] for s in x.splitlines() if s.startswith('INSTRUMENTATION_RESULT: hierarchy=')),'<hierarchy/>')
def clear_launcher_dialog():
 for attempt in range(3):
  root=ET.fromstring(tree())
  if not any("isn't responding" in n.get('text','') or 'is not responding' in n.get('text','') for n in root.iter('node')):return
  for n in root.iter('node'):
   if n.get('text')=='Close app':
    b=list(map(int,re.findall(r'\d+',n.get('bounds',''))));adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(2);break
def shot(n):
 clear_launcher_dialog()
 (out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']));(out/(n+'.xml')).write_text(tree())
def tap(label):
 clear_launcher_dialog()
 for n in ET.fromstring(tree()).iter('node'):
  if label in [n.get('text'),n.get('content-desc')]:
   b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
   if len(b)==4:adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(3);return True
 return False
def bottom(label):
 clear_launcher_dialog()
 for n in ET.fromstring(tree()).iter('node'):
  if n.get('text')==label:
   b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
   if len(b)==4 and b[1]>2100:adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(3);return True
 return False
adb('wait-for-device');adb('root');time.sleep(5);adb('wait-for-device')
for a in ['original.apk','comparison.apk','widgethost.apk']:assert 'Success' in adb('install','-g',a)
adb('shell','wm','size','1800x2560');adb('shell','wm','density','280');adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang in ['en','ar']:
  slug=name+'-'+lang;adb('shell','am','force-stop',pkg);folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
  xml=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
  adb('shell',f"echo '{xml}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
  adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(8);shot(slug+'-prayer')
  for label in (['Hisn','Calendar','Prayer'] if lang=='en' else ['حصن المسلم','التقويم','الصلاة']):
   ok=bottom(label);checks.append({'name':slug+'-bottom-'+label,'passed':ok});shot(slug+'-'+str(len(checks)))
  tap('Menu' if lang=='en' else 'القائمة');shot(slug+'-drawer');time.sleep(4);shot(slug+'-drawer-settled')
(out/'checks.json').write_text(json.dumps(checks,indent=2));(out/'logcat.txt').write_text(adb('logcat','-d'));assert all(x['passed'] for x in checks)
