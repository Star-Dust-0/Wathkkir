package com.example.fajr
import com.example.fajr.ui.FajrViewModel
import com.example.fajr.data.FajrPreferences
import com.example.fajr.data.City
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class CityAliasRecoveryTest {
 @Test fun aliasPersistsByPlaceAndResetRestoresBothNames() {
  val app=RuntimeEnvironment.getApplication();app.getSharedPreferences("fajr_prefs",0).edit().clear().commit()
  val vm=FajrViewModel(app);val prefs=FajrPreferences(app)
  val city=City("القاهرة","Cairo","مصر","Egypt",30.0444,31.2357)
  vm.setCity(city)
  assertFalse(vm.setCityAlias("  "))
  assertTrue(vm.setCityAlias("  Home  "))
  assertEquals("Home",prefs.getCityNameLocalized("ar"))
  assertEquals("Home",JSONObject(app.getSharedPreferences("fajr_prefs",0).getString("city_aliases_json",null)!!).getString(prefs.placeKey!!))
  vm.setCity(city);assertEquals("Home",prefs.getCityNameLocalized("en"))
  vm.resetCityAlias()
  assertEquals("Cairo, Egypt",prefs.getCityNameLocalized("en"))
  assertEquals("القاهرة، مصر",prefs.getCityNameLocalized("ar"))
  assertNull(prefs.cityNameArOrig)
  vm.setCity(city);assertEquals("Cairo, Egypt",prefs.getCityNameLocalized("en"))
 }
}
