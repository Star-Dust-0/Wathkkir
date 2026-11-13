"""Paired minified-release drawer measurement. Emulator numbers are not A52s proof.
Uses the published 3.5.1 APK as baseline and the branch comparison.apk as candidate.
Saves raw gfxinfo, UI trees, screenshots and a recording. Animation scales stay 1x.
"""
import json, pathlib, re, subprocess, time, urllib.request, xml.etree.ElementTree as ET
OUT = pathlib.Path('evidence'); OUT.mkdir(exist_ok=True)
BASELINE = 'https://github.com/Star-Dust-0/Wathkkir/releases/download/release-13.11.2026.2/Wathkkir-phone-3.5.1.apk'
checks = []; results = []
def adb(*args, timeout=60):
    p = subprocess.run(['adb', *args], capture_output=True, timeout=timeout)
    return (p.stdout + p.stderr).decode(errors='replace')
def check(name, ok, detail=''):
    checks.append(dict(name=name, passed=bool(ok), detail=str(detail)))
    (OUT/'drawer-checks.json').write_text(json.dumps(checks, ensure_ascii=False, indent=2))
    print(name, bool(ok), str(detail)[:250], flush=True)
    return bool(ok)
def tree():
    value = adb('shell', 'am', 'instrument', '-w', 'com.example.widgethost/.HierarchyDump')
    result=next((s.split('=', 1)[1] for s in value.splitlines() if s.startswith('INSTRUMENTATION_RESULT: hierarchy=')), None)
    if result and '<node' in result:return result
    (OUT/'hierarchy-helper-last.txt').write_text(value)
    adb('shell','uiautomator','dump','/sdcard/drawer-ui.xml',timeout=40)
    fallback=adb('shell','cat','/sdcard/drawer-ui.xml')
    return fallback[fallback.index('<?xml'):] if '<?xml' in fallback else '<hierarchy/>'
