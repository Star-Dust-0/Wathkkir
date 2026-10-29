<div align="center">
  <img src="docs/icon.png" width="120" height="120" alt="App icon">
  <h1>Wathkkir (وذكّر)</h1>
  <p><b>An app that reminds you of prayer times and dhikr through your day and your night.</b></p>
  <p>Accurate prayer times for your location, per-prayer adhan and iqama alerts, a Qibla compass, a Hijri date converter, and an azkar library with statistics that track your daily commitment — in a modern One UI-inspired design.</p>

  <p>
    <img src="https://img.shields.io/badge/platform-Android-green?logo=android" alt="Android">
    <img src="https://img.shields.io/badge/platform-Wear%20OS-blue?logo=wearos" alt="Wear OS">
    <img src="https://img.shields.io/badge/UI%20languages-11-orange" alt="11 languages">
  </p>

  <p><b><a href="README.md">🇪🇬 النسخة العربية</a></b></p>
</div>

---

## 📑 Contents

- [Overview](#-overview)
- [Key features](#-key-features)
- [Latest additions](#-latest-additions)
- [Gallery](#-gallery)
- [Watch app](#-watch-app)
- [Supported languages](#-supported-languages)
- [Download](#-download)

---

## 🌙 Overview

Wathkkir is an open-source Android app that brings the everyday tools a Muslim needs into one place: astronomically accurate prayer times, fully customizable per-prayer alerts, morning and evening azkar with Hisn al-Muslim, a digital tasbeeh counter, and a Qibla compass — all with full right-to-left support and an interface translated into eleven languages.

## ✨ Key features

| Area | Features |
|---|---|
| 🕌 **Prayer times** | Automatic location via GPS or manual country/city selection, eight certified calculation methods (Egyptian General Authority, Umm al-Qura, Muslim World League, ISNA, University of Karachi, Kuwait, Qatar, Dubai), Asr juristic choice (Shafi'i or Hanafi), high-latitude rules, and manual per-prayer minute adjustments plus a Hijri date offset. |
| 🔔 **Prayer alerts** | An independent bell for each of the six prayer rows (including Shuruq), delivered as notifications, one-minute timing resolution (at adhan time or any number of minutes before, typed or dragged), **iqama reminder notifications** with per-prayer delays (Fajr 25 min, Dhuhr/Asr/Isha 20 min, Maghrib 10 min), **automatic Do-Not-Disturb** between adhan and iqama, and shared settings for notification sound and vibration. |
| 🧭 **Qibla compass** | An elegant dial that follows the system light/dark theme: a smoothly rotating tick ring with degree labels every 30°, a large heading readout with the direction name, a Kaaba marker at the Qibla bearing that turns green with a haptic tick within 3°, magnetic-declination correction to true north based on your location, and a calibration prompt when sensor accuracy drops. |
| 🕰️ **Optional times** | Optional prayer-screen rows: Imsak (10 minutes before Fajr), Duha, Midnight, and Qiyam (start of the last third of the night) — enabled from Settings as needed. |
| 📅 **Date converter** | One box with a One UI direction switch (Gregorian to Hijri or Hijri to Gregorian), a month list that follows the direction, and a centered "Today" button. Umm al-Qura calendar, weekday shown. |
| 📿 **Azkar & Hisn al-Muslim** | The complete Hisn al-Muslim library in chapters, custom azkar organized with colored tags, favorites, timed reminders, and preloaded prophetic duas. |
| 📊 **Statistics** | Daily commitment tracking: azkar read today, this week, and this month, plus a day-streak counter. |
| 🔢 **Tasbeeh** | A digital counter that keeps your progress even after the app is closed. |
| 📱 **Lock-screen widgets** | Fully customizable widgets (colors, background, text) showing prayer times on the lock screen. |
| 🎨 **Personalization** | Eleven UI languages with full RTL and LTR support, light/dark/system themes, and multiple Arabic fonts (Cairo, Naskh, Amiri, Ruqaa). |

<details>
<summary><b>🔔 More about the alert system</b></summary>

- Alerts arrive precisely on time and are automatically rescheduled after the device reboots.
- Each prayer has its own settings sheet via a long-press on its bell: timing, iqama reminder, and automatic DND.
- If the Do-Not-Disturb policy permission is missing, the app routes you to the system permission screen instead of silently pretending the feature works.

</details>

## 🆕 Latest additions

- **Fajr full-screen alarm** with its own settings page: tone or audio file, volume, vibration, ring before and/or after the adhan, ring duration, snooze length, test button.
- **One UI style menus and dialogs** everywhere, with Apply and Cancel on pickers. They follow the font size you choose.
- **Font size**: a 9-step slider in Settings > Appearance.
- **Hisn Al-Muslim**: search, "done reading" button, notes, counter colours, default favorites, add to my adhkar.
- **Per-prayer tone and vibration**, plus touch feedback only on controls that change state, following the haptics setting.
- **Widgets**: live previews in the picker on Android 15+.
- New onboarding, new icon, and the name Wathkkir (وذكّر) in every language.
- **Date converter redesigned**: one box, a One UI direction switch, and a "Today" button.
- **Time wheels**: the selected value sits inside the selection band, and AM/PM is a plain two-item wheel with no looping.
- **Page titles**: the gap above each title now matches the gap below it.

## 📸 Gallery

<div align="center">
  <table>
    <tr><td align="center"><img src="docs/screenshots/04-prayer.png" width="200" alt="Prayer screen"><br><sub>Prayer screen</sub></td><td align="center"><img src="docs/screenshots/05-fajr-alarm.png" width="200" alt="Fajr alarm"><br><sub>Fajr alarm</sub></td><td align="center"><img src="docs/screenshots/06-hisn.png" width="200" alt="Hisn Al-Muslim"><br><sub>Hisn Al-Muslim</sub></td></tr>
    <tr><td align="center"><img src="docs/screenshots/07-reminders.png" width="200" alt="Reminders"><br><sub>Reminders</sub></td><td align="center"><img src="docs/screenshots/08-calendar.png" width="200" alt="Calendar"><br><sub>Calendar</sub></td><td align="center"><img src="docs/screenshots/09-qibla.png" width="200" alt="Qibla"><br><sub>Qibla</sub></td></tr>
    <tr><td align="center"><img src="docs/screenshots/10-menu.png" width="200" alt="One UI style menus"><br><sub>One UI style menus</sub></td><td align="center"><img src="docs/screenshots/11-dialog.png" width="200" alt="Dialogs"><br><sub>Dialogs</sub></td><td align="center"><img src="docs/screenshots/01-language.png" width="200" alt="Onboarding: language"><br><sub>Onboarding: language</sub></td></tr>
    <tr><td align="center"><img src="docs/screenshots/02-calculation.png" width="200" alt="Calculation method"><br><sub>Calculation method</sub></td><td align="center"><img src="docs/screenshots/03-alerts.png" width="200" alt="Enable alerts"><br><sub>Enable alerts</sub></td><td align="center"><img src="docs/screenshots/14-time-picker.png" width="200" alt="Time wheel"><br><sub>Time wheel</sub></td></tr>
    <tr><td align="center"><img src="docs/screenshots/12-converter-g2h.png" width="200" alt="Date converter: Gregorian to Hijri"><br><sub>Gregorian to Hijri</sub></td><td align="center"><img src="docs/screenshots/13-converter-h2g.png" width="200" alt="Date converter: Hijri to Gregorian"><br><sub>Hijri to Gregorian</sub></td></tr>
  </table>
</div>

### 📲 Widgets

<div align="center">
  <table>
    <tr><td align="center"><img src="docs/screenshots/p01-lock-widgets.jpg" width="170" alt="Lock-screen widgets"><br><sub>Lock screen</sub></td><td align="center"><img src="docs/screenshots/p02-lock-widget-picker.jpg" width="170" alt="Lock widget picker"><br><sub>Lock widget picker</sub></td><td align="center"><img src="docs/screenshots/p03-lock-widget-picker-2.jpg" width="170" alt="Lock widgets"><br><sub>Lock widgets</sub></td><td align="center"><img src="docs/screenshots/p04-lock-widget-picker-3.jpg" width="170" alt="Lock widgets"><br><sub>Lock widgets</sub></td></tr>
    <tr><td align="center"><img src="docs/screenshots/p05-home-widgets.jpg" width="170" alt="Home-screen widgets"><br><sub>Home widgets</sub></td><td align="center"><img src="docs/screenshots/p06-home-widget-picker.jpg" width="170" alt="Home widget picker"><br><sub>Home widget picker</sub></td><td align="center"><img src="docs/screenshots/p07-widget-settings.jpg" width="170" alt="Widget settings"><br><sub>Widget settings</sub></td></tr>
  </table>
</div>

## ⌚ Watch app

The watch app is a separate download and runs on Wear OS 3 and newer. Its features:

- **Prayer times on your wrist**: the next prayer with its remaining time, both Hijri and Gregorian dates, and the full day list.
- **A Tile with a progress arc** showing the run-up to the next prayer, plus an iqama countdown after the adhan, with a preview in the tile picker.
- **Watch-face complications**: a next-prayer countdown with a progress ring, next prayer time, Hijri date, Gregorian date, or both dates together.
- **Adhan alert on the watch** with a choice of alert style: a regular notification or a full-screen notification.
- **Qibla compass** on the watch.
- **One UI style settings** organized in sections: Appearance (patterned backgrounds, theme colours, font style, and font size), Notifications, Language & location (sync with the phone or choose manually), and an "About this app" section showing the version.

<div align="center">
  <table>
    <tr><td align="center"><img src="docs/screenshots/w01-tile.png" width="170" alt="Watch tile"><br><sub>Watch tile</sub></td><td align="center"><img src="docs/screenshots/w02-main.png" width="170" alt="Main screen"><br><sub>Main screen</sub></td><td align="center"><img src="docs/screenshots/w03-countdown.png" width="170" alt="Countdown complication"><br><sub>Countdown complication</sub></td></tr>
    <tr><td align="center"><img src="docs/screenshots/w06-friday-times.jpg" width="170" alt="Friday prayer times"><br><sub>Friday prayer times</sub></td><td align="center"><img src="docs/screenshots/w05-theme-colors.jpg" width="170" alt="Theme colors"><br><sub>Theme colors</sub></td><td align="center"><img src="docs/screenshots/w04-qibla.jpg" width="170" alt="Qibla compass"><br><sub>Qibla compass</sub></td></tr>
  </table>
</div>

## 🌍 Supported languages

العربية · English · Français · Deutsch · Español · Italiano · Türkçe · اردو · Indonesia · Русский · Português

## 📥 Download

- [Releases page](https://github.com/Star-Dust-0/Wathkkir/releases) for phone and watch APKs when published.
- No release is published yet. Source recovery and original-app verification are in progress.

---

<div align="center">
  <br>
  <b><a href="https://instinct.com">brought to you by Instinct</a></b>
  <p>Mohamed Elnaggar</p>
  <p><i>Please keep us in your prayers.</i></p>
</div>
