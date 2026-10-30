"""Small deterministic reference/rebuild capture, one emulator per platform."""
import subprocess,time,sys,pathlib
kind=sys.argv[1]
out=pathlib.Path('evidence');out.mkdir(exist_ok=True)
def adb(*args):
 r=subprocess.run(['adb',*args],capture_output=True,timeout=45)
 return r.stdout.decode(errors='replace').strip()
def clear_launcher_anr():
 # Launcher can ANR during cold emulator start. Close only its identified system dialog.
 adb('shell','uiautomator','dump','/sdcard/anr.xml')
 xml=adb('shell','cat','/sdcard/anr.xml')
 if "Pixel Launcher isn't responding" in xml:
  import xml.etree.ElementTree as ET,re
  for n in ET.fromstring(xml).iter('node'):
   if n.get('text')=='Close app':
    a=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
    if len(a)==4:adb('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2));time.sleep(2)
def shot(name):
 clear_launcher_anr()
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
   print(adb('shell','am','start','-S','-W','-n','com.wathakker.app/com.example.MainActivity'),flush=True);time.sleep(6);clear_launcher_anr();shot(f'{build}-{lang}-home')
   dims=adb('shell','wm','size').strip().split()[-1].split('x');ww,hh=map(int,dims)
   def tap(x,y): adb('shell','input','tap',str(int(ww*x)),str(int(hh*y)));time.sleep(2)
   def dump(name):
    adb('shell','uiautomator','dump','/sdcard/window.xml')
    (out/(name+'.xml')).write_text(adb('shell','cat','/sdcard/window.xml'))
   dump(f'{build}-{lang}-home-ui')
   # Inspect all four real bottom tabs, then return to prayer and open settings.
   for tab in [1,2,3]:
    x=(tab+.5)/4 if lang=='en' else 1-(tab+.5)/4
    tap(x,.925);shot(f'{build}-{lang}-tab{tab}');dump(f'{build}-{lang}-tab{tab}-ui')
   tap(.125 if lang=='en' else .875,.925)
   tap(.89 if lang=='en' else .11,.09);shot(f'{build}-{lang}-settings');dump(f'{build}-{lang}-settings-ui')
   adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(2)
   tap(.11 if lang=='en' else .89,.09);shot(f'{build}-{lang}-drawer');dump(f'{build}-{lang}-drawer-ui')
   adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1)
   adb('push','widgethost.apk','/data/local/tmp/host.apk')
   print(adb('shell','CLASSPATH=/data/local/tmp/host.apk app_process / GrantBind com.example.widgethost'),flush=True)
   for mode in ['default','iqama','barsA']:
    clock();print(adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','mode',mode),flush=True);time.sleep(7);shot(f'{build}-{lang}-widgets-{mode}')
    time.sleep(5);shot(f'{build}-{lang}-widgets-{mode}-later')
    dump(f'{build}-{lang}-widgets-{mode}-ui')
  (out/f'{build}-{lang}-logcat.txt').write_text(adb('logcat','-d'))
