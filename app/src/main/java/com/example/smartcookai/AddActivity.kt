package com.example.smartcookai
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smartcookai.DescriptionFragment
import com.example.smartcookai.IngredientsFragment
import com.example.smartcookai.databinding.ActivityAddBinding

class AddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBinding

    private val ingredientsFragment = IngredientsFragment()
    private val descriptionFragment = DescriptionFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Загружаем вкладку по умолчанию
        replaceFragment(ingredientsFragment)

        binding.btnTabIngredients.setOnClickListener {
            replaceFragment(ingredientsFragment)
        }
        binding.btnTabDescription.setOnClickListener {
            replaceFragment(descriptionFragment)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.addFragmentContainer.id, fragment)
            .commit()
    }
}
