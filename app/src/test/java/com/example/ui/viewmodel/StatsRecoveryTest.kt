package com.example.ui.viewmodel
import com.example.data.DhikrHistory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class StatsRecoveryTest {
 @Test fun originalRollingWeekMonthAndStreakIncludingYesterdayGrace() {
  val vm=MainViewModel(RuntimeEnvironment.getApplication())
  val method=MainViewModel::class.java.getDeclaredMethod("computeStats",List::class.java).apply{isAccessible=true}
  val today=Calendar.getInstance().apply{set(Calendar.HOUR_OF_DAY,12);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}
  fun h(days:Int,id:Int):DhikrHistory {val c=(today.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,days)};return DhikrHistory(0,id,c.timeInMillis,SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.time))}
  val rows=listOf(h(0,1),h(0,1),h(-1,2),h(-2,2),h(-6,3),h(-7,3),h(-40,1))
  val s=method.invoke(vm,rows) as DhikrStats
  assertEquals(2,s.todayCount);assertEquals(5,s.weekCount);assertEquals(7,s.totalCount);assertEquals(3,s.streakDays)
  val monthStart=(today.clone() as Calendar).apply{set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0)}.timeInMillis
  assertEquals(rows.count{it.timestamp>=monthStart},s.monthCount)
  assertEquals(1 to 3,s.topReadDhikr.first())
  val yesterday=method.invoke(vm,rows.filter{it.dateString!=rows.first().dateString}) as DhikrStats
  assertEquals(2,yesterday.streakDays)
  assertEquals(DhikrStats(),method.invoke(vm,emptyList<DhikrHistory>()))
 }
}
