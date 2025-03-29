package com.example.userblinkit.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.userblinkit.Utils.CartListener
import com.example.userblinkit.R
import com.example.userblinkit.Utils.Utils
import com.example.userblinkit.adapters.AdapterProduct
import com.example.userblinkit.databinding.FragmentCategoryBinding
import com.example.userblinkit.databinding.ItemViewProductBinding
import com.example.userblinkit.models.Product
import com.example.userblinkit.roomdb.CartProducts
import com.example.userblinkit.viewmodels.UserViewModel
import kotlinx.coroutines.launch

class CategoryFragment : Fragment() {

    private lateinit var binding : FragmentCategoryBinding
    private val viewModel : UserViewModel by viewModels()
    private var category : String ?= null
    private lateinit var adapterProduct: AdapterProduct
    private var cartListener : CartListener?= null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCategoryBinding.inflate(inflater, container, false)

        SetStatusBarColor()
        getProductCategory()
        onNavigationIconClick()
        setToolbarTitle()
        onSearchMenuClick()
        fetchCategoryProduct()
        return binding.root
    }

    // When back button is clicked, navigate to home fragment
    private fun onNavigationIconClick() {
        binding.tbSearchFragment.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_categoryFragment_to_homeFragment)
        }
    }

    // When search button is clicked, navigate to search fragment
    private fun onSearchMenuClick() {
        binding.tbSearchFragment.setOnMenuItemClickListener { menuItem ->
            when(menuItem.itemId) {
                R.id.searchMenu -> {
                    findNavController().navigate(R.id.action_categoryFragment_to_searchFragment)
                    true
                }
                else -> { false }
            }
        }
    }

    // Fetch the specific categories product from the database
    private fun fetchCategoryProduct() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        lifecycleScope.launch {
            viewModel.getCategoryProduct(category!!).collect {
                if(it.isEmpty()) {
                    binding.rvProducts.visibility = View.GONE
                    binding.tvText.visibility = View.VISIBLE
                } else {
                    binding.rvProducts.visibility = View.VISIBLE
                    binding.tvText.visibility = View.GONE
                }
                adapterProduct = AdapterProduct(::onAddButtonClicked,
                        ::onIncrementButtonClicked,
                        ::onDecrementButtonClicked
                )
                // adapter for the recycler view which shows the product information
                binding.rvProducts.adapter = adapterProduct // set the adapter
                adapterProduct.differ.submitList(it) // pass the list of products to the adapter
                binding.shimmerViewContainer.visibility = View.GONE
            }
        }
    }

    private fun setToolbarTitle() {
        binding.tbSearchFragment.title = category
    }

    // Get the category from the bundle which was created when we clicked on the category
    // and navigated to this fragment from the home fragment
    private fun getProductCategory() {
        val bundle = arguments
        category = bundle?.getString("category")
    }

    private fun onAddButtonClicked(product: Product, productBinding: ItemViewProductBinding) {
        productBinding.tvAdd.visibility = View.GONE
        productBinding.llProductCount.visibility = View.VISIBLE

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
            // saving product in the room database
            saveProductInRoomDb(product)
            // Update the itemCount in the realtime database
            // This is done so that if user switches to search fragment, when search fragment opens
            // it will load all the files from the DB and thus if we had added an item, it will persist in the search fragment
            viewModel.updateItemCount(product, itemCount)
        }
    }

    fun onIncrementButtonClicked(product: Product, productBinding: ItemViewProductBinding) {
        // first increasing the count for product then for cart
        var itemCountIncrement = productBinding.tvProductCount.text.toString().toInt()
        itemCountIncrement++

        if(product.productStock!! + 1 > itemCountIncrement) { // checking for product stock
            productBinding.tvProductCount.text = itemCountIncrement.toString()

            // setting the item count for the cart layout
            cartListener?.showCartLayout(1)
            product.itemCount = itemCountIncrement // update product count because it has to be stored in ROOM
            lifecycleScope.launch {
                // saving item count in sharedPreference
                cartListener?.savingCartItemCount(1)
                // Saving and updating the product in the room database
                saveProductInRoomDb(product)
                // Update the itemCount in the realtime database
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
            // If the item count is 0, remove the product from the cart and the room database
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

    // saves the product of the cart in the room database
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

        // saving the product
        lifecycleScope.launch {
            viewModel.insertCartProduct(cartProduct)
        }
    }

    fun SetStatusBarColor() {
        activity?.window?.apply {
            val statusBarColors = ContextCompat.getColor(requireContext(), R.color.yellow)
            statusBarColor = statusBarColors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
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