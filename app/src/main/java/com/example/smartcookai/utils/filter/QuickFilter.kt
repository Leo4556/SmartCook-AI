// data/QuickFilter.kt
package com.example.smartcookai.utils.filter

import com.example.smartcookai.data.RecipeEntity

/**
 * БЫСТРЫЕ ФИЛЬТРЫ
 * Sealed class = ограниченный набор вариантов (как enum, но с данными)
 */
sealed class QuickFilter(
    val id: String,          // Уникальный ID для хранения состояния
    val label: String,       // Текст на кнопке
    val emoji: String,       // Эмодзи для визуализации
    val description: String  // Подсказка при долгом нажатии
) {
    /**
     * КНОПКА "Все фильтры"
     * Открывает FilterBottomSheet
     */
    object AllFilters : QuickFilter(
        "all",
        "Все фильтры",
        "⚙️",
        "Открыть полные настройки"
    )

    /**
     * ФИЛЬТР "Быстрые"
     * Рецепты до 30 минут
     */
    object Quick : QuickFilter(
        "quick",
        "Быстрые",
        "⚡",
        "До 30 минут"
    )

    /**
     * ФИЛЬТР "Низкокалорийные"
     * До 350 ккал НА ПОРЦИЮ
     */
    object LowCalorie : QuickFilter(
        "low_cal",
        "Низкокалорийные",
        "🥗",
        "До 350 ккал на порцию"
    )

    /**
     * ФИЛЬТР "Белковые"
     * Больше 20г белка НА ПОРЦИЮ
     */
    object HighProtein : QuickFilter(
        "protein",
        "Белковые",
        "💪",
        "Больше 20г белка на порцию"
    )

    /**
     * ФИЛЬТР "Низкоуглеводные"
     * Меньше 15г углеводов НА ПОРЦИЮ
     */
    object LowCarb : QuickFilter(
        "low_carb",
        "Низкоуглеводные",
        "🥑",
        "Меньше 15г углеводов на порцию"
    )

    /**
     * ФИЛЬТР "Сбалансированные"
     * БЖУ примерно 30/30/40
     */
    object Balanced : QuickFilter(
        "balanced",
        "Сбалансированные",
        "⚖️",
        "Оптимальное БЖУ"
    )

    companion object {
        /**
         * Возвращает список ВСЕХ фильтров
         * Используется в QuickFilterAdapter
         */
        fun getAll() = listOf(
            AllFilters,
            Quick,
            LowCalorie,
            HighProtein,
            LowCarb,
            Balanced
        )
    }

    /**
     * ЛОГИКА ПРОВЕРКИ
     * Каждый фильтр по-своему проверяет рецепт
     */
    fun matches(recipe: RecipeEntity): Boolean {
        return when (this) {
            AllFilters -> true // Не фильтр, просто кнопка

            Quick -> {
                // Быстрые: от 1 до 30 минут
                recipe.cookingTime in 1..30
            }

            LowCalorie -> {
                // Низкокалорийные: до 350 ккал НА ПОРЦИЮ
                if (recipe.totalKcal <= 0.0 || recipe.servings <= 0) return false

                val kcalPerServing = recipe.totalKcal / recipe.servings
                kcalPerServing <= 350.0
            }

            HighProtein -> {
                // Белковые: >= 20г белка НА ПОРЦИЮ
                if (recipe.totalProtein <= 0.0 || recipe.servings <= 0) return false

                val proteinPerServing = recipe.totalProtein / recipe.servings
                proteinPerServing >= 20.0
            }

            LowCarb -> {
                // Низкоуглеводные: <= 15г углеводов НА ПОРЦИЮ
                if (recipe.totalCarbs <= 0.0 || recipe.servings <= 0) return false

                val carbsPerServing = recipe.totalCarbs / recipe.servings
                carbsPerServing > 0.0 && carbsPerServing <= 15.0
            }

            Balanced -> {
                // Сбалансированные: БЖУ примерно 30/30/40
                // Процент считается от ОБЩЕГО, не на порцию
                val totalNutrients = recipe.totalProtein + recipe.totalFat + recipe.totalCarbs

                if (totalNutrients < 10.0) return false

                val proteinPercent = (recipe.totalProtein / totalNutrients) * 100
                val fatPercent = (recipe.totalFat / totalNutrients) * 100
                val carbPercent = (recipe.totalCarbs / totalNutrients) * 100

                proteinPercent in 25.0..35.0 &&
                        fatPercent in 25.0..35.0 &&
                        carbPercent in 35.0..45.0
            }
        }
    }

    /**
     * TOOLTIP при долгом нажатии
     */
    fun getTooltip(): String {
        return when (this) {
            AllFilters -> "Настроить детальные параметры фильтрации"
            Quick -> "Рецепты, которые готовятся до 30 минут"
            LowCalorie -> "Рецепты с калорийностью до 350 ккал на одну порцию"
            HighProtein -> "Рецепты с содержанием белка от 20г на одну порцию"
            LowCarb -> "Рецепты с содержанием углеводов до 15г на одну порцию"
            Balanced -> "Рецепты со сбалансированным соотношением БЖУ (30/30/40)"
        }
    }
}