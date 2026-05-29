package com.example.smartcookai

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.databinding.ItemRecipeBinding


class RecipeAdapter(
    private var items: List<RecipeEntity>,
    private val onItemClick: (RecipeEntity, View) -> Unit = { _, _ -> },
    private val onFavoriteClick: (RecipeEntity) -> Unit = {}
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private var lastClickTime: Long = 0
    private val CLICK_DELAY = 200L

    inner class RecipeViewHolder(val binding: ItemRecipeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: RecipeEntity) {
            binding.apply {
                tvRecipeName.text = recipe.title
                tvRecipeTime.text = "${recipe.cookingTime} мин • ${getServingsText(recipe.servings)}"

                // Устанавливаем transition names для анимации
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ivRecipeImage.transitionName = "recipe_image_${recipe.id}"
                }
                if (!recipe.imagePath.isNullOrEmpty()) {
                    val bitmap = BitmapFactory.decodeFile(recipe.imagePath)
                    if (bitmap != null) {
                        binding.ivRecipeImage.setImageBitmap(bitmap)
                        // Принудительно устанавливаем скругление
                        binding.ivRecipeImage.shapeAppearanceModel = binding.ivRecipeImage.shapeAppearanceModel
                            .toBuilder()
                            .setAllCornerSizes(16.dpToPx(itemView.context))
                            .build()
                    } else {
                        binding.ivRecipeImage.setImageResource(R.drawable.ic_gallery)
                    }
                } else {
                    binding.ivRecipeImage.setImageResource(R.drawable.ic_gallery)
                }


                // Настройка иконки избранного
                if (recipe.isFavorite) {
                    btnFav.setImageResource(R.drawable.ic_bookmark_filled)
                    btnFav.contentDescription = "В избранном"
                    btnFav.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.colorAccentDark)
                    )
                } else {
                    btnFav.setImageResource(R.drawable.ic_bookmark)
                    btnFav.contentDescription = "Добавить в избранное"
                    btnFav.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.colorTextSecondary)
                    )
                }

                // Обработка клика на избранное с защитой от двойного клика
                btnFav.setOnClickListener {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > CLICK_DELAY) {
                        lastClickTime = currentTime
                        onFavoriteClick(recipe)
                    }
                }

                // Обработка клика на карточку - передаем View для анимации
                root.setOnClickListener {
                    onItemClick(recipe, ivRecipeImage)
                }

                // Загрузка изображения
                if (!recipe.imagePath.isNullOrEmpty()) {
                    val bitmap = BitmapFactory.decodeFile(recipe.imagePath)
                    if (bitmap != null) {
                        ivRecipeImage.setImageBitmap(bitmap)
                    } else {
                        ivRecipeImage.setImageResource(R.drawable.ic_gallery)
                    }
                } else {
                    ivRecipeImage.setImageResource(R.drawable.ic_gallery)
                }
            }
        }
    }

    // Вспомогательная функция
    fun Int.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
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

    private fun getServingsText(servings: Int): String {
        return when {
            servings % 10 == 1 && servings % 100 != 11 -> "$servings порция"
            servings % 10 in 2..4 && servings % 100 !in 12..14 -> "$servings порции"
            else -> "$servings порций"
        }
    }
}