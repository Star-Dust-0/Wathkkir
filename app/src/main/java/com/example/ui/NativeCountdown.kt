package com.example.ui
import android.os.SystemClock
import android.widget.Chronometer
import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontWeight
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
/** Native-first numeric counter: the Android view owns ticks, not a coroutine. */
@Composable
fun NativeCountdown(target:Long,color:Color,sizeSp:Float,bold:Boolean=false,prefix:String="",modifier:Modifier=Modifier) {
 val family=MaterialTheme.typography.bodyLarge.fontFamily
 val face=LocalFontFamilyResolver.current.resolve(family,if(bold)FontWeight.Bold else FontWeight.Normal).value as Typeface
 val pixels=with(androidx.compose.ui.platform.LocalDensity.current){ androidx.compose.ui.unit.TextUnit(sizeSp,androidx.compose.ui.unit.TextUnitType.Sp).toPx() }
 AndroidView(modifier=modifier,factory={c->Chronometer(c).apply {
  isCountDown=true;includeFontPadding=false;setSingleLine(true)
 }},update={v->
  v.setTextColor(color.toArgb());v.setTextSize(TypedValue.COMPLEX_UNIT_PX,pixels)
  v.typeface=face
  v.format=prefix+"%s"
  if(v.tag!=target){v.tag=target;v.base=SystemClock.elapsedRealtime()+target-System.currentTimeMillis();v.start()}
 },onRelease={it.stop()})
}
