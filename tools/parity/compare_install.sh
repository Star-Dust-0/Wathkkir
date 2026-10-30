#!/usr/bin/env bash
set -euo pipefail
kind=$1
adb wait-for-device
adb install -g original.apk | tee evidence/install-original.txt
adb install -g Wathkkir-$kind-test.apk | tee evidence/install-comparison.txt
original=com.wathakker.app
comparison=com.wathakker.app.compare
adb shell pm list packages | tr -d '\r' | grep -x "package:$original"
adb shell pm list packages | tr -d '\r' | grep -x "package:$comparison"
for pkg in "$original" "$comparison"; do
  adb shell dumpsys package "$pkg" > "evidence/package-$pkg.txt"
done
cls=com.example.MainActivity
[ "$kind" != watch ] || cls=com.wathakker.wear.MainActivity
for pkg in "$original" "$comparison"; do
  adb shell input keyevent KEYCODE_WAKEUP
  adb shell wm dismiss-keyguard
  adb shell am start -W -n "$pkg/$cls" | tee "evidence/launch-$pkg.txt"
  sleep 10
  adb exec-out screencap -p > "evidence/$kind-$pkg.png"
  adb shell pidof "$pkg" | tee "evidence/pid-$pkg.txt"
  test -s "evidence/pid-$pkg.txt"
done
adb logcat -d > evidence/logcat.txt
