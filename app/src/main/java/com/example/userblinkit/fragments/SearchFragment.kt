package com.example.userblinkit.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.userblinkit.Utils.CartListener
import com.example.userblinkit.R
import com.example.userblinkit.Utils.Utils
import com.example.userblinkit.adapters.AdapterProduct
import com.example.userblinkit.databinding.FragmentSearchBinding
import com.example.userblinkit.databinding.ItemViewProductBinding
import com.example.userblinkit.models.Product
import com.example.userblinkit.roomdb.CartProducts
import com.example.userblinkit.viewmodels.UserViewModel
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    val viewModel : UserViewModel by viewModels()
    private lateinit var binding : FragmentSearchBinding
    private lateinit var adapterProduct: AdapterProduct
    private var cartListener : CartListener?= null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(layoutInflater)

        getAllTheProducts()
        searchProducts()
        backToHomeFragment()

        return binding.root
    }

    private fun searchProducts() {
        binding.searchEt.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                // the first filter is the original list of all the products in DB using the 'getFilter function'
                // the second filter will take us to the FilteringProducts class's performFiltering function
                adapterProduct.filter.filter(query)
            }
        })
    }

    private fun backToHomeFragment() {
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_homeFragment)
        }
    }

    // When user clicks on search button, all the products should be visible to him
    private fun getAllTheProducts() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        lifecycleScope.launch {
            viewModel.fetchAllTheProducts().collect {
                if(it.isEmpty()) {
                    binding.rvProducts.visibility = View.GONE
                    binding.tvText.visibility = View.VISIBLE
                } else {
                    binding.rvProducts.visibility = View.VISIBLE
                    binding.tvText.visibility = View.GONE
                }
                adapterProduct = AdapterProduct(
                    ::onAddButtonClicked,
                    ::onIncrementButtonClicked,
                    ::onDecrementButtonClicked
                ) // adapter for the recycler view which shows the product information
                binding.rvProducts.adapter = adapterProduct // set the adapter
                adapterProduct.differ.submitList(it) // pass the list of products to the adapter
                adapterProduct.originalList = it as ArrayList<Product> // passing the original list for searching purpose
                binding.shimmerViewContainer.visibility = View.GONE
            }
        }
    }

    private fun onAddButtonClicked(product: Product, productBinding: ItemViewProductBinding) {
        productBinding.tvAdd.visibility = View.GONE
        productBinding.llProductCount.visibility = View.VISIBLE
        Log.d("TAGY", productBinding.tvProductCount.text.toString())

        // setting the item count for the individual product(not in cart) using tvProductCount in item_view_product
        var itemCount = productBinding.tvProductCount.text.toString().toInt()
        itemCount++
        productBinding.tvProductCount.text = itemCount.toString()

        // setting the item count for the cart layout
        cartListener?.showCartLayout(1)

        product.itemCount = itemCount
        lifecycleScope.launch {
            // saving item count in sharedPreference
            cartListener?.savingCartItemCount(1)
            saveProductInRoomDb(product)
            viewModel.updateItemCount(product, itemCount)
        }

    }

    fun onIncrementButtonClicked(product: Product, productBinding: ItemViewProductBinding) {
        // first increasing the count for product then for cart
        var itemCountIncrement = productBinding.tvProductCount.text.toString().toInt()
        itemCountIncrement++

        if(product.productStock!! + 1 > itemCountIncrement) {
            productBinding.tvProductCount.text = itemCountIncrement.toString()

            // setting the item count for the cart layout
            cartListener?.showCartLayout(1)
            // saving item count in sharedPreference
            product.itemCount = itemCountIncrement
            lifecycleScope.launch {
                // saving item count in sharedPreference
                cartListener?.savingCartItemCount(1)
                saveProductInRoomDb(product)
                viewModel.updateItemCount(product, itemCountIncrement)
            }
        } else {
            Utils.showToast(requireContext(), "Cant add more item of this")
        }
    }

    fun onDecrementButtonClicked(product: Product, productBinding: ItemViewProductBinding) {
        var itemCountDecrement = productBinding.tvProductCount.text.toString().toInt()
        itemCountDecrement--

        // saving item count in sharedPreference
        product.itemCount = itemCountDecrement
        lifecycleScope.launch {
            // saving item count in sharedPreference
            cartListener?.savingCartItemCount(-1)
            saveProductInRoomDb(product)
            viewModel.updateItemCount(product, itemCountDecrement)
        }

        // When we decrement from 1 to 0, we want to show the 'Add' button and not something like '- 0 +'
        if (itemCountDecrement > 0) {
            productBinding.tvProductCount.text = itemCountDecrement.toString()
        } else {
            lifecycleScope.launch {
                viewModel.deleteCartProduct(product.productRandomId!!)
            }
            productBinding.tvAdd.visibility = View.VISIBLE
            productBinding.llProductCount.visibility = View.GONE
            productBinding.tvProductCount.text = "0"

        }

        // setting the item count for the cart layout
        cartListener?.showCartLayout(-1)

    }

    private fun saveProductInRoomDb(product: Product) {
        val cartProduct = CartProducts(
            productId = product.productRandomId!!,
            productTitle = product.productTitle,
            productQuantity = product.productQuantity.toString() + product.productUnit.toString(),
            productPrice = "₹${product.productPrice}",
            productCount = product.itemCount,
            productStock = product.productStock,
            productImage = product.productImageUris?.get(0)!!,
            productCategory = product.productCategory,
            adminUid = product.adminUid,
            productType = product.productType
        )

        lifecycleScope.launch {
            viewModel.insertCartProduct(cartProduct)
        }
    }

    // onAttach() is the first method called when a Fragment is associated with an Activity
    // This function checks if the main activity(its parent activity) implements the CartListener interface
    override fun onAttach(context: Context) {
        super.onAttach(context) // attach fragment to activity

        if(context is CartListener) {
            cartListener = context
        } else {
            throw ClassCastException("Please implement cart listener")
        }
    }
}