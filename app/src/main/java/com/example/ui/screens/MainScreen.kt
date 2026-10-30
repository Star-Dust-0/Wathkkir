package com.example.ui.screens
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.foundation.background

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.fajr.ui.FajrViewModel
import com.example.lockwidgets.HomeWidgets
import com.example.prayer.PrayerSkyGlass
import com.example.prayer.PrayerTimesScreen
import com.example.ui.hisn.HisnChapterScreen
import com.example.ui.hisn.HisnListScreen
import com.example.ui.hisn.HisnViewModel
import com.example.ui.oneui.BottomTab
import com.example.ui.oneui.HeaderTools
import com.example.ui.oneui.LocalHazeState
import com.example.ui.oneui.OneUIBottomBar
import com.example.ui.oneui.OneUIDrawer
import com.example.ui.oneui.OneUIDrawerItem
import com.example.ui.viewmodel.MainViewModel
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.CubicBezierEasing

val oneUITabs: List<BottomTab> = listOf(
    BottomTab("prayer", L10n.t(S.PrayerTab)) { c ->
        Icon(Icons.Outlined.Mosque, null, Modifier.size(24.dp), tint = c)
    },
    BottomTab("hisn", L10n.t(S.HisnTab)) { c ->
        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(24.dp), tint = c)
    },
    BottomTab("azkar", L10n.t(S.AzkarTab)) { c ->
        Icon(Icons.Outlined.FormatListBulleted, null, Modifier.size(24.dp), tint = c)
    },
    BottomTab("calendar", L10n.t(S.CalendarTab)) { c ->
        Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(24.dp), tint = c)
    }
)

val oneUIDrawerItems: List<OneUIDrawerItem> = listOf(
    OneUIDrawerItem("favorites", L10n.t(S.Favorites), Icons.Outlined.Star, Color(4293243989L)),
    OneUIDrawerItem("reminders", L10n.t(S.Reminders), Icons.Outlined.Notifications, Color(4293243989L)),
    OneUIDrawerItem("statistics", L10n.t(S.Stats), Icons.Outlined.BarChart, Color(4283289796L)),
    OneUIDrawerItem("tags", L10n.t(S.Tags), Icons.AutoMirrored.Outlined.Label, Color(4290617322L)),
    OneUIDrawerItem("hijri_converter", L10n.t(S.HijriConvTitle), Icons.Outlined.DateRange, Color(4284061916L)),
    OneUIDrawerItem("qibla", L10n.t(S.QiblaTitle), Icons.Outlined.Explore, Color(4280391411L)),
    OneUIDrawerItem("tasbeeh", L10n.t(S.TasbeehTab), Icons.Outlined.AdsClick, Color(4290617322L)),
    OneUIDrawerItem("fajr_alarm", L10n.t(S.FajrAlarm), Icons.Outlined.Alarm, Color(0xFFE5B455)),
    OneUIDrawerItem("settings", L10n.t(S.Settings), Icons.Outlined.Settings, Color(4287669422L)),
    OneUIDrawerItem("about", L10n.t(S.About), Icons.Outlined.Info, Color(4287532691L))
)


