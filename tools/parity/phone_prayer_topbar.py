"""Prayer-screen top-bar frosted-pill capture: original vs comparison, isolated emulator only.
No network disclosure, release, real-device data mutation, or host account changes.
Captures full screens and top-bar strips of the prayer tab in light+Arabic and dark+Arabic
so the restored header-pill blur can be inspected pixel by pixel against the original.
"""
import subprocess,time,pathlib,json
out=pathlib.Path('evidence');out.mkdir(exist_ok=True);checks=[]
def adb(*a):
 r=subprocess.run(['adb',*a],capture_output=True,timeout=50);return (r.stdout+r.stderr).decode(errors='replace')
def check(n,ok,detail=''):
 checks.append(dict(name=n,passed=bool(ok),detail=detail));(out/'checks.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2));print(n,ok,detail,flush=True);return bool(ok)
def shot(name):
 (out/(name+'.png')).write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=25))
def launch(pkg):
 adb('shell','am','force-stop',pkg)
 adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity');time.sleep(6)
adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device')
for a in ['original.apk','comparison.apk']:
 check('install '+a,'Success' in adb('install','-r','-g',a))
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420');adb('shell','settings','put','global','hide_error_dialogs','1')
for name,pkg in [('original','com.wathakker.app'),('comparison','com.wathakker.app.compare')]:
 for theme in ['light','dark']:
  slug=name+'-ar-'+theme
  adb('shell','pm','clear',pkg)
  for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:adb('shell','pm','grant',pkg,'android.permission.'+perm)
  adb('shell','cmd','uimode','night','no' if theme=='light' else 'yes')
  folder=f'/data/data/{pkg}/shared_prefs';adb('shell','mkdir','-p',folder)
  x=f'<map><string name="language">ar</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><string name="theme_mode">{theme}</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
  adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
  launch(pkg)
  shot(slug+'-prayer-top')
  check(slug+' captured',(out/(slug+'-prayer-top.png')).stat().st_size>10000)
print('done',flush=True)
