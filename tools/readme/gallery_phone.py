"""README gallery capture: phone. Deterministic date/location, Arabic UI, dark theme.
Captures every screen with a hierarchy dump so failures are diagnosable.
Runs only inside an isolated CI emulator; no network disclosure or real-device changes.
"""
import subprocess, time, pathlib, re, xml.etree.ElementTree as ET

out = pathlib.Path('evidence'); out.mkdir(exist_ok=True)
PKG = 'com.wathakker.app'

def adb(*a, t=60):
    r = subprocess.run(['adb', *a], capture_output=True, timeout=t)
    return (r.stdout + r.stderr).decode(errors='replace')

def dump():
    adb('shell', 'uiautomator', 'dump', '/sdcard/_ui.xml')
    return adb('shell', 'cat', '/sdcard/_ui.xml')

def shot(n):
    (out/(n+'.xml')).write_text(dump())
    (out/(n+'.png')).write_bytes(subprocess.check_output(['adb', 'exec-out', 'screencap', '-p'], timeout=30))
    print('shot', n, flush=True)

def nodes():
    try:
        return list(ET.fromstring(dump()).iter('node'))
    except Exception:
        return []

def center(b):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', b))
    return (x1+x2)//2, (y1+y2)//2

def tap_xy(x, y, settle=1.2):
    adb('shell', 'input', 'tap', str(x), str(y)); time.sleep(settle)

def tap(text, exact=False, settle=1.2):
    """Tap the visible node whose text or content-desc matches, climbing to the clickable parent."""
    for _ in range(3):
        ns = nodes()
        parents = {c: p for p in ns for c in p}
        for n in ns:
            t, d = n.get('text', ''), n.get('content-desc', '')
            ok = (t == text or d == text) if exact else (text in t or text in d)
            if not ok or n.get('enabled', 'true') != 'true':
                continue
            t2 = n
            while t2 is not None and t2.get('clickable') != 'true' and t2 in parents:
                t2 = parents[t2]
            target = t2 if t2 is not None and t2.get('clickable') == 'true' else n
            x, y = center(target.get('bounds'))
            if y > 60:
                tap_xy(x, y, settle)
                return True
        time.sleep(1)
    print('tap miss:', text, flush=True)
    (out/('miss-' + re.sub(r'\W+', '_', text) + '.xml')).write_text(dump())
    return False

def visible(*texts):
    x = dump()
    return all(t in x for t in texts)

def back(n=1):
    for _ in range(n):
        adb('shell', 'input', 'keyevent', 'KEYCODE_BACK'); time.sleep(.8)

def go_home():
    for _ in range(5):
        if visible('القائمة') and visible('الصلاة'):
            return True
        back()
    return False

def set_prefs(files):
    folder = f'/data/data/{PKG}/shared_prefs'
    adb('shell', 'mkdir', '-p', folder)
    for name, x in files.items():
        adb('shell', f"echo '{x}' > {folder}/{name}.xml; chown $(stat -c %u:%g /data/data/{PKG}) {folder}/{name}.xml; chmod 660 {folder}/{name}.xml; restorecon -R {folder}")

def launch(wait=6):
    adb('shell', 'am', 'force-stop', PKG)
    adb('shell', 'am', 'start', '-W', '-n', PKG + '/com.example.MainActivity')
    time.sleep(wait)

APP = '<map><string name="language">ar</string><string name="digits_style">latin</string></map>'
FAJR = ('<map><float name="latitude" value="30.0444"/><float name="longitude" value="31.2357"/>'
        '<string name="city_name">القاهرة</string><string name="city_name_ar">القاهرة</string>'
        '<string name="city_name_en">Cairo</string><string name="country_name">مصر</string>'
        '<boolean name="use_current_location" value="false"/><int name="calculation_method" value="1"/>'
        '<int name="asr_juristic" value="0"/><int name="high_lat_rule" value="3"/></map>')

