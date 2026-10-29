package com.example.fajr.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.view.Gravity
import com.example.ui.oneui.oneUiMenuColor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajr.data.City
import com.example.fajr.data.Cities
import com.example.ui.i18n.L10n
import com.example.ui.i18n.Lang
import com.example.ui.i18n.S
import androidx.compose.material3.Text
import java.util.Locale

/** Built-in city picker with a search box. Filters on Arabic/English city and country names.
 *  Layout calibrated to the original 3.4.2 dialog: 320dp wide, 80% high, centred, 20dp padding. */
@Composable
fun CitySearchDialog(onDismiss: () -> Unit, onCitySelected: (City) -> Unit) {
    var query by remember { mutableStateOf("") }
    val list = remember(query) {
        if (query.isBlank()) Cities.all else {
            val q = query.trim().lowercase(Locale.ROOT)
            Cities.all.filter { it.nameAr.contains(q, true) || it.nameEn.contains(q, true) || it.countryAr.contains(q, true) || it.countryEn.contains(q, true) }
        }
    }
    val ar = L10n.lang == Lang.AR
    val cs = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.setDimAmount(0.6f)
            window.setGravity(Gravity.CENTER)
        }
        Column(
            Modifier.width(320.dp).fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp)).background(oneUiMenuColor())
                .padding(20.dp)
        ) {
            Text(L10n.t(S.PickCountryCity), fontSize = 18.sp, lineHeight = 23.6.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 79.dp).clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(24.dp)).padding(start = 14.dp, end = 52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f).padding(vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(L10n.t(S.SearchCityHint), fontSize = 18.sp, color = cs.onSurfaceVariant)
                    BasicTextField(value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, color = cs.onSurface),
                        modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list) { c ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF1F1F1))
                            .clickable { onCitySelected(c); onDismiss() }.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.LocationOn, null, tint = cs.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text((if (ar) c.nameAr else c.nameEn) + " - " + (if (ar) c.countryAr else c.countryEn), fontSize = 16.sp, lineHeight = 23.6.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                            Text((if (ar) c.nameEn else c.nameAr) + ", " + (if (ar) c.countryEn else c.countryAr), fontSize = 13.sp, lineHeight = 23.6.sp, color = cs.onSurfaceVariant)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                Text(L10n.t(S.Cancel), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
            }
        }
    }
}
