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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getInstance(this).recipeDao()
        val repository = RecipeRepository(dao)
        val factory = RecipeViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory).get(RecipeViewModel::class.java)

        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }


        replaceFragment(ingredientsFragment)
        binding.chipIngredients.setOnClickListener {
            replaceFragment(ingredientsFragment)
        }
        binding.chipDescription.setOnClickListener {
            replaceFragment(descriptionFragment)
        }


        binding.btnSave.setOnClickListener {
            saveRecipeToDatabase()
        }

        binding.bottomBar.tabHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        binding.bottomBar.tabFav.setOnClickListener {
            val intent = Intent(this, FavouritesActivity::class.java)
            startActivity(intent)
        }
        binding.bottomBar.tabSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.addFragmentContainer.id, fragment)
            .commit()
    }

    private fun saveRecipeToDatabase() {

        // Получаем данные
        val title = binding.etDishName.text.toString().trim()
        val ingredients = sharedViewModel.ingredients.trim() ?: ""
        val description = sharedViewModel.description.trim() ?: ""
        val cookingTime = binding.edCookingTime.text.toString().toIntOrNull() ?: 0
        val imageUri = selectedImageUri?.toString()


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
            imagePath = imageUri
        )

        // Сохраняем
        viewModel.addRecipe(recipe)

        Toast.makeText(this, "Рецепт сохранён!", Toast.LENGTH_SHORT).show()

        finish()
    }

}
