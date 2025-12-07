package com.example.smartcookai

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.databinding.ItemRecipeBinding

class RecipeAdapter(
    private var items: List<RecipeEntity>
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(val binding: ItemRecipeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val item = items[position]

        holder.binding.apply {
            tvRecipeName.text = item.title
            tvRecipeTime.text = item.cookingTime.toString()

            if (!item.imagePath.isNullOrEmpty())  {
                ivRecipeImage.setImageBitmap(BitmapFactory.decodeFile(item.imagePath))

            } else {
                ivRecipeImage.setImageResource(R.drawable.ic_home)
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<RecipeEntity>) {
        items = newList
        notifyDataSetChanged()
    }
}