adb('wait-for-device')
adb('root'); time.sleep(3); adb('wait-for-device')
print('install:', 'Success' in adb('install', '-r', '-g', 'app-debug.apk', t=300), flush=True)
adb('shell', 'wm', 'size', '1080x2400'); adb('shell', 'wm', 'density', '420')
adb('shell', 'settings', 'put', 'global', 'hide_error_dialogs', '1')
adb('shell', 'settings', 'put', 'global', 'auto_time', '0')
adb('shell', 'settings', 'put', 'global', 'auto_time_zone', '0')
adb('shell', 'setprop', 'persist.sys.timezone', 'Africa/Cairo')
adb('shell', 'date', '103016222026.00')
for p in ['POST_NOTIFICATIONS', 'ACCESS_COARSE_LOCATION', 'ACCESS_FINE_LOCATION']:
    adb('shell', 'pm', 'grant', PKG, 'android.permission.' + p)

# ---- Phase 1: onboarding (01 language, 02 calculation, 03 alerts) ----
adb('shell', 'pm', 'clear', PKG); time.sleep(2)
launch(3)  # first launch creates /data/data dirs
set_prefs({'app_prefs': APP})
launch(6)
shot('01-language')
# jump straight to the calculation page (onb_page is persisted)
set_prefs({'app_prefs': APP.replace('</map>', '<int name="onb_page" value="3"/></map>')})
launch(5)
shot('02-calculation')
set_prefs({'app_prefs': APP.replace('</map>', '<int name="onb_page" value="5"/></map>')})
launch(5)
shot('03-alerts')

# ---- Phase 2: main app ----
main_prefs = APP.replace('</map>',
    '<boolean name="onboarding_done" value="true"/><boolean name="perm_flow_done" value="true"/>'
    '<string name="theme_mode">dark</string></map>')
set_prefs({'app_prefs': main_prefs, 'fajr_prefs': FAJR})
launch(9)
if not visible('الصلاة'):
    shot('home-unexpected')
tap('الصلاة', exact=True)
time.sleep(2)
shot('04-prayer')

tap('القائمة', exact=True); time.sleep(1.5)
shot('10-menu')
back()

tap('القائمة', exact=True); time.sleep(1)
tap('منبه صلاة الفجر'); time.sleep(2)
shot('05-fajr-alarm')
go_home()

tap('حصن المسلم', exact=True); time.sleep(2)
shot('06-hisn')

tap('التذكيرات', exact=True); time.sleep(2)
shot('07-reminders')

tap('التقويم', exact=True); time.sleep(2)
shot('08-calendar')

go_home()
tap('القائمة', exact=True); time.sleep(1)
tap('القبلة'); time.sleep(2)
shot('09-qibla')
go_home()

tap('القائمة', exact=True); time.sleep(1)
tap('محوّل التاريخ'); time.sleep(2)
shot('12-converter-g2h')
if not tap('من هجري إلى ميلادي'):
    tap('هجري إلى ميلادي')
time.sleep(1.5)
shot('13-converter-h2g')
go_home()

# 11: One UI picker dialog (appearance theme picker)
tap('القائمة', exact=True); time.sleep(1)
tap('الإعدادات'); time.sleep(2)
shot('11-settings-page')
tap('المظهر'); time.sleep(1.5)
shot('11-dialog')
tap('إلغاء', exact=True)
go_home()

# 14: reminder time wheel. Try azkar options route first, then reminders route.
tap('التذكيرات', exact=True); time.sleep(2)
ok = False
if tap('خيارات'):
    if tap('أوقات التنبيه'):
        if tap('إضافة وقت تنبيه'):
            time.sleep(1.5); shot('14-time-picker'); ok = True
if not ok:
    (out/'14-fallback.xml').write_text(dump())
    shot('14-time-picker-candidate')
back(3)
go_home()

# fallback candidates for 02/03 inside the app proper
tap('القائمة', exact=True); time.sleep(1)
tap('الإعدادات'); time.sleep(1.5)
if tap('تغيير الموقع'):
    time.sleep(1.5)
    shot('02-location-page')
    if tap('طريقة الحساب'):
        time.sleep(1.5)
        shot('02-calc-dialog')
        tap('إلغاء', exact=True)
go_home()

(out/'date.txt').write_text(adb('shell', 'date'))
(out/'logcat.txt').write_text(adb('logcat', '-d', t=120))
print('done', flush=True)
