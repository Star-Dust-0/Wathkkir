"""3.4.3 picker preview hotfix verification: real launcher picker, system-generated previews, real widget renders."""
import subprocess,time,pathlib,json,re
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a,t=90):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=t);return (r.stdout+r.stderr).decode(errors='replace')
def check(item,ok,detail=''):checks.append(dict(item=item,passed=bool(ok),detail=str(detail)[:400]));print(item,bool(ok),str(detail)[:200],flush=True)
def shot(n):(out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
def host(mode,kind=None):
 args=['shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package','com.wathakker.app','--es','mode',mode]
 if kind:args+=['--es','kind',kind]
 return adb(*args)
def prefs(lang):
 folder='/data/data/com.wathakker.app/shared_prefs';adb('shell','mkdir','-p',folder)
 app=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
 loc='<map><float name="latitude" value="30.0444"/><float name="longitude" value="31.2357"/><string name="city_name">Cairo</string><string name="city_name_ar">القاهرة</string><boolean name="use_current_location" value="false"/><int name="calculation_method" value="1"/></map>'
 for f,x in [('app_prefs',app),('fajr_prefs',loc)]:adb('shell',f"echo '{x}' > {folder}/{f}.xml; chown $(stat -c %u:%g /data/data/com.wathakker.app) {folder}/{f}.xml; chmod 660 {folder}/{f}.xml; restorecon -R {folder}")
def uidump():
 adb('shell','uiautomator','dump','/sdcard/ui.xml');return adb('shell','cat','/sdcard/ui.xml')
def tap_text(xml,*texts):
 for text in texts:
  m=re.search(r'(?:text|content-desc)="'+re.escape(text)+r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',xml)
  if not m:m=re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*(?:text|content-desc)="'+re.escape(text)+r'"',xml)
  if m:
   x1,y1,x2,y2=map(int,m.groups());adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));return True
 return False
adb('wait-for-device');adb('root');time.sleep(4);adb('wait-for-device')
for a in ['app-debug.apk','widgethost.apk']:check(a+' installed','Success' in adb('install','-r','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420')
adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo')
adb('shell','appwidget','grantbind','--package','com.example.widgethost','--user','0')
# Real widget renders (comparison base for the preview bitmaps)
prefs('ar')
adb('shell','date','110610002026.00');adb('logcat','-c');host('oneui');time.sleep(13);shot('real-oneui');(out/'real-oneui-log.txt').write_text(adb('logcat','-d'))
adb('shell','date','110610002026.00');host('recover-homeA');time.sleep(13);shot('real-homeA-clock')
# System-generated picker previews (the exact API launchers use)
adb('logcat','-c');host('previews');time.sleep(12);shot('previews-top')
log=adb('logcat','-d');(out/'previews-log.txt').write_text(log)
for name,cls in [('OneUI','com.example.widget.OneUIWidgetProvider'),('Clock','com.example.widget.ClockWidgetProvider')]:
 check(name+' system preview generated','preview '+cls+' -> true' in log, re.findall(r'preview '+re.escape(cls)+r' -> \w+',log))
for i in range(3):
 adb('shell','input','swipe','540','1900','540','600','400');time.sleep(2);shot('previews-scroll-%d'%i)
# Real launcher widget picker
adb('shell','input','keyevent','KEYCODE_HOME');time.sleep(3)
adb('shell','input','swipe','540','1300','540','1300','900');time.sleep(2)
xml=uidump();(out/'picker-home-menu.xml').write_text(xml)
opened=tap_text(xml,'Widgets')
check('picker opened',opened)
time.sleep(3)
found_app=False
for i in range(14):
 xml=uidump()
 if 'وذكّر' in xml or 'Wathkkir' in xml:
  found_app=True
  tap_text(xml,'Wathkkir','وذكّر');time.sleep(3);break
 adb('shell','input','swipe','540','1900','540','600','400');time.sleep(2)
check('app row in picker',found_app)
seen=set()
for i in range(14):
 xml=uidump();(out/('picker-%d.xml'%i)).write_text(xml);shot('picker-%d'%i)
 for lbl,key in [('تقدم الصلاة - مزخرف','oneui'),('عداد الصلاة','clock')]:
  if lbl in xml:seen.add(key)
 if seen=={'oneui','clock'}:break
 adb('shell','input','swipe','540','1900','540','900','400');time.sleep(3)
check('picker shows مزخرف',('oneui' in seen))
check('picker shows عداد الصلاة',('clock' in seen))
(out/'checks.json').write_text(json.dumps(checks,indent=2));print(json.dumps(checks,indent=2));assert all(x['passed'] for x in checks)
