package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetTickScheduler.ACTION_TICK) {
            // Legacy alarm fired after upgrade: refresh once, then hand scheduling to the
            // consolidated LockWidgets pulse instead of re-arming this chain.
            WidgetUpdater.updateAll(context)
            WidgetTickScheduler.schedule(context)
        }
    }
}
