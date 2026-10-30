"""Paired interaction evidence. Failure is a gate, not an instruction to publish."""
import subprocess,time,pathlib,json,re,xml.etree.ElementTree as ET
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
adb('shell','svc','power','stayon','true');adb('shell','wm','size','1080x2400');adb('shell','wm','density','420')
for apk in ['original.apk','comparison.apk','widgethost.apk']:check('install-'+apk,'Success' in adb('install','-r','-g',apk))
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for lang in ['en','ar']:
  for dark in ['no','yes']:
   slug=f'{name}-{lang}-{dark}';adb('shell','am','force-stop',pkg)
   adb('shell','cmd','uimode','night',dark);adb('shell','date','103010002026.00')
   folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
   xml=f'<map><string name="language">{lang}</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
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
   adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1)
   proc=record_start(slug+'-tabs')
   for tab in [1,2,3,0]:
    x=int(1080*((tab+.5)/4 if lang=='en' else 1-(tab+.5)/4));adb('shell','input','tap',str(x),'2210');time.sleep(1);shot(slug+f'-tab{tab}')
   record_stop(proc,slug+'-tabs')
  # Widget host binds both packages at the same sizes under the same language.
  adb('shell','appwidget','grantbind','--package','com.example.widgethost','--user','0')
  for mode in ['barsA','barsB','previews']:
   adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','mode',mode);time.sleep(5);shot(f'{name}-{lang}-widgets-{mode}')
   for i in range(2):swipe();shot(f'{name}-{lang}-widgets-{mode}-scroll{i}')
  adb('shell','dumpsys','vibrator_manager')
 (OUT/(name+'-vibrator.txt')).write_text(adb('shell','dumpsys','vibrator_manager'))
(OUT/'checks.json').write_text(json.dumps(checks,indent=2));(OUT/'logcat.txt').write_text(adb('logcat','-d'))
print(json.dumps({'checks':len(checks),'failures':[x for x in checks if not x['passed']]},indent=2))

if any(not x["passed"] for x in checks): raise SystemExit("Interaction checks failed; inspect checks.json and actual screenshots")
