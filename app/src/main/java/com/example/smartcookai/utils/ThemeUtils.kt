package com.example.smartcookai.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

object ThemeUtils {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    fun saveThemeMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_MODE, mode)
            .apply()
    }

    fun getSavedThemeMode(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun applySavedTheme(context: Context) {
        val savedMode = getSavedThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(savedMode)
    }

    fun isDarkThemeEnabled(context: Context): Boolean {
        val savedMode = getSavedThemeMode(context)
        return when (savedMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> isSystemInDarkMode(context)
        }
    }

    private fun isSystemInDarkMode(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

    // Новый метод для обновления системных UI баров
    fun updateSystemBars(activity: androidx.appcompat.app.AppCompatActivity) {
        val isDarkTheme = isDarkThemeEnabled(activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Для Android 6.0+
            updateStatusBarIcons(activity, isDarkTheme)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Для Android 8.0+
            updateNavigationBar(activity, isDarkTheme)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.M)
    private fun updateStatusBarIcons(activity: androidx.appcompat.app.AppCompatActivity, isDarkTheme: Boolean) {
        val window = activity.window
        var flags = window.decorView.systemUiVisibility

        if (isDarkTheme) {
            // ТЁМНАЯ ТЕМА - СВЕТЛЫЕ иконки
            flags = flags and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            // СВЕТЛАЯ ТЕМА - ТЁМНЫЕ иконки
            flags = flags or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        window.decorView.systemUiVisibility = flags

        // Обновляем цвет статус-бара
        window.statusBarColor = ContextCompat.getColor(activity,
            if (isDarkTheme) android.R.color.black else android.R.color.white
        )
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun updateNavigationBar(activity: androidx.appcompat.app.AppCompatActivity, isDarkTheme: Boolean) {
        val window = activity.window
        var flags = window.decorView.systemUiVisibility

        if (isDarkTheme) {
            // ТЁМНАЯ ТЕМА - СВЕТЛЫЕ иконки навигации
            flags = flags and android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        } else {
            // СВЕТЛАЯ ТЕМА - ТЁМНЫЕ иконки навигации
            flags = flags or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        window.decorView.systemUiVisibility = flags
        window.navigationBarColor = ContextCompat.getColor(activity,
            if (isDarkTheme) android.R.color.black else android.R.color.white
        )
    }
}