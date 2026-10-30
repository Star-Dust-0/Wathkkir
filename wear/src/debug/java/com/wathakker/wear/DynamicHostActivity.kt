package com.wathakker.wear
import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.View
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
/** Debug-only AndroidX reference host. Not the manufacturer's watchface service. */
class DynamicHostActivity:Activity() {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
 override fun onCreate(b:Bundle?) {
  super.onCreate(b)
  val now=System.currentTimeMillis();val a=Slot(0,"Start",now-60000);val z=Slot(2,"Target",now+180000)
  val provider=object:PrayerComplicationService(){init{attachBaseContext(this@DynamicHostActivity)}}
  val data=provider.build(ComplicationType.RANGED_VALUE,PrayerState(now,listOf(a,z),a,z,z))!!
  val drawable=ComplicationDrawable(this)
  val view=object:View(this){override fun onDraw(c:Canvas){
   c.drawColor(Color.BLACK);val r=width*4/10;drawable.bounds=Rect(width/2-r,height/2-r,width/2+r,height/2+r)
   drawable.currentTime=java.time.Instant.now();drawable.draw(c);postInvalidateDelayed(1000)
  }};setContentView(view)
  Log.i("DynamicRingHost","provider_builds=1 sdk="+android.os.Build.VERSION.SDK_INT+" dynamic="+((data as RangedValueComplicationData).dynamicValue!=null))
  scope.launch {
   ComplicationDataEvaluator().evaluate(data.asWireComplicationData()).collect {wire->
    val evaluated=wire.toApiComplicationData()
    val ranged=evaluated as? RangedValueComplicationData
    Log.i("DynamicRingHost","evaluated elapsed="+ranged?.value+" max="+ranged?.max+" at="+System.currentTimeMillis())
    drawable.setComplicationData(evaluated,false);view.invalidate()
   }
  }
 }
 override fun onDestroy(){scope.cancel();super.onDestroy()}
}
