"""Small deterministic reference/rebuild capture, one emulator per platform."""
import subprocess,time,sys,pathlib
kind=sys.argv[1]
out=pathlib.Path('evidence');out.mkdir(exist_ok=True)
def adb(*args):
 r=subprocess.run(['adb',*args],capture_output=True,timeout=45)
 return r.stdout.decode(errors='replace').strip()
def shot(name):
 (out/(name+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=45))
def clock():
 adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0')
 adb('shell','setprop','persist.sys.timezone','Africa/Cairo');adb('shell','date','102910002026.00')
def prefs(wear,lang):
 app='com.wathakker.app';folder=f'/data/data/{app}/shared_prefs'
 adb('shell','mkdir','-p',folder)
 if wear: xml=f'<map><int name="lang" value="{2 if lang=="en" else 1}"/><boolean name="perm_asked" value="true"/></map>';name='wear_prefs'
 else: xml=f'<map><string name="language">{lang}</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>';name='app_prefs'
 adb('shell',f"echo '{xml}' > {folder}/{name}.xml; chown $(stat -c %u:%g /data/data/{app}) {folder}/{name}.xml; chmod 660 {folder}/{name}.xml")
adb('wait-for-device')
for i in range(90):
 if adb('shell','getprop','sys.boot_completed').strip() == '1': break
 time.sleep(3)
time.sleep(30)
adb('root');time.sleep(4);adb('wait-for-device')
adb('shell','settings','put','global','hide_error_dialogs','1')
adb('shell','svc','power','stayon','true')
adb('shell','settings','put','system','screen_off_timeout','1800000')
adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard')
for build in ['original','rebuilt']:
 adb('uninstall','com.wathakker.app')
 print(adb('install','-g',f'{kind}-{build}.apk'),flush=True)
 if kind=='phone':print(adb('install','-r','widgethost.apk'),flush=True)
 else:
  # One-time warm-up after installation, before any English reference capture.
  adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard')
  print(adb('shell','am','start','-W','-n','com.wathakker.app/com.wathakker.wear.MainActivity'),flush=True)
  time.sleep(20)
 for lang in ['en','ar']:
  adb('shell','am','force-stop','com.wathakker.app');prefs(kind=='watch',lang);clock();adb('logcat','-c')
  if kind=='watch':
   for page in [0,1,2,3]:
    clock();print(adb('shell','am','start','-S','-W','-n','com.wathakker.app/com.wathakker.wear.MainActivity','--ei','page',str(page),'--ei','lang','2' if lang=='en' else '1'),flush=True)
    time.sleep(4);shot(f'{build}-{lang}-page{page}')
   print(adb('shell','am','start','-S','-W','-n','com.wathakker.app/com.wathakker.wear.CirclesActivity'),flush=True);time.sleep(3);shot(f'{build}-{lang}-circles')
  else:
   print(adb('shell','am','start','-S','-W','-n','com.wathakker.app/com.example.MainActivity'),flush=True);time.sleep(6);shot(f'{build}-{lang}-home')
   adb('push','widgethost.apk','/data/local/tmp/host.apk')
   print(adb('shell','CLASSPATH=/data/local/tmp/host.apk app_process / GrantBind com.example.widgethost'),flush=True)
   for mode in ['default','iqama','barsA']:
    clock();print(adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','mode',mode),flush=True);time.sleep(7);shot(f'{build}-{lang}-widgets-{mode}')
  (out/f'{build}-{lang}-logcat.txt').write_text(adb('logcat','-d'))
