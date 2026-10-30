package com.example.fajr.ui

import com.example.ui.i18n.L10n
import com.example.ui.i18n.Lang
import com.example.ui.i18n.S

import androidx.compose.foundation.background
import com.example.utils.clickable
import com.example.utils.plainClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import com.example.ui.oneui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fajr.data.CitiesDatabase
import com.example.fajr.data.CityItem

@Composable
fun CitySearchDialog(onDismiss: () -> Unit, onCitySelected: (CityItem) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = remember(searchQuery) { CitiesDatabase.searchCities(searchQuery) }
    val cs = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        com.example.ui.theme.AppFontScaled {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = com.example.ui.oneui.oneUiMenuColor(),
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اختر الدولة والمدينة", color = cs.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث عن مدينة أو دولة...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Outlined.Close, "Clear") }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
                if (filteredCities.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("لم يتم العثور على نتائج للبحث", color = cs.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCities, key = { it.countryNameEn + "_" + it.cityNameEn }) { city ->
                            CityRowItem(city) {
                                onCitySelected(city)
                                onDismiss()
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp).plainClickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(L10n.t(S.Back), color = cs.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    }
}

@Composable
fun CityRowItem(city: CityItem, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(22.dp), tint = cs.primary)
        Spacer(Modifier.size(12.dp))
        Column {
            if (L10n.lang == Lang.AR) {
                Text(city.cityNameAr + " - " + city.countryNameAr, color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(city.cityNameEn + ", " + city.countryNameEn, color = cs.onSurfaceVariant, fontSize = 12.sp)
            } else {
                Text(city.cityNameEn + " - " + city.countryNameEn, color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(city.cityNameAr + ", " + city.countryNameAr, color = cs.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}
