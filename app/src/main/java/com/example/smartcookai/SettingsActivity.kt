package com.example.smartcookai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.smartcookai.databinding.ActivitySettingsBinding
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupThemeSwitch()
    }

//    private fun applySavedTheme() {
//        val themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
//        val savedTheme = themePrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
//
//        when (savedTheme) {
//            AppCompatDelegate.MODE_NIGHT_NO -> {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//            }
//            AppCompatDelegate.MODE_NIGHT_YES -> {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//            }
//            else -> {
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
//            }
//        }
//    }

    private fun setupThemeSwitch() {
        val switchTheme = findViewById<SwitchMaterial>(R.id.switchTheme)

        val themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val savedTheme = themePrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        when (savedTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> switchTheme.isChecked = true
            AppCompatDelegate.MODE_NIGHT_NO -> switchTheme.isChecked = false
            else -> {
                // Для системной темы проверяем текущий режим системы
                val currentNightMode = resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                switchTheme.isChecked = currentNightMode ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                val themeMode = if (isChecked) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }

                themePrefs.edit().putInt("theme_mode", themeMode).apply()

                // Применяем тему
                AppCompatDelegate.setDefaultNightMode(themeMode)

                recreate()
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomBar.tabHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.bottomBar.tabAdd.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
            finish()
        }

        binding.bottomBar.tabFav.setOnClickListener {
            startActivity(Intent(this, FavouritesActivity::class.java))
            finish()
        }

        binding.bottomBar.tabSettings.isSelected = true
    }

    override fun onResume() {
        super.onResume()
        // Обновляем выделение вкладки при возвращении
        binding.bottomBar.tabHome.isSelected = false
        binding.bottomBar.tabAdd.isSelected = false
        binding.bottomBar.tabFav.isSelected = false
        binding.bottomBar.tabSettings.isSelected = true
    }
}