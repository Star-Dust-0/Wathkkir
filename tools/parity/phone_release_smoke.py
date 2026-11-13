"""Smoke-verify the signed release APK (Wathkkir-phone-*.apk in the workspace) on an emulator:
install, confirm version 3.5.2 (84) for com.wathakker.app, launch the prayer screen,
and open the Friday alert sheet to confirm the Jumu'a surface (no iqama option).
Uses uiautomator dump directly; no helper apps.
"""
import subprocess,time,pathlib,json,re,glob,xml.etree.ElementTree as ET
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a,timeout=50):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=timeout);return (r.stdout+r.stderr).decode(errors='replace')
def check(n,ok,detail=''):
 checks.append(dict(name=n,passed=bool(ok),detail=str(detail)[:600]));(out/'checks.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2));print(n,ok,str(detail)[:300],flush=True);return bool(ok)
def shot(name):
 (out/(name+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=25))
def tree():
 adb('shell','uiautomator','dump','/sdcard/w.xml',timeout=40)
 return adb('shell','cat','/sdcard/w.xml')
def find(desc):
 for n in ET.fromstring(tree()).iter('node'):
  if n.get('content-desc')==desc or n.get('text')==desc:return n
 return None
def tap_node(n):
 x0,y0,x1,y1=map(int,re.findall(r'\d+',n.get('bounds','')))
 adb('shell','input','tap',str((x0+x1)//2),str((y0+y1)//2));time.sleep(1.2);return True
def longpress_node(n):
 x0,y0,x1,y1=map(int,re.findall(r'\d+',n.get('bounds','')))
 adb('shell','input','swipe',str(x0),str(y0),str(x0),str(y0),'1400');time.sleep(2);return True

apk=sorted(glob.glob('Wathkkir-phone-*.apk'))
check('release artifact present',len(apk)==1,str(apk))
PKG='com.wathakker.app'
adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device')
if apk:
 check('install signed release','Success' in adb('install','-r','-g',apk[0],timeout=120))
 dump=adb('shell','pm','dump',PKG,timeout=60)
 (out/'package-dump.txt').write_text(dump)
 vn=re.search(r'versionName=([\d.]+)',dump)
 vc=re.search(r'versionCode=(\d+)',dump)
 check('versionName is 3.5.2',vn and vn.group(1)=='3.5.2',vn.group(1) if vn else 'missing')
 check('versionCode is 84',vc and vc.group(1)=='84',vc.group(1) if vc else 'missing')
 adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','settings','put','global','hide_error_dialogs','1')
 adb('shell','setprop','persist.sys.timezone','Africa/Cairo');time.sleep(1)
 for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',PKG,'android.permission.'+perm)
 adb('shell','cmd','uimode','night','no')
 folder=f'/data/data/{PKG}/shared_prefs';adb('shell','mkdir','-p',folder)
 x='<map><string name="language">ar</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><string name="theme_mode">light</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
 adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{PKG}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
 adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
 adb('shell','am','start','-W','-n',PKG+'/com.example.MainActivity',timeout=60)
 time.sleep(9)
 n=find('Close app')
 if n is not None: tap_node(n)
 shot('release-prayer')
 tr=tree();(out/'release-prayer.xml').write_text(tr)
 check('prayer screen renders','مواعيد الصلاة' in tr or 'الفجر' in tr,str(len(tr)))
 b=find('bell_2')
 check('bell_2 found',b is not None)
 if b is not None:
  # The pre-adhan controls intentionally show only while the prayer bell is enabled.
  tap_node(b)
  b=find('bell_2')
  assert b is not None, 'bell missing after enable'
  longpress_node(b)
  tr2=tree();(out/'release-sheet.xml').write_text(tr2)
  shot('release-sheet')
  check('sheet title is Jumu\'a (Friday)','الجمعة' in tr2,'الجمعة' if 'الجمعة' in tr2 else tr2[:150])
  check('Friday sheet hides iqama row','تنبيه الإقامة' not in tr2)
  check('Friday sheet keeps pre-adhan row','تنبيه قبل الأذان' in tr2)
print('done',flush=True)

assert all(x["passed"] for x in checks), "Smoke assertions failed; see checks.json"
