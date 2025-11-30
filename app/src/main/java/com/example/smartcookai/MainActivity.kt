package com.example.smartcookai
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Switch
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartcookai.databinding.ActivityMainBinding
import com.example.smartcookai.databinding.ViewBottomBarBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*// Выделяем главную кнопку при запуске
        selectTab(binding.bottomBar.tabHome)

        binding.bottomBar.tabHome.setOnClickListener {
            selectTab(it)
        }*/

        binding.bottomBar.tabAdd.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
//            selectTab(it)
            startActivity(intent)
        }
    }
/*

    private fun selectTab(selected: View) {
        binding.bottomBar.tabHome.isSelected = false
        binding.bottomBar.tabAdd.isSelected = false
        binding.bottomBar.tabFav.isSelected = false
        binding.bottomBar.tabSettings.isSelected = false

        selected.isSelected = true
    }
*/

}