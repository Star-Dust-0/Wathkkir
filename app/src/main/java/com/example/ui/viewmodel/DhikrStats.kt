package com.example.ui.viewmodel

data class DhikrStats(
    val todayCount: Int = 0,
    val weekCount: Int = 0,
    val monthCount: Int = 0,
    val totalCount: Int = 0,
    val streakDays: Int = 0,
    val topReadDhikr: List<Pair<Int, Int>> = emptyList()
)
