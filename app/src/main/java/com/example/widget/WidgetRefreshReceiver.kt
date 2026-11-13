package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.lockwidgets.HomeWidgets

/**
 * 3.4.2: tap on a Home countdown/progress widget whose tap option is "تحديث العرض". Refreshes the
 * widgets on demand (they can lag with the non-wakeup minute pulse) and shows the just-rendered
 * remaining time in a standard native Toast - the exact same text the widget shows.
 */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != ACTION) return
        val id = intent.getIntExtra("appWidgetId", -1)
        var kind: String? = null
        try {
            val cn = AppWidgetManager.getInstance(context).getAppWidgetInfo(id).provider
            val cls = cn.className
            kind = cls.substring("com.example.widget.".length, cls.length - "WidgetProvider".length)
        } catch (e: Exception) {
            Log.w("WathakkerWidgetRefresh", "provider lookup failed for id $id", e)
        }
        val text = HomeWidgets.refreshAllAndText(context, kind ?: "")
        Log.i("WathakkerWidgetRefresh", "tap id=$id kind=$kind text=$text")
        Toast.makeText(context.applicationContext, text, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val ACTION = "com.wathakker.widget.REFRESH"
    }
}
