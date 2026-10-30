package com.example.ui.screens
import com.example.data.Dhikr
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
// DhikrInstance is the literal historical Oct10 declaration. Dialog properties grounded in consumers.
data class DhikrInstance(val dhikr: Dhikr, val hour: Int, val minute: Int)
class DhikrDialogState { var times: Dhikr? by mutableStateOf(null); var edit: Dhikr? by mutableStateOf(null) }
