package com.example.smartcookai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AddRecipeSharedViewModel : ViewModel() {

    private val _ingredientsLiveData = MutableLiveData<String>()
    val ingredientsLiveData: LiveData<String> get() = _ingredientsLiveData

    private val _descriptionLiveData = MutableLiveData<String>()
    val descriptionLiveData: LiveData<String> get() = _descriptionLiveData

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

    fun clearData() {
        ingredients = ""
        description = ""
    }

}