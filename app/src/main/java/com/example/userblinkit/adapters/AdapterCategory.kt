package com.example.userblinkit.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.userblinkit.databinding.ItemViewProductCategoryBinding
import com.example.userblinkit.models.Category

class AdapterCategory(
    val categoryList: ArrayList<Category>,
    val onCategoryIconClicked: (Category) -> Unit
) : RecyclerView.Adapter<AdapterCategory.CategoryViewHolder>() {

        class CategoryViewHolder(val binding: ItemViewProductCategoryBinding) : ViewHolder(binding.root)

    // Responsible for inflating the views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        return CategoryViewHolder(
            ItemViewProductCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    // This method returns the total number of items in the dataset, helping RecyclerView determine
    // how many items it needs to display.
    override fun getItemCount(): Int {
        return categoryList.size
    }

    // This method binds data to the views in the ViewHolder based on the item's position in the dataset.
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.binding.apply {
            ivCategoryImage.setImageResource(categoryList[position].image)
            tvCategoryTitle.text = categoryList[position].title
        }

        holder.itemView.setOnClickListener {
            onCategoryIconClicked(category)
        }
    }

}