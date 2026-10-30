"""Paired interaction evidence. Failure is a gate, not an instruction to publish."""
import subprocess,time,pathlib,json,re,os,xml.etree.ElementTree as ET
OUT=pathlib.Path('evidence');OUT.mkdir(exist_ok=True);checks=[]
def run(*args):
 r=subprocess.run(args,capture_output=True,timeout=60)
 return (r.stdout+r.stderr).decode(errors='replace').strip()
def adb(*args):return run('adb',*args)
def hierarchy():
 text=adb('shell','am','instrument','-w','com.example.widgethost/.HierarchyDump')
 for line in text.splitlines():
  if line.startswith('INSTRUMENTATION_RESULT: hierarchy='):return line.split('=',1)[1]
 return '<hierarchy/>'
def shot(name):
 (OUT/(name+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=30))
 (OUT/(name+'.xml')).write_text(hierarchy())
def tap_text(label):
 root=ET.fromstring(hierarchy())
 for n in root.iter('node'):
  if label in [n.get('text'),n.get('content-desc')]:
   b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
   if len(b)==4:
    adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(1);return True
 return False
def foreground(pkg):
 text=adb('shell','dumpsys','activity','activities')
 return any(pkg+'/' in line and any(marker in line for marker in ['mResumedActivity:', 'topResumedActivity=', 'mFocusedApp=']) for line in text.splitlines())
def relaunch(pkg):
 adb('shell','am','force-stop',pkg)
 adb('shell','am','start','-W','-n',f'{pkg}/com.example.MainActivity');time.sleep(3)
 (OUT/(pkg+'-foreground.txt')).write_text(adb('shell','dumpsys','activity','activities'))
 check(pkg+'-foreground',foreground(pkg))
def tap_bottom(label):
 root=ET.fromstring(hierarchy())
 for n in root.iter('node'):
  if n.get('text') != label:continue
  b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
  if len(b)==4 and b[1]>2100:
   adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(2);return True
 return False
def swipe(up=True):
 adb('shell','input','swipe','540','1800' if up else '650','540','650' if up else '1800','500');time.sleep(1)
def check(name,ok,**detail):checks.append(dict(name=name,passed=bool(ok),**detail))
def record_start(name):
 return subprocess.Popen(['adb','shell','screenrecord','--time-limit','35',f'/sdcard/{name}.mp4'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
def record_stop(proc,name):
 adb('shell','pkill','-INT','screenrecord')
 try:proc.wait(timeout=8)
 except:proc.kill()
 adb('pull',f'/sdcard/{name}.mp4',str(OUT/(name+'.mp4')))
adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device')
adb('shell','settings','put','system','haptic_feedback_enabled','1')
adb('shell','settings','put','global','window_animation_scale','1');adb('shell','settings','put','global','transition_animation_scale','1');adb('shell','settings','put','global','animator_duration_scale','1')
adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo')
adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
adb('shell','svc','power','stayon','true');adb('shell','wm','size','1080x2400');adb('shell','wm','density','420')
for apk in ['original.apk','comparison.apk','widgethost.apk']:check('install-'+apk,'Success' in adb('install','-r','-g',apk))
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang in ['en','ar']:
  for dark in ['no','yes']:
   if os.getenv('FOCUSED_UI') == '1' and (lang,dark) not in [('en','no'),('ar','yes')]:continue
   slug=f'{name}-{lang}-{dark}';adb('shell','am','force-stop',pkg)
   adb('shell','cmd','uimode','night',dark);adb('shell','date','103010002026.00')
   folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
   xml=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
   adb('shell',f"echo '{xml}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
   adb('shell','am','start','-W','-n',f'{pkg}/com.example.MainActivity');time.sleep(5)
   shot(slug+'-prayer');check(slug+'-running',bool(adb('shell','pidof',pkg)))
   proc=record_start(slug+'-scroll')
   for i in range(3):swipe();shot(slug+f'-prayer-up{i}')
   for i in range(3):swipe(False);shot(slug+f'-prayer-down{i}')
   record_stop(proc,slug+'-scroll')
   settings='Settings' if lang=='en' else 'الإعدادات'
   opened=tap_text(settings);check(slug+'-settings',opened);shot(slug+'-settings-top')
   if opened:
    for i in range(4):swipe();shot(slug+f'-settings-scroll{i}')
    for i in range(4):swipe(False)
    vib='Notification vibration' if lang=='en' else 'الاهتزاز للإشعارات'
    for i in range(3):
     if tap_text(vib):break
     swipe()
    else:i=3
    check(slug+'-vibration-toggle-found',i!=3);shot(slug+'-vibration-off')
    if i!=3:tap_text(vib)
    haptic='Haptic feedback' if lang=='en' else 'الاستجابة اللمسية'
    check(slug+'-haptic-toggle-found',tap_text(haptic));shot(slug+'-haptic-off');tap_text(haptic)
    for i in range(5):swipe(False)
    adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1)
   menu='Menu' if lang=='en' else 'القائمة';check(slug+'-drawer',tap_text(menu));shot(slug+'-drawer')
   # Drawer back behavior is recorded separately; relaunch before tab tests so a
   # failed dismissal cannot turn coordinate taps into unrelated browser actions.
   adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1)
   check(slug+'-drawer-back-stays-in-app',foreground(pkg));shot(slug+'-drawer-back')
   relaunch(pkg)
   proc=record_start(slug+'-tabs')
   labels=['Hisn','Azkar','Calendar','Prayer'] if lang=='en' else ['حصن المسلم','التذكيرات','التقويم','الصلاة']
   for i,label in enumerate(labels):
    found=tap_bottom(label);check(slug+'-tab-'+str(i)+'-found',found)
    check(slug+'-tab-'+str(i)+'-in-app',foreground(pkg));shot(slug+f'-tab{i}')
    if i==0:
     time.sleep(10);shot(slug+'-hisn-settled')
     fav='Favorites' if lang=='en' else 'المفضلة';check(slug+'-hisn-favorite-chip',tap_text(fav));time.sleep(3);shot(slug+'-hisn-favorites')
     tap_text('All' if lang=='en' else 'الكل')
   record_stop(proc,slug+'-tabs')
  # Widget host binds both packages at the same sizes under the same language.
  adb('shell','appwidget','grantbind','--package','com.example.widgethost','--user','0')
  for mode in ['barsA','barsB','previews']:
   adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','mode',mode);time.sleep(5);shot(f'{name}-{lang}-widgets-{mode}')
   for i in range(2):swipe();shot(f'{name}-{lang}-widgets-{mode}-scroll{i}')
  adb('shell','dumpsys','vibrator_manager')
 adb('pull',f'/data/data/{pkg}/databases',str(OUT/(name+'-databases')))
 adb('pull',f'/data/data/{pkg}/shared_prefs/hisn_prefs.xml',str(OUT/(name+'-hisn-prefs.xml')))
 (OUT/(name+'-vibrator.txt')).write_text(adb('shell','dumpsys','vibrator_manager'))
(OUT/'checks.json').write_text(json.dumps(checks,indent=2));(OUT/'logcat.txt').write_text(adb('logcat','-d'))
print(json.dumps({'checks':len(checks),'failures':[x for x in checks if not x['passed']]},indent=2))

if any(not x["passed"] for x in checks): raise SystemExit("Interaction checks failed; inspect checks.json and actual screenshots")
