"""Oct25 merge + Oct26 recovery acceptance: real hosts, paired pixels, taps, alarms."""
import subprocess,time,pathlib,json,re,os,xml.etree.ElementTree as ET
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=70);return (r.stdout+r.stderr).decode(errors='replace')
def check(item,ok,detail=''):checks.append(dict(item=item,passed=bool(ok),detail=detail));print(item,bool(ok),detail,flush=True)
def shot(n):
 state=adb('shell','dumpsys','window');(out/(n+'-window.txt')).write_text(state)
 check(n+' foreground',any('mCurrentFocus=' in x and 'com.example.widgethost' in x for x in state.splitlines()) and 'Application Not Responding' not in state)
 (out/(n+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p']))
def host(pkg,mode,tap=None):
 args=['shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','mode',mode]
 if tap:args+=['--es','tapKind',tap]
 adb(*args)
def prefs(pkg,lang,extra=''):
 adb('shell','am','force-stop',pkg);folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
 app=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
 fajr='<map><float name="latitude" value="30.8619"/><float name="longitude" value="29.5845"/><string name="city_name">New Borg El Arab</string><string name="city_name_ar">برج العرب الجديدة</string><boolean name="use_current_location" value="false"/><int name="calculation_method" value="1"/>'+extra+'</map>'
 for f,x in [('app_prefs',app),('fajr_prefs',fajr)]:adb('shell',f"echo '{x}' > {folder}/{f}.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/{f}.xml; chmod 660 {folder}/{f}.xml; restorecon -R {folder}")
adb('wait-for-device');adb('root');time.sleep(4);adb('wait-for-device')
for a in ['original.apk','comparison.apk','widgethost.apk']:check(a+' installed','Success' in adb('install','-r','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420')
adb('shell','settings','put','global','auto_time','0');adb('shell','settings','put','global','auto_time_zone','0');adb('shell','setprop','persist.sys.timezone','Africa/Cairo')
adb('shell','am','force-stop','com.google.android.apps.nexuslauncher');adb('shell','input','keyevent','KEYCODE_BACK');adb('shell','appwidget','grantbind','--package','com.example.widgethost','--user','0')
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 prefs(pkg,'en');adb('shell','date','103112002026.00');adb('logcat','-c');host(pkg,'recover-homeB');time.sleep(6);shot(name+'-before');log=adb('logcat','-d');ids=re.findall(r'WidgetRecoveryAudit.*bound kind=[^ ]+ id=(\d+) ok=true',log);check(name+' four initial bindings',len(ids)==4);(out/(name+'-alarm-before.txt')).write_text(adb('shell','dumpsys','alarm'));adb('reboot');time.sleep(12);alive=subprocess.run(['pgrep','-f','qemu-system'],capture_output=True).returncode==0
 if not alive:
  (out/(name+'-emulator-restart.txt')).write_text('Emulator exited on adb reboot; restarting same AVD disk without wipe-data.');subprocess.Popen([os.environ['ANDROID_HOME']+'/emulator/emulator','-port','5554','-avd','test','-no-window','-gpu','swiftshader_indirect','-no-snapshot','-noaudio','-no-boot-anim'],stdout=(out/(name+'-coldboot-log.txt')).open('w'),stderr=subprocess.STDOUT)
 subprocess.run(['adb','wait-for-device'],capture_output=True,timeout=240)
 for attempt in range(90):
  if adb('shell','getprop','sys.boot_completed').strip()=='1':break
  time.sleep(2)
 adb('root');time.sleep(3);adb('wait-for-device');adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard');time.sleep(15);(out/(name+'-alarm-afterboot.txt')).write_text(adb('shell','dumpsys','alarm'));(out/(name+'-appwidgets-afterboot.txt')).write_text(adb('shell','dumpsys','appwidget'));adb('logcat','-c');adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','operation','resume');time.sleep(5);shot(name+'-after');log=adb('logcat','-d');(out/(name+'-resume-log.txt')).write_text(log);restored=re.findall(r'WidgetRebootAudit.*reattach id=(\d+)',log);check(name+' same four ids after reboot',sorted(restored)==sorted(ids));check(name+' no renderer crash',not any(t in log for t in ['FATAL EXCEPTION','RemoteViews$ActionException']));adb('shell','am','start','-S','-W','-n','com.example.widgethost/.MainActivity','--es','package',pkg,'--es','operation','delete');time.sleep(2)
(out/'checks.json').write_text(json.dumps(checks,indent=2));print('checks',len(checks),'fails',sum(not x['passed'] for x in checks));assert all(x['passed'] for x in checks)
