package com.example.ui.oneui

import android.os.Build
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S

@Composable
fun BlurBehindDialogWindow(radiusDp: Int = 28) {
    val view = LocalView.current
    val density = LocalDensity.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        if (Build.VERSION.SDK_INT >= 31) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val attrs = window.attributes
            attrs.blurBehindRadius = with(density) { radiusDp.dp.roundToPx() }
            window.attributes = attrs
            window.setDimAmount(0.28f)
            window.setGravity(android.view.Gravity.BOTTOM); window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        } else {
            window.setDimAmount(0.5f)
            window.setGravity(android.view.Gravity.BOTTOM); window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }
}

@Composable
fun OneUIDialogSurface(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    confirmText: String = L10n.t(S.Save),
    dismissText: String = L10n.t(S.Cancel),
    confirmEnabled: Boolean = true,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
        Column(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(oneUiMenuColor())
            ) {
                Text(
                    title,
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp),
                    color = cs.onSurface,
                    textAlign = TextAlign.Start,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleLarge
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                        .padding(horizontal = 24.dp)
                ) {
                    content()
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dismissBtn: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(dismissText, color = cs.onSurface, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 17.sp, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    val confirmBtn: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable(enabled = confirmEnabled, onClick = onConfirm ?: {}),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                confirmText,
                                color = if (confirmEnabled) cs.onSurface else cs.onSurface.copy(alpha = 0.38f),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 17.sp,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    // One child order everywhere: Cancel first, confirm second. Compose mirrors
                    // rows automatically in RTL, so Arabic shows حفظ on the left / إلغاء on the
                    // right (One UI convention); English keeps Cancel left / Save right.
                    if (onConfirm == null) dismissBtn()
                    else {
                        dismissBtn()
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(cs.onSurface.copy(alpha = 0.18f)))
                        confirmBtn()
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun OneUIDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    confirmText: String = L10n.t(S.Save),
    dismissText: String? = null,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BlurBehindDialogWindow()
        com.example.ui.theme.AppFontScaled {
        val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.88f).dp
        OneUIDialogSurface(
            title = title,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).heightIn(max = maxDialogHeight),
            confirmText = confirmText,
            dismissText = dismissText ?: L10n.t(if (onConfirm == null) S.Back else S.Cancel),
            confirmEnabled = confirmEnabled,
            content = content
        )
        }
    }
}
