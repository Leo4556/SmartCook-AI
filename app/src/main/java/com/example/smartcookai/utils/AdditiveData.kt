package com.example.smartcookai.utils

data class Additive(
    val code: String,
    val name: String,
    val safety: String,
    val description: String
)

object AdditivesData {

    val additives = mapOf(

        "E100" to Additive(
            "E100",
            "Куркумин",
            "Безопасна",
            "Натуральный краситель. Обладает противовоспалительными свойствами."
        ),

        "E211" to Additive(
            "E211",
            "Бензоат натрия",
            "Умеренно опасна",
            "Консервант. Может вызывать аллергические реакции."
        ),

        "E621" to Additive(
            "E621",
            "Глутамат натрия",
            "Спорная",
            "Усилитель вкуса. Возможны головные боли при чрезмерном употреблении."
        ),

        "E330" to Additive(
            "E330",
            "Лимонная кислота",
            "Безопасна",
            "Регулятор кислотности. Натуральное происхождение."
        )
    )
}