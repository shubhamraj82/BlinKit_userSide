package com.example.userblinkit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.models.SlideModel
import com.example.userblinkit.viewmodels.FilteringProducts
import com.example.userblinkit.databinding.ItemViewProductBinding
import com.example.userblinkit.models.Product


class AdapterProduct(
    val onAddButtonClicked: (Product, ItemViewProductBinding) -> Unit,
    val onIncrementButtonClicked: (Product, ItemViewProductBinding) -> Unit,
    val onDecrementButtonClicked: (Product, ItemViewProductBinding) -> Unit
) : RecyclerView.Adapter<AdapterProduct.ProductViewHolder>(), Filterable {

    class ProductViewHolder(val binding: ItemViewProductBinding) : RecyclerView.ViewHolder(binding.root)

    // When at item is added or removed in the recycler view,
    // we use diffutil to update the recycler view efficiently
    // DiffUtil updates only the changed items efficiently.
    val diffutil = object : DiffUtil.ItemCallback<Product>() {

        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.productRandomId == newItem.productRandomId
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }

    // AsyncListDiffer is used to calculate differences in a background thread, making UI updates smoother.
    val differ = AsyncListDiffer(this, diffutil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(ItemViewProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        // currentList holds the latest version of the list which is made by AsyncListDiffer(this runs async in background)
        val product = differ.currentList[position]

        holder.binding.apply {
            val imageList = ArrayList<SlideModel>()
            val productImage = product.productImageUris

            if (!productImage.isNullOrEmpty()) {
                for (uri in productImage) {
                    imageList.add(SlideModel(uri.toString()))
                }
                ivImageSlider.setImageList(imageList)
            }

            tvProductTitle.text = product.productTitle
            val quantity = product.productQuantity.toString()  + " " + product.productUnit
            productQuantity.text = quantity
            tvProductPrice.text = "₹" + product.productPrice

            // If product count is > 0, no need to show the Add button
            if (product.itemCount!! > 0) {
                tvProductCount.text=product.itemCount.toString()
                tvAdd.visibility=View.GONE
                llProductCount.visibility= View.VISIBLE
            }

            tvAdd.setOnClickListener {
                onAddButtonClicked(product, this) // 'this' refers to the ItemViewProductBinding
            }

            tvIncrementCount.setOnClickListener {
                onIncrementButtonClicked(product, this)
            }

            tvDecrementCount.setOnClickListener {
                onDecrementButtonClicked(product, this)
            }

        }
    }


    val filteredList: FilteringProducts? = null
    var originalList = ArrayList<Product>()
    override fun getFilter(): Filter {
        if (filteredList == null) {
            return FilteringProducts(this, originalList)
        }
        return filteredList
    }
}