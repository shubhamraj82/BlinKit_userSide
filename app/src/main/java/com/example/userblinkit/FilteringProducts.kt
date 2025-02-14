package com.example.userblinkit

import android.widget.Filter
import com.example.userblinkit.adapters.AdapterProduct
import com.example.userblinkit.models.Product
import java.util.Locale

class FilteringProducts (
    val adapter : AdapterProduct,
    val filter : ArrayList<Product> // initial value will be passed down as all the products in the database
) : Filter() {
    // called when a user types a search query. constraint in parameter is the query of user
    // The return type is FilterResults which holds the filtered list of products based on query
    override fun performFiltering(constraint: CharSequence?): FilterResults {

        val result = FilterResults()

        if (!constraint.isNullOrEmpty()) {
            val filteredList = ArrayList<Product>()
            // if the query passed as constraint was AmulPacket change it to Amul Packet
            val query = constraint.toString().trim().uppercase(Locale.getDefault()).split(" ")
            for (products in filter) { // here the filter will contain all the products inside the database
                if(query.any {
                        // checking whether the product title contains the query
                        products.productTitle?.uppercase(Locale.getDefault())?.contains(it) == true ||
                        products.productCategory?.uppercase(Locale.getDefault())?.contains(it) == true ||
                        products.productPrice?.toString()?.uppercase(Locale.getDefault())?.contains(it) == true ||
                        products.productType?.uppercase(Locale.getDefault())?.contains(it) == true

                    }) {
                    filteredList.add(products)
                }
            }
            result.values = filteredList
            result.count = filteredList.size

        } else {
            result.values = filter
            result.count = filter.size
        }
        return result
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        adapter.differ.submitList(ArrayList(results?.values as List<Product>))
    }

}