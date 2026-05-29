// data/RecipeFilter.kt
package com.example.smartcookai.utils.filter

import com.example.smartcookai.data.RecipeEntity

/**
 * МОДЕЛЬ ПОЛНОГО ФИЛЬТРА
 * Используется в FilterBottomSheet для детальной настройки
 */
data class RecipeFilter(
    val timeRanges: Set<TimeRange> = emptySet(),      // Диапазоны времени
    val servingRanges: Set<ServingRange> = emptySet() // Диапазоны порций
) {
    /**
     * ENUM для времени приготовления
     * Каждый элемент = один чип в BottomSheet
     */
    enum class TimeRange(val min: Int, val max: Int, val label: String) {
        QUICK(0, 15, "До 15 мин"),
        MEDIUM(15, 30, "15-30 мин"),
        LONG(30, 60, "30-60 мин"),
        VERY_LONG(60, Int.MAX_VALUE, "Больше часа")
    }

    /**
     * ENUM для количества порций
     * Каждый элемент = один чип в BottomSheet
     */
    enum class ServingRange(val min: Int, val max: Int, val label: String) {
        ONE(1, 1, "1 порция"),
        TWO_FOUR(2, 4, "2-4 порции"),
        FIVE_EIGHT(5, 8, "5-8 порций"),
        MANY(9, Int.MAX_VALUE, "Больше 8 порций")
    }

    /**
     * ПРОВЕРКА: есть ли активные фильтры?
     * Используется для показа индикатора
     */
    fun isActive(): Boolean {
        return timeRanges.isNotEmpty() || servingRanges.isNotEmpty()
    }

    /**
     * ЛОГИКА ФИЛЬТРАЦИИ
     * Проверяет, подходит ли рецепт под выбранные фильтры
     * @param recipe - рецепт для проверки
     * @return true если рецепт подходит
     */
    fun matches(recipe: RecipeEntity): Boolean {
        // Проверка времени приготовления
        if (timeRanges.isNotEmpty()) {
            val matchesTime = timeRanges.any { range ->
                recipe.cookingTime in range.min until range.max
            }
            if (!matchesTime) return false
        }

        // Проверка количества порций
        if (servingRanges.isNotEmpty()) {
            val matchesServings = servingRanges.any { range ->
                recipe.servings in range.min..range.max
            }
            if (!matchesServings) return false
        }

        return true
    }

    /**
     * ТЕКСТОВОЕ ОПИСАНИЕ
     * Для показа в empty state: "Фильтры: Время: До 15 мин • Порции: 2-4"
     */
    fun getActiveFiltersDescription(): String {
        val parts = mutableListOf<String>()

        if (timeRanges.isNotEmpty()) {
            parts.add("Время: ${timeRanges.joinToString(", ") { it.label }}")
        }

        if (servingRanges.isNotEmpty()) {
            parts.add("Порции: ${servingRanges.joinToString(", ") { it.label }}")
        }

        return parts.joinToString(" • ")
    }
}