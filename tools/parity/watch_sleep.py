"""Original/rebuild minute wake and boundary test under forced device idle.
Uses each APK's existing PreviewActivity real-provider request hook, not a fake alarm.
"""
import subprocess,time,pathlib,json
out=pathlib.Path('evidence');out.mkdir(exist_ok=True)
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=60)
 return r.stdout.decode(errors='replace').strip()+'\n'+r.stderr.decode(errors='replace').strip()
def save(n,s): (out/n).write_text(s)
def shot(n): (out/n).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=45))
adb('wait-for-device')
for i in range(90):
 if adb('shell','getprop','sys.boot_completed').strip()=='1':break
 time.sleep(3)
time.sleep(30);adb('root');time.sleep(4);adb('wait-for-device')
adb('shell','settings','put','global','hide_error_dialogs','1')
adb('shell','settings','put','global','auto_time','0')
adb('shell','settings','put','global','auto_time_zone','0')
adb('shell','setprop','persist.sys.timezone','Africa/Cairo')
results={}
for build in ['original','rebuilt']:
 adb('shell','cmd','deviceidle','unforce');adb('shell','dumpsys','battery','reset');adb('uninstall','com.wathakker.app')
 save(build+'-install.txt',adb('install','-g',f'watch-{build}.apk'))
 adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard')
 adb('shell','svc','power','stayon','true')
 # Fixed weekday/date and identical local prayer context. Boundary hook uses current time.
 adb('shell','date','102910002026.00')
 adb('logcat','-c')
 save(build+'-start.txt',adb('shell','am','start','-W','-n','com.wathakker.app/com.wathakker.wear.PreviewActivity','--es','mode','ranged','--ei','request_id','101','--es','req_type','ranged','--es','spoof_cls','com.wathakker.wear.PrayerComplicationService','--ei','boundary_sec','90','--ei','countdown_sec','180'))
 time.sleep(6);shot(build+'-countdown-awake.png')
 save(build+'-alarm-before.txt',adb('shell','dumpsys','alarm'))
 save(build+'-log-before.txt',adb('logcat','-d','-s','WathakkerCd:I','*:S'))
 adb('shell','svc','power','stayon','false');adb('shell','dumpsys','battery','unplug');adb('shell','input','keyevent','KEYCODE_SLEEP')
 save(build+'-idle-enter.txt',adb('shell','cmd','deviceidle','force-idle'))
 save(build+'-idle-before.txt',adb('shell','dumpsys','deviceidle'))
 time.sleep(145)
 save(build+'-idle-after.txt',adb('shell','dumpsys','deviceidle'))
 log=adb('logcat','-d','-s','WathakkerCd:I','*:S');save(build+'-sleep-log.txt',log)
 save(build+'-alarm-after.txt',adb('shell','dumpsys','alarm'))
 save(build+'-batterystats.txt',adb('shell','dumpsys','batterystats','com.wathakker.app'))
 adb('shell','cmd','deviceidle','unforce');adb('shell','dumpsys','battery','reset');adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard');time.sleep(3)
 shot(build+'-countdown-after.png')
 results[build]={'boundary_fire_observed':'boundary fire' in log,'legacy_tick_events':[l for l in log.splitlines() if 'tick' in l.lower()]}
 # Capture all six actual provider renderings for visual inspection.
 for k in [0,1,2,3,4,8]:
  save(f'{build}-provider-{k}-start.txt',adb('shell','am','start','-W','-n','com.wathakker.app/com.wathakker.wear.PreviewActivity','--es','mode','short','--ei','kind',str(k)))
  time.sleep(2);shot(f'{build}-provider-{k}.png')
 save(build+'-package.txt',adb('shell','dumpsys','package','com.wathakker.app'))
(out/'sleep-results.json').write_text(json.dumps(results,indent=2))
print(json.dumps(results,indent=2))
