package com.example.smartcookai
import android.content.Intent
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

        replaceFragment(ingredientsFragment)

        binding.chipIngredients.setOnClickListener {
            replaceFragment(ingredientsFragment)
        }
        binding.chipDescription.setOnClickListener {
            replaceFragment(descriptionFragment)
        }

        binding.bottomBar.tabHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.addFragmentContainer.id, fragment)
            .commit()
    }
}
