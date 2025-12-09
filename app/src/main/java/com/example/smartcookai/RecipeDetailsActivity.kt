package com.example.smartcookai

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.smartcookai.databinding.ActivityRecipeDetailsBinding

class RecipeDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем данные из Intent
        val title = intent.getStringExtra("title") ?: ""
        val time = intent.getIntExtra("time", 0)
        val ingredients = intent.getStringExtra("ingredients") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val imagePath = intent.getStringExtra("imagePath")

        // Устанавливаем данные
        binding.tvTitle.text = title
        binding.tvTime.text = "$time мин"
        binding.tvIngredients.text = ingredients
        binding.tvDescription.text = description

        // Фото
        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null)
                binding.ivRecipeDetailsImage.setImageBitmap(bitmap)
        }

        setupBottomNavigation()

    }

    private fun setupBottomNavigation() {

        binding.bottomBar.tabHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Закрываем текущую активити
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
