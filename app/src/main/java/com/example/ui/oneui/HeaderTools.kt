package com.example.ui.oneui

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import java.lang.ref.WeakReference

/** Small helpers shared by the screen headers: tile backgrounds and the settings shortcut. */
object HeaderTools {
    private var nav: WeakReference<Any>? = null

    @JvmStatic
    fun setNav(obj: Any?) {
        nav = WeakReference(obj)
    }

    @JvmStatic
    fun settings() {
        val nc = nav?.get() as? NavController ?: return
        if (nc.currentBackStackEntry?.destination?.route != "settings") nc.navigate("settings")
    }

    @androidx.compose.runtime.Composable
    fun isOnSettings(): Boolean {
        val nc = nav?.get() as? NavController ?: return false
        val entry by nc.currentBackStackEntryAsState()
        return entry?.destination?.route == "settings"
    }

    @androidx.compose.runtime.Composable
    fun SettingsButton() {
        val nc = nav?.get() as? NavController
        if (nc != null) {
            val entry by nc.currentBackStackEntryAsState()
            if (entry?.destination?.route == "settings") return
        }
        androidx.compose.material3.IconButton(onClick = { settings() }) {
            androidx.compose.material3.Icon(
                Icons.Outlined.Settings,
                contentDescription = L10n.t(S.Settings),
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @androidx.compose.runtime.Composable
    fun tile(modifier: Modifier): Modifier {
        val shape = RoundedCornerShape(26.dp)
        return modifier.clip(shape).background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh, shape)
    }

    // Same glass material as the bottom bar (oneUIGlass: haze blur + 85% tint, near-opaque
    // fallback without blur support) so pill labels stay readable over scrolled content.
    // Apply on a background layer only (see OneUIScreen) so the fade-in alpha never touches
    // the pill's icons/labels; the strip between pills stays clear.
    @androidx.compose.runtime.Composable
    fun glassTile(modifier: Modifier, frost: FrostSpec? = null): Modifier =
        if (frost != null) {
            modifier.oneUIFrosted(RoundedCornerShape(26.dp), frost, 28)
        } else {
            // Non-prayer screens (build 50, his call - 3.1.0 design): solid pill like
            // HeaderTools.tile; the layer alpha outside handles the fade-in.
            modifier.clip(RoundedCornerShape(26.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(26.dp))
        }
}

