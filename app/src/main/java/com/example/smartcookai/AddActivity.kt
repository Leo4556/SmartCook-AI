package com.example.smartcookai

import android.content.Intent
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
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory
import java.io.File
import java.io.FileOutputStream

class AddActivity : AppCompatActivity() {

    private val sharedViewModel: AddRecipeSharedViewModel by viewModels()

    private lateinit var binding: ActivityAddBinding
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

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "recipe_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)

            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getInstance(this).recipeDao()
        val repository = RecipeRepository(dao)
        val factory = RecipeViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(RecipeViewModel::class.java)

        setupUI()
        setupBottomNavigation()
    }

    private fun setupUI() {
        // Обработчики для переключения фрагментов
        binding.chipIngredients.setOnClickListener {
            replaceFragment(ingredientsFragment)
        }
        binding.chipDescription.setOnClickListener {
            replaceFragment(descriptionFragment)
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

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.addFragmentContainer.id, fragment)
            .commit()
    }

    private fun clearForm() {
        binding.etDishName.text?.clear()
        binding.edCookingTime.text?.clear()

        // Очищаем изображение
        selectedImageUri = null
        binding.ivDishPhoto.setImageResource(R.drawable.ic_gallery)

        // Очищаем фрагменты
        clearFragments()

        // Очищаем SharedViewModel
        sharedViewModel.clearData()
    }

    private fun clearFragments() {
        // Очищаем поле ингредиентов
        if (ingredientsFragment.isAdded) {
            ingredientsFragment.clearIngredients()
        }

        // Очищаем поле описания
        if (descriptionFragment.isAdded) {
            descriptionFragment.clearDescription()
        }
    }

    private fun saveRecipeToDatabase() {
        // Получаем данные
        val title = binding.etDishName.text.toString().trim()
        val ingredients = sharedViewModel.ingredients.trim() ?: ""
        val description = sharedViewModel.description.trim() ?: ""
        val cookingTime = binding.edCookingTime.text.toString().toIntOrNull() ?: 0
        val savedImagePath = selectedImageUri?.let { saveImageToInternalStorage(it) }

        // Проверки
        if (title.isEmpty()) {
            Toast.makeText(this, "Введите название блюда", Toast.LENGTH_SHORT).show()
            return
        }

        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Добавьте ингредиенты", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Введите описание рецепта", Toast.LENGTH_SHORT).show()
            return
        }

        // Создаём объект рецепта
        val recipe = RecipeEntity(
            title = title,
            ingredients = ingredients,
            description = description,
            cookingTime = cookingTime,
            imagePath = savedImagePath
        )

        // Сохраняем
        viewModel.addRecipe(recipe)

        Toast.makeText(this, "Рецепт сохранён!", Toast.LENGTH_SHORT).show()

        // Очистка формы после сохранения
        clearForm()
    }

    private fun setupBottomNavigation() {
        binding.bottomBar.tabHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.bottomBar.tabFav.setOnClickListener {
            val intent = Intent(this, FavouritesActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.bottomBar.tabAdd.setOnClickListener {
            // Мы уже на экране добавления, просто очищаем форму
            clearForm()
        }
        binding.bottomBar.tabSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}