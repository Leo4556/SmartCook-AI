// QuickFilterAdapter.kt
package com.example.smartcookai.utils.filter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcookai.R
import com.example.smartcookai.databinding.ItemQuickFilterBinding

class QuickFilterAdapter(
    private var filters: List<QuickFilter>,
    private val onFilterClick: (QuickFilter) -> Unit,
    private val onAllFiltersClick: () -> Unit
) : RecyclerView.Adapter<QuickFilterAdapter.FilterViewHolder>() {

    private val activeFilters = mutableSetOf<String>()

    inner class FilterViewHolder(
        private val binding: ItemQuickFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(filter: QuickFilter, isActive: Boolean) {
            binding.chipFilter.apply {
                text = "${filter.emoji} ${filter.label}"

                // ОСОБАЯ ОБРАБОТКА для кнопки "Все фильтры"
                if (filter is QuickFilter.AllFilters) {
                    // ❌ НЕ checkable - это обычная кнопка!
                    isCheckable = false
                    isChecked = false

                    // Показываем иконку настроек
                    setChipIconResource(R.drawable.ic_settings)

                    // Меняем цвет иконки если есть активные фильтры
                    chipIconTint = ContextCompat.getColorStateList(
                        context,
                        if (activeFilters.isNotEmpty())
                            R.color.colorAccentDark
                        else
                            R.color.colorIconDefault
                    )

                    // Фон всегда прозрачный (не меняется)
                    setChipBackgroundColorResource(android.R.color.transparent)

                    // Обводка меняется
//                    chipStrokeColor = ContextCompat.getColorStateList(
//                        context,
//                        if (activeFilters.isNotEmpty())
//                            R.color.colorAccent
//                        else
//                            R.color.colorDivider
//                    )

                    // При клике открываем BottomSheet
                    setOnClickListener {
                        onAllFiltersClick()
                    }

                    contentDescription = filter.getTooltip()
                } else {
                    // Обычный фильтр - это чип с галочкой
                    isCheckable = true
                    isChecked = isActive
                    chipIcon = null

                    // Цвета через селекторы
                    setChipBackgroundColorResource(R.color.chip_background_selector)

                    // При клике переключаем состояние
                    setOnClickListener {
                        if (activeFilters.contains(filter.id)) {
                            activeFilters.remove(filter.id)
                        } else {
                            activeFilters.add(filter.id)
                        }
                        onFilterClick(filter)
                        notifyItemChanged(adapterPosition)
                        notifyItemChanged(0) // Обновляем "Все фильтры"
                    }

                    // Долгое нажатие = подсказка
                    setOnLongClickListener {
                        android.widget.Toast.makeText(
                            context,
                            filter.description,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        true
                    }

                    contentDescription = filter.getTooltip()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = ItemQuickFilterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filters[position]
        val isActive = activeFilters.contains(filter.id)
        holder.bind(filter, isActive)
    }

    override fun getItemCount() = filters.size

    fun clearFilters() {
        activeFilters.clear()
        notifyDataSetChanged()
    }

    fun getActiveFilterIds() = activeFilters.toSet()

    fun setActiveFilters(filterIds: Set<String>) {
        activeFilters.clear()
        activeFilters.addAll(filterIds)
        notifyDataSetChanged()
    }

    fun getActiveFilters(): List<QuickFilter> {
        return filters.filter { activeFilters.contains(it.id) }
    }

    fun hasActiveFilters() = activeFilters.isNotEmpty()
}