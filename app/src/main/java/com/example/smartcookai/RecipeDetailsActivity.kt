package com.example.smartcookai

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.ViewTreeObserver
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setBackgroundDrawable(null)
            postponeEnterTransition()

            // ← ДОБАВЛЕНО: Максимальное время ожидания 100ms
            window.decorView.postDelayed({
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startPostponedEnterTransition()
                }
            }, 100)
        }

        binding = ActivityRecipeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getInstance(this)
        val repo = RecipeRepository(db.recipeDao())
        val factory = RecipeViewModelFactory(repo)
        recipeViewModel = ViewModelProvider(this, factory).get(RecipeViewModel::class.java)

        currentRecipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("recipe", RecipeEntity::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("recipe")
        }

        currentRecipe?.let { recipe ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.ivRecipeDetailsImage.transitionName = "recipe_image_${recipe.id}"
            }
        }

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

            val servings = recipe.servings.coerceAtLeast(1)

            val kcalPerServing = recipe.totalKcal / servings
            val proteinPerServing = recipe.totalProtein / servings
            val fatPerServing = recipe.totalFat / servings
            val carbsPerServing = recipe.totalCarbs / servings

            binding.tvServings.text = "Порций: $servings"

            binding.tvNutritionPerServing.text =
                "${kcalPerServing.toInt()} ккал | Б ${proteinPerServing.toInt()} г | Ж ${fatPerServing.toInt()} г | У ${carbsPerServing.toInt()} г"

            binding.tvNutritionTotal.text =
                "Всего: ${recipe.totalKcal.toInt()} ккал | Б ${recipe.totalProtein.toInt()} г | Ж ${recipe.totalFat.toInt()} г | У ${recipe.totalCarbs.toInt()} г"

            // ← ИЗМЕНЕНО: Упрощенная загрузка изображения
            if (!recipe.imagePath.isNullOrEmpty()) {
                val bitmap = BitmapFactory.decodeFile(recipe.imagePath)
                if (bitmap != null) {
                    binding.ivRecipeDetailsImage.setImageBitmap(bitmap)
                } else {
                    binding.ivRecipeDetailsImage.setImageResource(R.drawable.ic_gallery)
                }
            } else {
                binding.ivRecipeDetailsImage.setImageResource(R.drawable.ic_gallery)
            }

            // ← ИЗМЕНЕНО: Запускаем анимацию сразу после установки изображения
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.ivRecipeDetailsImage.viewTreeObserver.addOnPreDrawListener(
                    object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            binding.ivRecipeDetailsImage.viewTreeObserver.removeOnPreDrawListener(this)
                            startPostponedEnterTransition()
                            return true
                        }
                    }
                )
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
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}