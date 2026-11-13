package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.example.lockwidgets.HomeWidgets

/** Base provider: refreshes widget content and the shared tick on lifecycle events. */
abstract class BaseWathakkerWidgetProvider : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        HomeWidgets.updateAll(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.updateAll(context)
        WidgetTickScheduler.schedule(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val e = context.getSharedPreferences("fajr_prefs", 0).edit()
        for (id in appWidgetIds) {
            e.remove("widget_bg_color_$id")
            e.remove("widget_bar_color_$id")
            e.remove("widget_tap_$id")
        }
        e.apply()
        // Re-evaluate the shared pulse: LockWidgets.schedule cancels it only when no widget of
        // ANY family remains, so deleting the last com.example.widget.* instance stops the pulse
        // immediately instead of at the next (possibly doze-deferred) tick delivery.
        WidgetTickScheduler.schedule(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdater.updateAll(context)
        WidgetTickScheduler.schedule(context)
    }
}

/** One UI prayer progress + date tile widget (3.2.2). */
class OneUIWidgetProvider : BaseWathakkerWidgetProvider()

class HeroWidgetProvider : BaseWathakkerWidgetProvider()

class NextPrayerWidgetProvider : BaseWathakkerWidgetProvider()

class PrayerBarWidgetProvider : BaseWathakkerWidgetProvider()
class ScheduleWidgetProvider : BaseWathakkerWidgetProvider()
class HijriWidgetProvider : BaseWathakkerWidgetProvider()
class GregorianWidgetProvider : BaseWathakkerWidgetProvider()
class DayPrayerWidgetProvider : BaseWathakkerWidgetProvider()
class PillWidgetProvider : BaseWathakkerWidgetProvider()
class ClockWidgetProvider : BaseWathakkerWidgetProvider()
class WideBarWidgetProvider : BaseWathakkerWidgetProvider()
class IqamaWidgetProvider : BaseWathakkerWidgetProvider()
class IqamaSmallWidgetProvider : BaseWathakkerWidgetProvider()
