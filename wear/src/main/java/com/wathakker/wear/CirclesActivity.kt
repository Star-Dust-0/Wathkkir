package com.wathakker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay

/** Standalone prayer circles screen, opened by the complication. 80% dark scrim over whatever is behind it. */
class CirclesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.load(this)
        LocationSync.refresh(this)
        setContent {
            val ctx = LocalContext.current
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
            val state = remember(now / 20000) { Prayer.compute(ctx, now) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    CirclesPage(state, now, maxWidth.value, true, Color.Black.copy(alpha = 0.8f))
                }
            }
        }
    }
}
