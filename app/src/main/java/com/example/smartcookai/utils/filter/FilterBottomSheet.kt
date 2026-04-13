// FilterBottomSheet.kt
package com.example.smartcookai.utils.filter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.smartcookai.R
import com.example.smartcookai.databinding.BottomSheetFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class FilterBottomSheet(
    private val currentFilter: RecipeFilter,
    private val onApplyFilter: (RecipeFilter) -> Unit,
    private val onDismissWithoutChanges: () -> Unit  // ← НОВЫЙ callback
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterBinding? = null
    private val binding get() = _binding!!

    // Временное хранилище выбранных фильтров
    private val selectedTimeRanges = mutableSetOf<RecipeFilter.TimeRange>()
    private val selectedServingRanges = mutableSetOf<RecipeFilter.ServingRange>()

    // ← НОВОЕ: флаг "применили ли фильтры?"
    private var wasApplied = false

    companion object {
        const val TAG = "FilterBottomSheet"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Копируем текущие фильтры
        selectedTimeRanges.addAll(currentFilter.timeRanges)
        selectedServingRanges.addAll(currentFilter.servingRanges)

        setupTimeChips()
        setupServingChips()
        setupButtons()
        updateApplyButtonState()
    }

    private fun setupTimeChips() {
        RecipeFilter.TimeRange.values().forEach { timeRange ->
            val chip = createChip(timeRange.label, selectedTimeRanges.contains(timeRange))
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedTimeRanges.add(timeRange)
                } else {
                    selectedTimeRanges.remove(timeRange)
                }
                updateApplyButtonState()
            }
            binding.chipGroupTime.addView(chip)
        }
    }

    private fun setupServingChips() {
        RecipeFilter.ServingRange.values().forEach { servingRange ->
            val chip = createChip(servingRange.label, selectedServingRanges.contains(servingRange))
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedServingRanges.add(servingRange)
                } else {
                    selectedServingRanges.remove(servingRange)
                }
                updateApplyButtonState()
            }
            binding.chipGroupServings.addView(chip)
        }
    }

    private fun setupButtons() {
        // Кнопка закрытия (X)
        binding.btnClose.setOnClickListener {
            // ← Закрываем БЕЗ применения
            dismiss()
        }

        // Кнопка "Применить"
        binding.btnApply.setOnClickListener {
            val filter = RecipeFilter(
                timeRanges = selectedTimeRanges.toSet(),
                servingRanges = selectedServingRanges.toSet()
            )
            wasApplied = true  // ← Отметили, что применили
            onApplyFilter(filter)
            dismiss()
        }

        // Кнопка "Сбросить"
        binding.btnReset.setOnClickListener {
            wasApplied = true  // ← Сброс тоже считается применением
            onApplyFilter(RecipeFilter())
            dismiss()
        }
    }

    private fun updateApplyButtonState() {
        val hasActiveFilters =
            selectedTimeRanges.isNotEmpty() || selectedServingRanges.isNotEmpty()

        binding.btnApply.isEnabled = hasActiveFilters

        if (hasActiveFilters) {
            val count = selectedTimeRanges.size + selectedServingRanges.size
            binding.btnApply.text = "Применить ($count)"
        } else {
            binding.btnApply.text = "Применить"
        }
    }

    private fun createChip(text: String, isChecked: Boolean): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            this.isChecked = isChecked
            setChipBackgroundColorResource(R.color.chip_background_selector)
            setTextColor(resources.getColorStateList(R.color.colorTextPrimary, null))
        }
    }

    // ← НОВОЕ: вызывается при закрытии
    override fun onDestroyView() {
        super.onDestroyView()

        // Если закрыли БЕЗ применения - сообщаем об этом
        if (!wasApplied) {
            onDismissWithoutChanges()
        }

        _binding = null
    }
}