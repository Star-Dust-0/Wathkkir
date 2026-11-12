package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FaceWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REQUEST) {
            val pageId = intent.getStringExtra("pageId")
            if (pageId != null) {
                WidgetUpdater.INSTANCE.pushFaceWidget(context, pageId)
            }
        }
    }

    companion object {
        const val ACTION_REQUEST = "com.samsung.android.intent.action.REQUEST_SERVICEBOX_REMOTEVIEWS"
        const val ACTION_RESPONSE = "com.samsung.android.intent.action.RESPONSE_SERVICEBOX_REMOTEVIEWS"
        const val PAGE_ID_PRAYER = "wathakker_prayer"
    }
}