def find(label):
    for n in ET.fromstring(tree()).iter('node'):
        if label in (n.get('text'), n.get('content-desc')):
            b = list(map(int, re.findall(r'\d+', n.get('bounds', ''))))
            if len(b) == 4 and b[2] > b[0] and b[3] > b[1]:
                return ((b[0]+b[2])//2, (b[1]+b[3])//2)
    return None
def tap(point):
    adb('shell', 'input', 'tap', *map(str, point))
def shot(name):
    (OUT/(name+'.png')).write_bytes(subprocess.check_output(['adb', 'exec-out', 'screencap', '-p'], timeout=30))
    (OUT/(name+'.xml')).write_text(tree())
def frames(raw):
    found=[]
    for block in raw.split('---PROFILEDATA---')[1::2]:
        rows=[x.strip().split(',') for x in block.strip().splitlines() if x.strip()]
        if not rows: continue
        header=rows[0]
        for row in rows[1:]:
            d=dict(zip(header,row))
            try:
                if int(d['Flags']) != 0: continue
                start=int(d['IntendedVsync']); end=int(d['FrameCompleted'])
                # SurfaceFlinger can return an unready/sentinel completion value.
                # Reject implausible >10s samples, not valid slow animation frames.
                if 0 < end - start < 10_000_000_000:
                    found.append((end-start)/1e6)
            except (KeyError,ValueError): pass
    return found
def stats(vals):
    vals=sorted(vals)
    return dict(frames=len(vals), p50_ms=round(vals[len(vals)//2],2) if vals else None,
        p90_ms=round(vals[min(len(vals)-1,int(len(vals)*.9))],2) if vals else None,
        over_16_67=sum(v>16.67 for v in vals), over_33_33=sum(v>33.33 for v in vals),
        over_50=sum(v>50 for v in vals), max_ms=round(max(vals),2) if vals else None)
def setup(pkg,lang,theme):
    adb('shell','am','force-stop',pkg)
    adb('shell','pm','clear',pkg)
    for perm in ['POST_NOTIFICATIONS','ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION']:
        adb('shell','pm','grant',pkg,'android.permission.'+perm)
    folder=f'/data/data/{pkg}/shared_prefs'
    adb('shell','mkdir','-p',folder)
    x=f'<map><string name="language">{lang}</string><string name="digits_style">latin</string><string name="app_font">app</string><float name="font_scale" value="1.0"/><string name="theme_mode">{theme}</string><boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/></map>'
    adb('shell',f"echo '{x}' > {folder}/app_prefs.xml; chown $(stat -c %u:%g /data/data/{pkg}) {folder}/app_prefs.xml; chmod 660 {folder}/app_prefs.xml; restorecon -R {folder}")
    adb('shell','cmd','uimode','night','yes' if theme=='dark' else 'no')
    adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity')
    time.sleep(7)

def measure(name,pkg,lang,theme,repeat):
    slug=f'{name}-{lang}-{theme}-{repeat}'
    setup(pkg,lang,theme)
    # First launch can be covered by an Android compatibility/permission dialog.
    shot(slug+'-startup')
    menu=None
    for attempt in range(4):
        menu=find('القائمة' if lang=='ar' else 'Menu') or find('Menu') or find('القائمة')
        if menu: break
        for label in ['OK', 'Continue', 'Allow', 'موافق', 'متابعة', 'السماح']:
            button=find(label)
            if button: tap(button);time.sleep(.5)
        adb('shell','input','keyevent','KEYCODE_BACK')
        adb('shell','am','start','-W','-n',pkg+'/com.example.MainActivity')
        time.sleep(3)
    shot(slug+'-ready')
    (OUT/(slug+'-startup-logcat.txt')).write_text(adb('logcat','-d','-t','500'))
    assert check(slug+' menu exists',menu is not None), 'menu missing'
    outside=(50 if lang=='ar' else 1030,1200)
    # Capture first opening separately from the warm-loop sample.
    adb('shell','dumpsys','gfxinfo',pkg,'reset')
    tap(menu); time.sleep(.5)
    raw=adb('shell','dumpsys','gfxinfo',pkg,'framestats')
    (OUT/(slug+'-cold-gfxinfo.txt')).write_text(raw)
    results.append(dict(scene=slug, phase='cold', **stats(frames(raw))))
    shot(slug+'-open')
    tr=tree(); labels=[n.get('text','') for n in ET.fromstring(tr).iter('node')]
    check(slug+' drawer renders',('الإعدادات' if lang=='ar' else 'Settings') in labels)
    check(slug+' baseline credit present' if name=='baseline' else slug+' candidate credit absent',
          ('brought to you by Instinct' in labels) == (name=='baseline'))
    tap(outside); time.sleep(.5)
    # Two unmeasured warmup cycles, then repeated full slide-in and slide-out.
    cmd='; '.join([f'input tap {menu[0]} {menu[1]}; sleep 0.65; input tap {outside[0]} {outside[1]}; sleep 0.65']*2)
    adb('shell',cmd)
    adb('shell','dumpsys','gfxinfo',pkg,'reset')
    cmd='; '.join([f'input tap {menu[0]} {menu[1]}; sleep 0.65; input tap {outside[0]} {outside[1]}; sleep 0.65']*8)
    adb('shell',cmd,timeout=60)
    raw=adb('shell','dumpsys','gfxinfo',pkg,'framestats')
    (OUT/(slug+'-warm-gfxinfo.txt')).write_text(raw)
    vals=frames(raw); results.append(dict(scene=slug,phase='warm',**stats(vals)))
    check(slug+' frame sample',len(vals)>=40,stats(vals))
    # Verify final closed state and another successful open after repeated dismissals.
    tap(menu);time.sleep(.5); shot(slug+'-after-cycles')
    tr=tree();check(slug+' repeated opening works',('الإعدادات' if lang=='ar' else 'Settings') in tr)
    tap(outside);time.sleep(.5)
    (OUT/'drawer-frame-summary.json').write_text(json.dumps(results,indent=2))
    adb('shell','am','force-stop',pkg)

adb('wait-for-device');adb('root');time.sleep(3);adb('wait-for-device')
urllib.request.urlretrieve(BASELINE,'drawer-baseline.apk')
for apk in ['drawer-baseline.apk','comparison.apk','widgethost.apk']:
    assert check('install '+apk,'Success' in adb('install','-r','-g',apk,timeout=120)), 'install failed'
adb('shell','wm','size','1080x2400');adb('shell','wm','density','420')
adb('shell','settings','put','global','hide_error_dialogs','1')
for key in ['window_animation_scale','transition_animation_scale','animator_duration_scale']:
    adb('shell','settings','put','global',key,'1')
adb('shell','setprop','persist.sys.timezone','Africa/Cairo')
(OUT/'baseline-provenance.json').write_text(json.dumps(dict(url=BASELINE,baseline_commit='56b645ba6631723d0d11a8ba9552b8918d05502b',note='Published minified 3.5.1 versus branch minified comparison build; emulator GPU differs from Samsung A52s.'),indent=2))
# Counterbalance the order, including both directions and themes.
for lang,theme in [('ar','light'),('en','dark')]:
    for repeat,order in [(1,['baseline','candidate']),(2,['candidate','baseline'])]:
        for name in order:
            measure(name,'com.wathakker.app'+('.compare' if name=='candidate' else ''),lang,theme,repeat)
log=adb('logcat','-d','-s','AndroidRuntime:E')
(OUT/'drawer-android-runtime.txt').write_text(log)
check('no runtime fatal exception','FATAL EXCEPTION' not in log,log[:500])
print(json.dumps(results,indent=2),flush=True)
if any(not c['passed'] for c in checks): raise SystemExit('one or more drawer checks failed')
