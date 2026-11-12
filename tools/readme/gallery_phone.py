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

def dismiss_dialogs():
    """Clear ANR (Pixel Launcher isn't responding) and system permission prompts."""
    for _ in range(3):
        x = dump()
        if "isn" in x and 'responding' in x:
            for label in ['Wait', 'انتظار', 'Close app', 'إغلاق التطبيق']:
                if label in x:
                    _tap_in(x, label)
                    time.sleep(2)
                    adb('shell', 'am', 'force-stop', 'com.google.android.apps.nexuslauncher')
                    break
            continue
        if "Don't allow" in x or 'عدم السماح' in x:
            for label in ['Allow all the time', 'While using the app', 'Allow only while using the app', 'Allow', 'ALLOW', 'السماح', 'السماح دائمًا']:
                if label in x:
                    _tap_in(x, label)
                    time.sleep(2)
                    break
            continue
        break

def _tap_in(x, label):
    try:
        root = ET.fromstring(x)
    except Exception:
        return
    ns = list(root.iter('node'))
    parents = {c: p for p in ns for c in p}
    for n in ns:
        if n.get('text', '') != label and n.get('content-desc', '') != label:
            continue
        t2 = n
        while t2 is not None and t2.get('clickable') != 'true' and t2 in parents:
            t2 = parents[t2]
        target = t2 if t2 is not None else n
        cx, cy = center(target.get('bounds'))
        adb('shell', 'input', 'tap', str(cx), str(cy))
        return

def shot(n):
    dismiss_dialogs()
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
        dismiss_dialogs()
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


def top_xml():
    return dump()

def in_app(x):
    return 'package="com.wathakker.app"' in x

def at_main(x):
    return in_app(x) and 'حصن المسلم' in x and 'التذكيرات' in x

def home():
    """Return to a main tab without ever exiting the app; relaunch if backgrounded."""
    for _ in range(6):
        x = top_xml()
        if at_main(x):
            return True
        if not in_app(x):
            adb('shell', 'am', 'start', '-W', '-n', PKG + '/com.example.MainActivity')
            time.sleep(6)
            continue
        adb('shell', 'input', 'keyevent', 'KEYCODE_BACK'); time.sleep(1.5)
    print('home() failed', flush=True)
    return False

def relaunch():
    adb('shell', 'am', 'start', '-W', '-n', PKG + '/com.example.MainActivity')
    time.sleep(6)

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
adb('shell', 'am', 'force-stop', 'com.google.android.apps.nexuslauncher')
print('install:', 'Success' in adb('install', '-r', '-g', 'app-debug.apk', t=300), flush=True)
adb('shell', 'wm', 'size', '1080x2400'); adb('shell', 'wm', 'density', '420')
adb('shell', 'settings', 'put', 'global', 'hide_error_dialogs', '1')
adb('shell', 'settings', 'put', 'global', 'auto_time', '0')
adb('shell', 'settings', 'put', 'global', 'auto_time_zone', '0')
adb('shell', 'setprop', 'persist.sys.timezone', 'Africa/Cairo')
adb('shell', 'date', '103016222026.00')
# ---- Phase 1: onboarding (01 language, 02 calculation, 03 alerts) ----
adb('shell', 'pm', 'clear', PKG); time.sleep(2)
# grants must come AFTER pm clear (clear wipes them)
for p in ['POST_NOTIFICATIONS', 'ACCESS_COARSE_LOCATION', 'ACCESS_FINE_LOCATION']:
    adb('shell', 'pm', 'grant', PKG, 'android.permission.' + p)
adb('shell', 'appops', 'set', PKG, 'MANAGE_EXTERNAL_STORAGE', 'allow')
launch(3)  # first launch creates /data/data dirs
dismiss_dialogs()
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
# drawer is still open here; tap items directly (a back press at root would exit the app)
tap('منبه صلاة الفجر', exact=True); time.sleep(2.5)
shot('05-fajr-alarm')
home()

tap('حصن المسلم', exact=True); time.sleep(2)
shot('06-hisn')

tap('التذكيرات', exact=True); time.sleep(2)
shot('07-reminders')

tap('التقويم', exact=True); time.sleep(2)
shot('08-calendar')

home()
tap('القائمة', exact=True); time.sleep(2)
tap('القبلة', exact=True); time.sleep(2.5)
shot('09-qibla')
home()

tap('القائمة', exact=True); time.sleep(2)
tap('محول التاريخ', exact=True); time.sleep(2.5)
shot('12-converter-g2h')
if not tap('من هجري إلى ميلادي'):
    tap('هجري إلى ميلادي')
time.sleep(2)
shot('13-converter-h2g')
home()

# 11: One UI picker dialog (appearance theme picker)
tap('القائمة', exact=True); time.sleep(2)
tap('الإعدادات', exact=True); time.sleep(2.5)
shot('11-settings-page')
tap('المظهر'); time.sleep(2)
shot('11-dialog')
tap('إلغاء', exact=True)
home()

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
home()

# fallback candidates for 02/03 inside the app proper
tap('القائمة', exact=True); time.sleep(2)
tap('الإعدادات', exact=True); time.sleep(2)
if tap('تغيير الموقع'):
    time.sleep(1.5)
    shot('02-location-page')
    if tap('طريقة الحساب'):
        time.sleep(1.5)
        shot('02-calc-dialog')
        tap('إلغاء', exact=True)
home()

(out/'date.txt').write_text(adb('shell', 'date'))
(out/'logcat.txt').write_text(adb('logcat', '-d', t=120))
print('done', flush=True)
