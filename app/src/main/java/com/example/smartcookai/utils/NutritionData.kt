package com.example.smartcookai.utils

data class Nutrition(
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double
)

object NutritionData {

    val ingredients = mapOf(

        "свекла" to Nutrition(43.0, 1.6, 0.2, 9.6),
        "картофель" to Nutrition(77.0, 2.0, 0.4, 17.0),
        "капуста" to Nutrition(25.0, 1.3, 0.1, 6.0),
        "морковь" to Nutrition(41.0, 0.9, 0.2, 10.0),
        "лук" to Nutrition(40.0, 1.1, 0.1, 9.3),
        "говядина" to Nutrition(250.0, 26.0, 15.0, 0.0),
        "сметана" to Nutrition(206.0, 2.8, 20.0, 3.2),
        "рис" to Nutrition(130.0, 2.7, 0.3, 28.0),
        "курица" to Nutrition(165.0, 31.0, 3.6, 0.0)

    )
}
