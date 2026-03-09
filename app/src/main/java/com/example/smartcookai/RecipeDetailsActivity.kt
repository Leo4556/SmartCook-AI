package com.example.smartcookai

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityRecipeDetailsBinding
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class RecipeDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityRecipeDetailsBinding
    private lateinit var recipeViewModel: RecipeViewModel
    private var currentRecipe: RecipeEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getInstance(this)
        val repo = RecipeRepository(db.recipeDao())
        val factory = RecipeViewModelFactory(repo)
        recipeViewModel = ViewModelProvider(this, factory).get(RecipeViewModel::class.java)

        currentRecipe = intent.getParcelableExtra("recipe")
        currentRecipe?.id?.let { id ->
            recipeViewModel.getRecipeByIdLive(id)
                .observe(this) { updatedRecipe ->
                    if (updatedRecipe != null) {
                        currentRecipe = updatedRecipe
                        displayRecipe()
                    }
                }
        }

        displayRecipe()

        setupBottomNavigation()
        setupDeleteButton()
        setupEditButton()
    }

    private fun setupEditButton() {
        binding.btnEdit.setOnClickListener {
            currentRecipe?.let { recipe ->
                val intent = Intent(this, EditRecipeActivity::class.java)
                intent.putExtra("recipe", recipe)
                startActivity(intent)
            }
        }
    }

    private fun displayRecipe() {
        currentRecipe?.let { recipe ->
            binding.tvTitle.text = recipe.title
            binding.tvTime.text = "${recipe.cookingTime} мин"
            binding.tvIngredients.text = recipe.ingredients
            binding.tvDescription.text = recipe.description
            binding.tvCalories.text = "${recipe.totalKcal.toInt()} ккал"
            binding.tvProtein.text = "Белки: ${recipe.totalProtein.toInt()} г"
            binding.tvFat.text = "Жиры: ${recipe.totalFat.toInt()} г"
            binding.tvCarbs.text = "Углеводы: ${recipe.totalCarbs.toInt()} г"


            if (!recipe.imagePath.isNullOrEmpty()) {
                val bitmap = BitmapFactory.decodeFile(recipe.imagePath)
                if (bitmap != null) {
                    binding.ivRecipeDetailsImage.setImageBitmap(bitmap)
                }
            }
        } ?: run {
            showErrorAndClose()
        }
    }

    private fun setupDeleteButton() {
        binding.btnDelete.setOnClickListener {
            deleteRecipe()
        }
    }

    private fun deleteRecipe() {
        currentRecipe?.let { recipe ->
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Удаление рецепта")
                .setMessage("Вы уверены, что хотите удалить рецепт \"${recipe.title}\"?")
                .setPositiveButton("Удалить") { _, _ ->
                    performDeletion(recipe)
                }
                .setNegativeButton("Отмена", null)
                .setCancelable(true)
                .show()

            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccentDark))

            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccentDark))
        }
    }

    private fun performDeletion(recipe: RecipeEntity) {
        recipeViewModel.deleteRecipe(recipe)
        val handler = binding.root
        val navigateRunnable = Runnable {
            navigateToMain()
        }

        handler.postDelayed(navigateRunnable, 2000)

        showUndoSnackbar(
            message = "Рецепт \"${recipe.title}\" удален",
            actionText = "Отменить"
        ) {
            handler.removeCallbacks(navigateRunnable)

            recipeViewModel.addRecipe(recipe)

            showUndoSnackbar("Рецепт восстановлен")
        }

    }

    private fun showErrorAndClose() {
        showUndoSnackbar("Ошибка загрузки рецепта")
        binding.root.postDelayed({
            navigateToMain()
        }, 1500)
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showUndoSnackbar(
        message: String,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {

        val snackbar = Snackbar.make(
            window.decorView.findViewById(android.R.id.content),
            message,
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

        actionText?.let {
            snackbar.setAction(it) {
                action?.invoke()
            }

            snackbar.setActionTextColor(
                ContextCompat.getColor(this, R.color.colorAccentDark)
            )
        }

        snackbar.show()
    }

    private fun setupBottomNavigation() {
        binding.bottomBar.tabHome.setOnClickListener {
            navigateToMain()
        }

        binding.bottomBar.tabAdd.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
            finish()
        }

        binding.bottomBar.tabFav.setOnClickListener {
            startActivity(Intent(this, FavouritesActivity::class.java))
            finish()
        }

        binding.bottomBar.tabSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }
}