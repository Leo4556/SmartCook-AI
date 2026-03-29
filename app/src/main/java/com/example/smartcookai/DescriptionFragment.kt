package com.example.smartcookai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.smartcookai.databinding.FragmentDescriptionBinding

class DescriptionFragment : Fragment() {

    private val sharedViewModel: AddRecipeSharedViewModel by activityViewModels()

    private var _binding: FragmentDescriptionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDescriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.descriptionLiveData.observe(viewLifecycleOwner) { description ->
            if (binding.etRecipeDescription.text.toString() != description) {
                binding.etRecipeDescription.setText(description)
            }
        }

        binding.etRecipeDescription.addTextChangedListener {
            sharedViewModel.description = it.toString()
        }

    }

    fun getDescription(): String {
        return binding.etRecipeDescription.text.toString()
    }

    fun clearDescription() {
        binding.etRecipeDescription.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}