package com.example.smartcookai

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat

abstract class BaseActivity : AppCompatActivity() {

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        updateStatusBar()
    }

    private fun updateStatusBar() {
        val isDarkTheme =
            resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK ==
                    Configuration.UI_MODE_NIGHT_YES

        window.statusBarColor = getColor(R.color.colorBackground)

        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = !isDarkTheme
    }
}