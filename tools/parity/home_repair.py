"""Bounded capture of recovered late HomeScreen literals/chip repair."""
import subprocess,time,pathlib,json,re,xml.etree.ElementTree as ET
out=pathlib.Path('evidence');out.mkdir(exist_ok=True)
def adb(*a):return subprocess.run(['adb',*a],capture_output=True,text=True,timeout=60).stdout
def hierarchy():
 x=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump')
 return next((s.split('=',1)[1] for s in x.splitlines() if s.startswith('INSTRUMENTATION_RESULT: hierarchy=')),'<hierarchy/>')
def tap(label):
 for n in ET.fromstring(hierarchy()).iter('node'):
  if n.get('text')==label:
   b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
   if len(b)==4:adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(2);return True
 return False
def shot(n):
 (out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']));(out/(n+'.xml')).write_text(hierarchy())
adb('wait-for-device');adb('root');time.sleep(5);adb('wait-for-device')
for a in ['comparison.apk','widgethost.apk']:assert 'Success' in adb('install','-g',a)
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
pkg='com.wathakker.app.compare';checks=[]
for lang,dark,label in [('en','no','Azkar'),('ar','yes','التذكيرات')]:
 adb('shell','am','force-stop',pkg);adb('shell','cmd','uimode','night',dark)
 folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
 xml=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
 adb('shell',f"echo '{xml}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
 adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(8)
 assert tap(label);shot(lang+'-home');x=hierarchy();checks.append({'lang':lang,'title_count':x.count('text="'+label+'"')});assert checks[-1]['title_count']>=2
 # First seeded tag exercises the shared animated chip group.
 tap('أذكار الصباح');shot(lang+'-tag-selected')
 tap('All' if lang=='en' else 'الكل');shot(lang+'-all')
(out/'result.json').write_text(json.dumps(checks,indent=2));(out/'logcat.txt').write_text(adb('logcat','-d'))
