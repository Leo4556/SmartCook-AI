package com.example.smartcookai

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.databinding.ItemRecipeBinding

class RecipeAdapter(
    private var items: List<RecipeEntity>,
    private val onFavoriteClick: (RecipeEntity) -> Unit // Новый параметр
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(val binding: ItemRecipeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: RecipeEntity) {
            binding.apply {
                tvRecipeName.text = recipe.title
                tvRecipeTime.text = "${recipe.cookingTime} мин" // Добавим текст

                // Всегда показываем закрашенную закладку на экране избранного
                btnFav.setImageResource(R.drawable.ic_bookmark_filled)
                btnFav.contentDescription = "Удалить из избранного"

                // Обработка клика на закладку
                btnFav.setOnClickListener {
                    onFavoriteClick(recipe)
                }

                if (!recipe.imagePath.isNullOrEmpty()) {
                    ivRecipeImage.setImageBitmap(BitmapFactory.decodeFile(recipe.imagePath))
                } else {
                    ivRecipeImage.setImageResource(R.drawable.ic_gallery)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<RecipeEntity>) {
        items = newList
        notifyDataSetChanged()
    }
}