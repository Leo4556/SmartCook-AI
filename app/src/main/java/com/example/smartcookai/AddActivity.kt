package com.example.smartcookai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityAddBinding
import com.example.smartcookai.utils.ThemeUtils
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class AddActivity : AppCompatActivity() {

    private val sharedViewModel: AddRecipeSharedViewModel by viewModels()
    private lateinit var binding: ActivityAddBinding
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
        ThemeUtils.applySavedTheme(this)

        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        foodClassifier = FoodClassifier(this)

        val dao = AppDatabase.getInstance(this).recipeDao()
        val repository = RecipeRepository(dao)
        val factory = RecipeViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[RecipeViewModel::class.java]

        setupUI()
        setupBottomNavigation()
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
            .replace(binding.addFragmentContainer.id, fragment)
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
        val imagePath = selectedImageUri?.let { saveImageToInternalStorage(it) }

        if (title.isEmpty() || ingredients.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "❌ Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val recipe = RecipeEntity(
            title = title,
            ingredients = ingredients,
            description = description,
            cookingTime = cookingTime,
            imagePath = imagePath
        )

        viewModel.addRecipe(recipe)
        Toast.makeText(this, "✅ Рецепт сохранён!", Toast.LENGTH_SHORT).show()
        clearForm()
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
