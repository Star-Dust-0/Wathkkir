package com.example.ui
import android.os.SystemClock
import android.widget.Chronometer
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
/** Native-first numeric counter: the Android view owns ticks, not a coroutine. */
@Composable
fun NativeCountdown(target:Long,color:Color,sizeSp:Float,bold:Boolean=false,prefix:String="",modifier:Modifier=Modifier) {
 AndroidView(modifier=modifier,factory={c->Chronometer(c).apply {
  isCountDown=true;includeFontPadding=false;setSingleLine(true)
 }},update={v->
  v.setTextColor(color.toArgb());v.setTextSize(TypedValue.COMPLEX_UNIT_SP,sizeSp)
  v.typeface=Typeface.create(Typeface.DEFAULT,if(bold)Typeface.BOLD else Typeface.NORMAL)
  v.format=prefix+"%s"
  if(v.tag!=target){v.tag=target;v.base=SystemClock.elapsedRealtime()+target-System.currentTimeMillis();v.start()}
 },onRelease={it.stop()})
}
