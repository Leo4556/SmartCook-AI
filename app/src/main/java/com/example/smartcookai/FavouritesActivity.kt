package com.example.smartcookai

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityFavouritesBinding
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory

class FavouritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavouritesBinding
    private lateinit var adapter: RecipeAdapter

    private val recipeViewModel: RecipeViewModel by lazy {
        val db = AppDatabase.getInstance(this)
        val repo = RecipeRepository(db.recipeDao())
        val factory = RecipeViewModelFactory(repo)
        ViewModelProvider(this, factory).get(RecipeViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavouritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        observeDatabase()

        binding.bottomBar.tabHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        binding.bottomBar.tabAdd.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
            startActivity(intent)
        }
        binding.bottomBar.tabSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

    }

    private fun setupRecycler() {
        binding.rvFavourites.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(emptyList())
        binding.rvFavourites.adapter = adapter
    }

    private fun observeDatabase() {
        recipeViewModel.allRecipes.observe(this) { list ->
            adapter.updateList(list)
        }
    }
}