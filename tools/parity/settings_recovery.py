"""Focused settings source-recovery interaction check. Never publishes an installer."""
import subprocess,time,pathlib,json,re,xml.etree.ElementTree as ET
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a):return subprocess.run(['adb',*a],capture_output=True,text=True,timeout=60).stdout
def hierarchy():
 x=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump')
 return next((s.split('=',1)[1] for s in x.splitlines() if s.startswith('INSTRUMENTATION_RESULT: hierarchy=')),'<hierarchy/>')
def tap(label,switch=False):
 for n in ET.fromstring(hierarchy()).iter('node'):
  if n.get('text')==label:
   b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
   if len(b)==4:adb('shell','input','tap',str(970 if switch else (b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(1);return True
 return False
def shot(n):
 (out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']));(out/(n+'.xml')).write_text(hierarchy())
def check(n,v):checks.append({'name':n,'passed':bool(v)});assert v,n
def swipe(up=True):adb('shell','input','swipe','540','1800' if up else '650','540','650' if up else '1800','500');time.sleep(1)
def prefs(pkg):return adb('shell','cat',f'/data/data/{pkg}/shared_prefs/app_haptics.xml')
def recording(n):return subprocess.Popen(['adb','shell','screenrecord','--time-limit','60',f'/sdcard/{n}.mp4'])
def stop(proc,n):
 adb('shell','pkill','-INT','screenrecord')
 try:proc.wait(timeout=8)
 except:proc.kill()
 adb('pull',f'/sdcard/{n}.mp4',str(out/(n+'.mp4')))
adb('wait-for-device');adb('root');time.sleep(5);adb('wait-for-device')
for a in ['original.apk','comparison.apk','widgethost.apk']:check('install-'+a,'Success' in adb('install','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
adb('shell','settings','put','system','haptic_feedback_enabled','1')
for k in ['window_animation_scale','transition_animation_scale','animator_duration_scale']:adb('shell','settings','put','global',k,'1')
try:
 for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
  for lang,dark in [('en','no'),('ar','yes')]:
   slug=name+'-'+lang;adb('shell','am','force-stop',pkg);adb('shell','cmd','uimode','night',dark)
   folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
   xml=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
   adb('shell',f"echo '{xml}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
   adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(8)
   check(slug+'-settings',tap('Settings' if lang=='en' else 'الإعدادات'));shot(slug+'-settings-top')
   proc=recording(slug+'-settings-motion')
   for i in range(4):swipe();shot(slug+'-scroll-'+str(i))
   for i in range(4):swipe(False)
   label='Haptic feedback' if lang=='en' else 'الاستجابة اللمسية'
   for i in range(6):
    if label in hierarchy():break
    swipe()
   check(slug+'-haptic-visible',label in hierarchy());shot(slug+'-haptic-before')
   # Explicitly exercise row touch, then separate switch touch. Persisted state is the assertion.
   check(slug+'-row-off',tap(label));time.sleep(1);p=prefs(pkg);(out/(slug+'-off-prefs.xml')).write_text(p);check(slug+'-persist-off','value="false"' in p);shot(slug+'-haptic-off')
   check(slug+'-switch-on',tap(label,True));time.sleep(1);p=prefs(pkg);(out/(slug+'-on-prefs.xml')).write_text(p);check(slug+'-persist-on','value="true"' in p);shot(slug+'-haptic-on')
   stop(proc,slug+'-settings-motion')
   # Relaunch, verify persisted on; this is software evidence, not felt motor output.
   adb('shell','am','force-stop',pkg);adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(4)
   check(slug+'-restart-on','value="true"' in prefs(pkg))
   check(slug+'-drawer',tap('Menu' if lang=='en' else 'القائمة'));shot(slug+'-drawer')
   if name=='comparison':
    adb('shell','am','force-stop',pkg);adb('shell','wm','size','1800x2560');adb('shell','wm','density','280');adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(4)
    if tap('Menu' if lang=='en' else 'القائمة'):shot(slug+'-wide-drawer')
    adb('shell','wm','size','1080x2400');adb('shell','wm','density','420')
finally:
 (out/'checks.json').write_text(json.dumps(checks,indent=2));(out/'logcat.txt').write_text(adb('logcat','-d'))
 (out/'vibrator.txt').write_text(adb('shell','dumpsys','vibrator_manager'))
