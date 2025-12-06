package com.example.smartcookai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.smartcookai.databinding.FragmentIngredientsBinding
import androidx.fragment.app.activityViewModels


class IngredientsFragment : Fragment() {


    private val sharedViewModel: AddRecipeSharedViewModel by activityViewModels()

    private var _binding: FragmentIngredientsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIngredientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etIngredients.addTextChangedListener {
            sharedViewModel.ingredients = it.toString()
        }
    }


    fun getIngredients(): String {
        return binding.etIngredients.text.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
