package com.example.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: FaceWidgetReceiver.kt */
@Metadata(d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \n2\u00020\u0001:\u0001\nB\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0016¨\u0006\u000b"}, d2 = {"Lcom/example/widget/FaceWidgetReceiver;", "Landroid/content/BroadcastReceiver;", "<init>", "()V", "onReceive", "", "context", "Landroid/content/Context;", "intent", "Landroid/content/Intent;", "Companion", "app"}, k = 1, mv = {2, 2, 0}, xi = 48)
/* loaded from: classes10.dex */
public final class FaceWidgetReceiver extends BroadcastReceiver {
    public static final String ACTION_REQUEST = "com.samsung.android.intent.action.REQUEST_SERVICEBOX_REMOTEVIEWS";
    public static final String ACTION_RESPONSE = "com.samsung.android.intent.action.RESPONSE_SERVICEBOX_REMOTEVIEWS";
    public static final String PAGE_ID_PRAYER = "wathakker_prayer";
    public static final int $stable = 8;

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String pageId;
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(intent, "intent");
        if (Intrinsics.areEqual(intent.getAction(), ACTION_REQUEST) && (pageId = intent.getStringExtra("pageId")) != null) {
            WidgetUpdater.INSTANCE.pushFaceWidget(context, pageId);
        }
    }
}