@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    HeaderTools.setNav(navController)
    val shortcutRoute = com.example.ui.ShortcutNav.route
    androidx.compose.runtime.LaunchedEffect(shortcutRoute) {
        if (shortcutRoute != null) {
            com.example.ui.ShortcutNav.route = null
            navController.navigate(shortcutRoute)
        }
    }
    val hazeState = rememberHazeState()
    val selectedDhikrNotif by viewModel.selectedDhikrFromNotification.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val hisnViewModel: HisnViewModel = viewModel()
    var showAddDialog by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = HomeWidgets.initialPage()) { oneUITabs.size }
    val scope = rememberCoroutineScope()
    val backEntry by navController.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route
    val isTab = route == "tabs"
    val currentTabRoute = oneUITabs[pagerState.currentPage.coerceIn(0, oneUITabs.size - 1)].route

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(Modifier.fillMaxSize()) {
            // One UI platform page transition, frame-measured from three 90/120fps
            // screen recordings of Samsung Settings on the owner's device:
            //  - incoming page slides the full width in ~345ms (measured 333-355ms
            //    across three samples), easing cubic-bezier(0.15, 0.5, 0.1, 0.9),
            //    least-squares fit against all three videos (MSE 7e-6). No fade on it.
            //  - outgoing page parallaxes to 25% width on its own faster curve:
            //    ~120ms (measured 100-133ms), easing cubic-bezier(0.15, 0.0, 0.3, 0.85)
            //  - outgoing page dims to ~73% brightness over ~75ms and holds
            //  - back is the exact mirror; Navigation 2.8 seeks these transitions for
            //    predictive back automatically (enableOnBackInvokedCallback is set).
            //  - mirrored for RTL like the platform does.
            val oneUiEase = CubicBezierEasing(0.15f, 0.5f, 0.1f, 0.9f)
            val oneUiParallaxEase = CubicBezierEasing(0.15f, 0.0f, 0.3f, 0.85f)
            val dirSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
            // 3.4: one full-screen blur source under the pages. The frosted bottom bar used to blur only the list
            // that sat under it, so on wide pages (centred frame, two-pane Hisn) and mid-swipe the bar had no
            // background where no list was drawn. This source always covers the whole screen.
            Box(Modifier.fillMaxSize().hazeSource(hazeState).background(MaterialTheme.colorScheme.background)) {
            NavHost(
                navController = navController,
                startDestination = "tabs",
                enterTransition = {
                    slideInHorizontally(tween(345, easing = oneUiEase)) { it * dirSign }
                },
                exitTransition = {
                    slideOutHorizontally(tween(120, easing = oneUiParallaxEase)) { -(it / 4) * dirSign } +
                        fadeOut(tween(75), targetAlpha = 0.73f)
                },
                popEnterTransition = {
                    slideInHorizontally(tween(120, easing = oneUiParallaxEase)) { -(it / 4) * dirSign } +
                        fadeIn(tween(75), initialAlpha = 0.73f)
                },
                popExitTransition = {
                    slideOutHorizontally(tween(345, easing = oneUiEase)) { it * dirSign }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                composable("tabs") {
                    val wide = com.example.ui.oneui.isWideScreen()
                    val tabsContent: @Composable () -> Unit = {
                        HorizontalPager(state = pagerState, userScrollEnabled = !wide, modifier = Modifier.fillMaxSize()) { page ->
                            when (oneUITabs[page].route) {
                                "calendar" -> com.example.ui.oneui.WideContentFrame { HijriCalendarScreen { drawerOpen = true } }
                                "prayer" -> PrayerTimesScreen(onQibla = { navController.navigate("qibla") }, onMenu = { drawerOpen = true }, onLocation = { navController.navigate("location_calc") })
                                "hisn" -> if (com.example.ui.oneui.isTwoPane()) HisnTwoPane(
                                    hisnViewModel, viewModel,
                                    onMenu = { drawerOpen = true }
                                ) else HisnListScreen(
                                    hisnViewModel,
                                    { chapter -> navController.navigate("hisn/" + chapter.id) },
                                    { drawerOpen = true }
                                )
                                "home" -> DashboardScreen(
                                    viewModel,
                                    { navController.navigate(it) },
                                    { drawerOpen = true }
                                )
                                else -> com.example.ui.oneui.WideContentFrame { AzkarListScreen(viewModel, { showAddDialog = true }, { drawerOpen = true }) }
                            }
                        }
                    }
                    if (wide) {
                        val onRailSelect: (BottomTab) -> Unit = { tab ->
                            val i = oneUITabs.indexOf(tab)
                            if (i >= 0) scope.launch { pagerState.scrollToPage(i) }
                        }
                        if (currentTabRoute == "prayer") {
                            // The sky scene runs edge to edge; the rail floats over it and the page keeps clear of it.
                            Box(Modifier.fillMaxSize()) {
                                androidx.compose.runtime.CompositionLocalProvider(
                                    com.example.ui.oneui.LocalRailInset provides com.example.ui.oneui.RailInset
                                ) { tabsContent() }
                                com.example.ui.oneui.OneUINavRail(
                                    tabs = oneUITabs,
                                    selectedRoute = currentTabRoute,
                                    onSelect = onRailSelect,
                                    solid = false
                                )
                            }
                        } else {
                            androidx.compose.foundation.layout.Row(Modifier.fillMaxSize()) {
                                com.example.ui.oneui.OneUINavRail(
                                    tabs = oneUITabs,
                                    selectedRoute = currentTabRoute,
                                    onSelect = onRailSelect
                                )
                                Box(Modifier.weight(1f).fillMaxSize()) { tabsContent() }
                            }
                        }
                    } else tabsContent()
                }
                composable(
                    "hisn/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType })
                ) { entry -> com.example.ui.oneui.WideContentFrame {
                    val id = entry.arguments?.getInt("id") ?: 0
                    val addedDhikr by viewModel.allDhikr.collectAsStateWithLifecycle()
                    HisnChapterScreen(
                        id,
                        hisnViewModel,
                        { navController.popBackStack() },
                        addedDhikr.map { it.title }.toSet(),
                        { title, content -> viewModel.addDhikrWithSchedule(title, content, listOf("09:00")) }
                    )
                } }
                framed("favorites") { FavoritesScreen(viewModel) { navController.popBackStack() } }
                framed("reminders") { RemindersScreen(viewModel) { navController.popBackStack() } }
                framed("statistics") { StatisticsScreen(viewModel) { navController.popBackStack() } }
                framed("tags") { TagsScreen(viewModel) { navController.popBackStack() } }
                framed("settings") {
                    SettingsScreen(
                        viewModel,
                        { navController.popBackStack() },
                        onNavigateToLocation = { navController.navigate("location_calc") },
                        onNavigateToOptionalTimes = { navController.navigate("optional_times") },
                        onNavigateToHijriConverter = { navController.navigate("hijri_converter") },
                        onNavigateToAbout = { navController.navigate("about") },
                        onNavigateToWatch = { navController.navigate("watch_settings") }
                    )
                }
                framed("optional_times") {
                    com.example.prayer.OptionalTimesScreen { navController.popBackStack() }
                }
                framed("qibla") {
                    com.example.prayer.QiblaScreen { navController.popBackStack() }
                }
                framed("hijri_converter") {
                    com.example.prayer.HijriConverterScreen { navController.popBackStack() }
                }
                framed("location_calc") {
                    val fajrViewModel: FajrViewModel = viewModel()
                    com.example.prayer.LocationCalculationScreen(fajrViewModel) { navController.popBackStack() }
                }
                framed("fajr_alarm") {
                    val fajrAlarmVm: FajrViewModel = viewModel()
                    FajrAlarmSettingsScreen(fajrAlarmVm) { navController.popBackStack() }
                }
                framed("watch_settings") { WatchSettingsScreen { navController.popBackStack() } }
                framed("tasbeeh") { TasbeehScreen(viewModel) { navController.popBackStack() } }
                framed("about") { AboutScreen { navController.popBackStack() } }
            }
            }
            // v3.2.0 glass system: over the prayer sky the bottom bar and the drawer
            // use the same frost material as the cards; everywhere else they keep
            // their default look.
            val skyFrost = if (isTab && currentTabRoute == "prayer") PrayerSkyGlass.spec.value else null
            if (isTab && !com.example.ui.oneui.isWideScreen()) {
                OneUIBottomBar(
                    tabs = oneUITabs,
                    selectedRoute = currentTabRoute,
                    frost = skyFrost,
                    onSelect = { tab ->
                        val i = oneUITabs.indexOf(tab)
                        if (i >= 0) scope.launch {
                            // Animating across several pages composes every heavy screen in between
                            // mid-animation (the visible stutter). Animate only adjacent hops.
                            if (kotlin.math.abs(i - pagerState.currentPage) <= 1) pagerState.animateScrollToPage(i)
                            else pagerState.scrollToPage(i)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            OneUIDrawer(
                open = drawerOpen,
                currentRoute = if (isTab) currentTabRoute else (route ?: ""),
                items = oneUIDrawerItems,
                onSelect = {
                    drawerOpen = false
                    if (route != it) navController.navigate(it) { launchSingleTop = true }
                },
                onDismiss = { drawerOpen = false },
                frost = skyFrost,
                footer = {
                    val uri = LocalUriHandler.current
                    Text(
                        "brought to you by Instinct",
                        modifier = Modifier.clickable { uri.openUri("https://instinct.com") },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
            if (showAddDialog) {
                DhikrAddDialog(
                    allTags = allTags,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { title, content, times, tagIds ->
                        viewModel.addDhikrWithScheduleAndTags(title, content, times, tagIds)
                        showAddDialog = false
                    },
                    onCreateTag = { viewModel.addTag(it, "#008080", null) }
                )
            }
            val notifDhikr = selectedDhikrNotif
            if (notifDhikr != null) {
                DhikrDetailDialog(
                    dhikr = notifDhikr,
                    onDismiss = { viewModel.clearSelectedDhikrFromNotification() },
                    onMarkAsRead = { viewModel.markAsRead(notifDhikr.id) }
                )
            }
        }
    }
}


/** 3.4 wide screens: Hisn al-Muslim chapter list on one side, the open chapter on the other. */
@Composable
fun HisnTwoPane(hisnViewModel: HisnViewModel, viewModel: MainViewModel, onMenu: () -> Unit) {
    var selected by remember { mutableStateOf<Int?>(null) }
    val addedDhikr by viewModel.allDhikr.collectAsStateWithLifecycle()
    androidx.compose.foundation.layout.Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(0.42f).fillMaxSize()) {
            HisnListScreen(hisnViewModel, { chapter -> selected = chapter.id }, onMenu)
        }
        Box(Modifier.weight(0.58f).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val id = selected
            if (id != null) {
                HisnChapterScreen(
                    id,
                    hisnViewModel,
                    { selected = null },
                    addedDhikr.map { it.title }.toSet(),
                    { title, content -> viewModel.addDhikrWithSchedule(title, content, listOf("09:00")) }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(L10n.t(S.HisnTab), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}


/** 3.4: a non-tab page on wide screens, kept to a readable column instead of stretching edge to edge. */
private fun androidx.navigation.NavGraphBuilder.framed(route: String, content: @Composable () -> Unit) {
    composable(route) { com.example.ui.oneui.WideContentFrame { content() } }
}
