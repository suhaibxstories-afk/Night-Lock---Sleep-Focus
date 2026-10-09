package com.example.util

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val category: AppCategory = AppCategory.OTHER
)

enum class AppCategory {
    SOCIAL,
    ENTERTAINMENT,
    GAMES,
    PRODUCTIVITY,
    SYSTEM,
    OTHER
}
