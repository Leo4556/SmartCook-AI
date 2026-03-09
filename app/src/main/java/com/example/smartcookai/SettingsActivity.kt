package com.example.smartcookai

import android.app.Application
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Dao
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivitySettingsBinding
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class SmartCookApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val savedTheme = prefs.getInt(
            "theme_mode",
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        AppCompatDelegate.setDefaultNightMode(savedTheme)
    }
}

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: RecipeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getInstance(this).recipeDao()
        repository = RecipeRepository(dao)

        setupThemeSwitch()
        setupClearFavorites()
        deleteAllRecipes()
        setupBottomNavigation()
    }

    private fun setupThemeSwitch() {

        val themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)

        val savedTheme = themePrefs.getInt(
            "theme_mode",
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        // Выставляем активную кнопку
        when (savedTheme) {
            AppCompatDelegate.MODE_NIGHT_NO ->
                binding.themeToggle.check(R.id.radioLight)

            AppCompatDelegate.MODE_NIGHT_YES ->
                binding.themeToggle.check(R.id.radioDark)

            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ->
                binding.themeToggle.check(R.id.radioSystem)
        }

        binding.themeToggle.setOnCheckedChangeListener { _, checkedId ->

            val themeMode = when (checkedId) {

                R.id.radioLight ->
                    AppCompatDelegate.MODE_NIGHT_NO

                R.id.radioDark ->
                    AppCompatDelegate.MODE_NIGHT_YES

                else ->
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            themePrefs.edit()
                .putInt("theme_mode", themeMode)
                .apply()

            AppCompatDelegate.setDefaultNightMode(themeMode)
        }
    }

    private fun setupClearFavorites() {

        binding.btnClearFavorites.setOnClickListener {

            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Очистить избранное")
                .setMessage("Удалить все рецепты из избранного?")
                .setPositiveButton("Очистить") { _, _ ->

                    lifecycleScope.launch {
                        repository.clearFavorites()

                        Toast.makeText(
                            this@SettingsActivity,
                            "Избранное очищено",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }

                .setNegativeButton("Отмена", null)
                .show()
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccentDark))

            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccentDark))
        }
    }

    private fun deleteAllRecipes() {
        binding.btnDeleteAllRecipes.setOnClickListener {

            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Удалить все рецепты")
                .setMessage("Вы уверены? Это действие нельзя отменить.")
                .setPositiveButton("Удалить") { _, _ ->

                    lifecycleScope.launch {
                        repository.deleteAllRecipes()
                        showDeleteAllSnackbar()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()


            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccentDark))

            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccentDark))


        }
    }

    private fun showDeleteAllSnackbar() {

        val snackbar = Snackbar.make(
            binding.root,
            "Все рецепты удалены",
            Snackbar.LENGTH_LONG
        )

        snackbar.anchorView = binding.bottomBar.bottomBar

        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as? CoordinatorLayout.LayoutParams

        params?.apply {
            marginStart = 16
            marginEnd = 16
            bottomMargin = 16
        }

        snackbar.show()
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