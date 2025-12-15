package com.example.smartcookai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AddRecipeSharedViewModel : ViewModel() {

    var ingredients: String = ""
        set(value) {
            field = value
            _ingredientsLiveData.value = value
        }

    var description: String = ""
        set(value) {
            field = value
            _descriptionLiveData.value = value
        }

    private val _ingredientsLiveData = MutableLiveData<String>()
    val ingredientsLiveData: LiveData<String> get() = _ingredientsLiveData

    private val _descriptionLiveData = MutableLiveData<String>()
    val descriptionLiveData: LiveData<String> get() = _descriptionLiveData

    fun clearData() {
        ingredients = ""
        description = ""
        _ingredientsLiveData.value = ""
        _descriptionLiveData.value = ""
    }

    // Обрезка пробелов
    fun String.trim(): String = this.trim()
}