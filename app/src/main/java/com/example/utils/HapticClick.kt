package com.example.utils

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable as foundationClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

/** Drop-in for foundation clickable: gives the platform tick first, respecting the app haptics setting. */
fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    this.foundationClickable(enabled = enabled, onClickLabel = onClickLabel, role = role) {
        AppHaptics.tick(view); onClick()
    }
}

fun Modifier.clickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    this.foundationClickable(
        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        indication = indication, enabled = enabled, onClickLabel = onClickLabel, role = role
    ) { AppHaptics.tick(view); onClick() }
}

/** Same as foundation clickable, without a tick: for OK/Cancel-style action buttons. */
fun Modifier.plainClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = this.foundationClickable(enabled = enabled, onClickLabel = onClickLabel, role = role, onClick = onClick)
