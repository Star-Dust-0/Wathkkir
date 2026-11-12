"""README gallery capture: watch (Wear OS). Deterministic date (a Friday), Arabic UI.
Intent-driven pages only; no tapping. Isolated CI emulator only.
"""
import subprocess, time, pathlib

out = pathlib.Path('evidence'); out.mkdir(exist_ok=True)
PKG = 'com.wathakker.app'

def adb(*a, t=60):
    r = subprocess.run(['adb', *a], capture_output=True, timeout=t)
    return (r.stdout + r.stderr).decode(errors='replace')

def shot(n):
    (out/(n+'.png')).write_bytes(subprocess.check_output(['adb', 'exec-out', 'screencap', '-p'], timeout=30))
    print('shot', n, flush=True)

def start(component, *extras, wait=5):
    adb('shell', 'am', 'force-stop', PKG)
    adb('shell', 'am', 'start', '-W', '-n', PKG + '/' + component, *extras)
    time.sleep(wait)

adb('wait-for-device'); time.sleep(20)
rooted = '0' in adb('shell', 'id') or 'uid=0' in adb('root', t=20) or True
adb('root', t=20); time.sleep(3); adb('wait-for-device')
adb('shell', 'settings', 'put', 'global', 'auto_time', '0')
adb('shell', 'settings', 'put', 'global', 'auto_time_zone', '0')
adb('shell', 'setprop', 'persist.sys.timezone', 'Africa/Cairo')
print('date set:', adb('shell', 'date', '103016222026.00').strip(), flush=True)
print('install:', adb('install', '-r', '-g', 'watch-debug.apk', t=300).strip(), flush=True)
adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
adb('shell', 'wm', 'dismiss-keyguard')
adb('shell', 'svc', 'power', 'stayon', 'true')

# w02 main page, w06 Friday times list, w04 qibla, w05 theme colors (settings sub 2)
start('com.wathakker.wear.MainActivity', '--ei', 'page', '0', '--ei', 'lang', '1', wait=6)
shot('w02-main')
start('com.wathakker.wear.MainActivity', '--ei', 'page', '1', '--ei', 'lang', '1', wait=5)
shot('w06-friday-times')
start('com.wathakker.wear.MainActivity', '--ei', 'page', '2', '--ei', 'lang', '1', wait=5)
shot('w04-qibla')
start('com.wathakker.wear.MainActivity', '--ei', 'page', '3', '--ei', 'sub', '2', '--ei', 'lang', '1', wait=5)
shot('w05-theme-colors')

# w01 native tile (arc-accurate under the default en-US system locale)
start('com.wathakker.wear.PreviewActivity', '--es', 'mode', 'tile', '--ei', 'countdown_sec', '180', '--ez', 'native_tile', 'true', wait=7)
shot('w01-tile')

# w03 countdown complication, live state
start('com.wathakker.wear.PreviewActivity', '--es', 'mode', 'live', wait=5)
shot('w03-countdown')

(out/'date.txt').write_text(adb('shell', 'date'))
(out/'logcat.txt').write_text(adb('logcat', '-d', t=120))
print('done', flush=True)
