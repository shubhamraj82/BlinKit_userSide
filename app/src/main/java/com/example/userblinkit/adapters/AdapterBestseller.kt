package com.example.userblinkit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.userblinkit.databinding.ItemViewBestsellerBinding
import com.example.userblinkit.models.Bestseller

class AdapterBestseller(val seeAllButtonClicked: (Bestseller) -> Unit) : RecyclerView.Adapter<AdapterBestseller.BestSellerViewHolder>() {
    class BestSellerViewHolder(val binding: ItemViewBestsellerBinding) : ViewHolder(binding.root)

    val diffUtil = object : DiffUtil.ItemCallback<Bestseller>() {

        override fun areItemsTheSame(oldItem: Bestseller, newItem: Bestseller): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Bestseller, newItem: Bestseller): Boolean {
            return oldItem == newItem
        }
    }

    // When the list changes, differ will have the latest list and it uses diffutil to calculate the
    // changes in the background
    val differ = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BestSellerViewHolder {
        return BestSellerViewHolder(ItemViewBestsellerBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: BestSellerViewHolder, position: Int) {
        val productType = differ.currentList[position]

        holder.binding.apply {
            tvProductType.text = productType.productType
            tvTotalProducts.text = productType.products?.size.toString() + "Products"

            val listOfIv = listOf(ivProduct1, ivProduct2, ivProduct3)

            val minimumSize = minOf(listOfIv.size, productType.products?.size!!)

            for (i in 0 until minimumSize) {
                listOfIv[i].visibility = View.VISIBLE
                // If admin has uploaded multiple images of a product, we need only the first one for bestseller
                Glide.with(holder.itemView).load(productType.products[i].productImageUris?.get(0)).into(listOfIv[i])
            }

            if (productType.products.size!! > 3) {
                tvProductCount.visibility = View.VISIBLE
                tvProductCount.text = "+" + (productType.products?.size!! - 3).toString()
            }
        }

        holder.itemView.setOnClickListener {
            seeAllButtonClicked(productType)
        }
    }
}