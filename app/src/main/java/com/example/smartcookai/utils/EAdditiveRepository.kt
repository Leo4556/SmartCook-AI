package com.example.smartcookai.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object EAdditiveRepository {

    private var additives: List<EAdditive> = emptyList()

    fun load(context: Context) {

        val json = context.assets
            .open("e_additives.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<EAdditive>>() {}.type
        additives = Gson().fromJson(json, type)
    }

    fun getAdditive(code: String): EAdditive? {
        return additives.find { it.code.equals(code, ignoreCase = true) }
    }
}