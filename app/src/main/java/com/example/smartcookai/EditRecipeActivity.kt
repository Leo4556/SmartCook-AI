package com.example.smartcookai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityAddBinding
import com.example.smartcookai.databinding.ActivityEditRecipeBinding
import com.example.smartcookai.databinding.ActivityRecipeDetailsBinding
import com.example.smartcookai.utils.NutritionData
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class EditRecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditRecipeBinding

    private var currentRecipe: RecipeEntity? = null

    private val sharedViewModel: AddRecipeSharedViewModel by viewModels()
    private lateinit var foodClassifier: FoodClassifier
    private val ingredientsFragment = IngredientsFragment()
    private val descriptionFragment = DescriptionFragment()

    private lateinit var viewModel: RecipeViewModel
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                binding.ivDishPhoto.setImageURI(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        foodClassifier = FoodClassifier(this)

        val dao = AppDatabase.getInstance(this).recipeDao()
        val repository = RecipeRepository(dao)
        val factory = RecipeViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[RecipeViewModel::class.java]
        currentRecipe = intent.getParcelableExtra("recipe")

        fillFields()
        setupUI()
        setupBottomNavigation()
    }

    private fun fillFields() {
        currentRecipe?.let { recipe ->
            binding.etDishName.setText(recipe.title)
            binding.edCookingTime.setText(recipe.cookingTime.toString())

            sharedViewModel.ingredients = recipe.ingredients
            sharedViewModel.description = recipe.description

            recipe.imagePath?.let {
                binding.ivDishPhoto.setImageURI(Uri.fromFile(File(it)))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        foodClassifier.close()
    }

    private fun setupUI() {
        binding.chipIngredients.setOnClickListener {
            replaceFragment(ingredientsFragment)
        }

        binding.chipDescription.setOnClickListener {
            replaceFragment(descriptionFragment)
        }

        binding.chipAI.setOnClickListener {
            analyzePhotoWithAI()
        }

        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveRecipeToDatabase()
        }

        binding.btnCancel.setOnClickListener {
            clearForm()
            Toast.makeText(this, "Форма очищена", Toast.LENGTH_SHORT).show()
        }

        replaceFragment(ingredientsFragment)
    }

    private fun analyzePhotoWithAI() {
        val uri = selectedImageUri
        if (uri == null) {
            Toast.makeText(this, "❌ Сначала выберите фото", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = getBitmapFromUri(this, uri)
        if (bitmap == null) {
            Toast.makeText(this, "❌ Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
            return
        }

        val result = foodClassifier.analyzeFood(bitmap)

        if (result == null) {
            Toast.makeText(this, "🤷 Не удалось определить блюдо", Toast.LENGTH_SHORT).show()
            return
        }

        binding.etDishName.setText(result.foodName)

        val ingredientsText = if (result.ingredients.isNotEmpty()) {
            result.ingredients.joinToString("\n") { "• $it" }
        } else {
            "⚠️ Для '${result.foodName}' нет ингредиентов в базе\nДобавьте ингредиенты вручную:"
        }

        sharedViewModel.ingredients = ingredientsText

        if (ingredientsFragment.isAdded) {
            ingredientsFragment.updateIngredients(ingredientsText)
        }

        replaceFragment(ingredientsFragment)
        binding.chipIngredients.isChecked = true

        val message = if (result.ingredients.isNotEmpty()) {
            "✅ ${result.foodName}\nИнгредиенты добавлены"
        } else {
            "✅ ${result.foodName}\n⚠️ Ингредиенты не найдены"
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "recipe_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)

            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.editFragmentContainer.id, fragment)
            .commit()
    }

    private fun clearForm() {
        binding.etDishName.text?.clear()
        binding.edCookingTime.text?.clear()
        selectedImageUri = null
        binding.ivDishPhoto.setImageResource(R.drawable.ic_gallery)
        sharedViewModel.clearData()
    }

    private fun saveRecipeToDatabase() {

        val title = binding.etDishName.text.toString().trim()
        val ingredients = sharedViewModel.ingredients.trim()
        val description = sharedViewModel.description.trim()
        val cookingTime = binding.edCookingTime.text.toString().toIntOrNull() ?: 0

        if (title.isEmpty() || ingredients.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "❌ Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {

            val (kcal, protein, fat, carbs) =
                calculateNutritionFromText(ingredients)

            // если фото новое — сохраняем, иначе оставляем старый путь
            val imagePath = if (selectedImageUri != null) {
                saveImageToInternalStorage(selectedImageUri!!)
            } else {
                currentRecipe?.imagePath
            }

            // копируем старый рецепт, чтобы сохранить ID
            val updatedRecipe = currentRecipe?.copy(
                title = title,
                ingredients = ingredients,
                description = description,
                cookingTime = cookingTime,
                imagePath = imagePath,
                totalKcal = kcal,
                totalProtein = protein,
                totalFat = fat,
                totalCarbs = carbs
            )

            if (updatedRecipe != null) {
                viewModel.updateRecipe(updatedRecipe)
            }

            runOnUiThread {
                Toast.makeText(
                    this@EditRecipeActivity,
                    "✅ Изменения сохранены!",
                    Toast.LENGTH_SHORT
                ).show()
                finish() // закрываем экран редактирования
            }
        }
    }


    private fun calculateNutritionFromText(
        ingredientsText: String
    ): Quadruple<Double, Double, Double, Double> {

        var totalKcal = 0.0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalCarbs = 0.0

        val lines = ingredientsText.split("\n")

        for (line in lines) {
            var cleanLine = line
                .replace("•", "")
                .replace(",", "")
                .trim()

            cleanLine = cleanLine
                .replace("-", " ")
                .replace("—", " ")
                .replace(":", " ")
                .replace("=", " ")

            cleanLine = cleanLine
                .replace("грамм", "")
                .replace("грамма", "")
                .replace("граммов", "")
                .replace("гр", "")
                .replace("г", "")
                .trim()

            cleanLine = cleanLine.replace(Regex("\\s+"), " ")

            val parts = cleanLine.split(" ")

            if (parts.size < 2) continue

            val weight = parts.last().toDoubleOrNull() ?: continue
            val name = parts.dropLast(1).joinToString(" ").lowercase()

            val nutrition = NutritionData.ingredients[name]

            if (nutrition != null) {

                totalKcal += nutrition.kcal * weight / 100
                totalProtein += nutrition.protein * weight / 100
                totalFat += nutrition.fat * weight / 100
                totalCarbs += nutrition.carbs * weight / 100
            }
        }

        return Quadruple(totalKcal, totalProtein, totalFat, totalCarbs)
    }



    private fun setupBottomNavigation() {
        binding.bottomBar.tabHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.bottomBar.tabFav.setOnClickListener {
            startActivity(Intent(this, FavouritesActivity::class.java))
            finish()
        }
        binding.bottomBar.tabAdd.setOnClickListener {
            clearForm()
        }
        binding.bottomBar.tabSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }
}