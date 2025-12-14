package com.example.smartcookai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.smartcookai.databinding.FragmentIngredientsBinding

class IngredientsFragment : Fragment() {

    private var _binding: FragmentIngredientsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedViewModel: AddRecipeSharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIngredientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity()).get(AddRecipeSharedViewModel::class.java)

        // Подписываемся на изменения ингредиентов
        sharedViewModel.ingredientsLiveData.observe(viewLifecycleOwner) { ingredients ->
            if (ingredients.isNotEmpty()) {
                binding.etIngredients.setText(ingredients)
            }
        }

        // Сохраняем изменения ингредиентов
        binding.etIngredients.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                sharedViewModel.ingredients = binding.etIngredients.text.toString()
            }
        }
    }

    fun updateIngredients(ingredients: String) {
        binding.etIngredients.setText(ingredients)
        sharedViewModel.ingredients = ingredients
    }

    fun clearIngredients() {
        binding.etIngredients.text?.clear()
        sharedViewModel.ingredients = ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}