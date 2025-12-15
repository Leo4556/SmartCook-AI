package com.example.smartcookai

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityRecipeDetailsBinding
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RecipeDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailsBinding
    private lateinit var recipeViewModel: RecipeViewModel
    private var currentRecipe: RecipeEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentRecipe = intent.getParcelableExtra("recipe")

        val db = AppDatabase.getInstance(this)
        val repo = RecipeRepository(db.recipeDao())
        val factory = RecipeViewModelFactory(repo)
        recipeViewModel = ViewModelProvider(this, factory).get(RecipeViewModel::class.java)

        displayRecipe()

        setupBottomNavigation()
        setupDeleteButton()
    }

    private fun displayRecipe() {
        currentRecipe?.let { recipe ->
            binding.tvTitle.text = recipe.title
            binding.tvTime.text = "${recipe.cookingTime} мин"
            binding.tvIngredients.text = recipe.ingredients
            binding.tvDescription.text = recipe.description

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

        binding.btnDelete.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .start()

        binding.btnDelete.setOnLongClickListener {
            showSnackbar("Удалить рецепт")
            true
        }
    }

    private fun deleteRecipe() {
        currentRecipe?.let { recipe ->
            MaterialAlertDialogBuilder(this)
                .setTitle("Удаление рецепта")
                .setMessage("Вы уверены, что хотите удалить рецепт \"${recipe.title}\"? Это действие нельзя отменить.")
                .setPositiveButton("Удалить") { _, _ ->
                    performDeletion(recipe)
                }
                .setNegativeButton("Отмена", null)
                .setCancelable(true)
                .show()
        }
    }

    private fun performDeletion(recipe: RecipeEntity) {
        recipeViewModel.deleteRecipe(recipe)

        showSnackbar("Рецепт \"${recipe.title}\" удален", "Отменить") {
            //Действие при отмене - восстанавливаем рецепт
            recipeViewModel.addRecipe(recipe)
            showSnackbar("Рецепт восстановлен")
        }

        // 4. Через 2 секунды возвращаемся на главный экран
        binding.root.postDelayed({
            navigateToMain()
        }, 2000)
    }

    private fun showErrorAndClose() {
        showSnackbar("Ошибка загрузки рецепта")
        binding.root.postDelayed({
            navigateToMain()
        }, 1500)
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showSnackbar(message: String, actionText: String? = null, action: (() -> Unit)? = null) {
        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        )

        actionText?.let {
            snackbar.setAction(it) {
                action?.invoke()
            }
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