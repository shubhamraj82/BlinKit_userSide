package com.example.userblinkit.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.userblinkit.Utils.CartListener
import com.example.userblinkit.Utils.Constants
import com.example.userblinkit.R
import com.example.userblinkit.Utils.Utils
import com.example.userblinkit.adapters.AdapterBestseller
import com.example.userblinkit.adapters.AdapterCategory
import com.example.userblinkit.adapters.AdapterProduct
import com.example.userblinkit.databinding.BsSeeAllBinding
import com.example.userblinkit.databinding.FragmentHomeBinding
import com.example.userblinkit.databinding.ItemViewProductBinding
import com.example.userblinkit.models.Bestseller
import com.example.userblinkit.models.Category
import com.example.userblinkit.models.Product
import com.example.userblinkit.roomdb.CartProducts
import com.example.userblinkit.viewmodels.UserViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel : UserViewModel by viewModels()
    private lateinit var adapterBestSellers : AdapterBestseller
    private lateinit var adapterProduct : AdapterProduct
    private var cartListener : CartListener?= null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        SetStatusBarColor()
        setAllCategories()
        navigatingToSearchFragment()
        onProfileClicked()
        fetchBestSellers()

        return binding.root
    }

    private fun fetchBestSellers() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        lifecycleScope.launch {
            viewModel.fetchProductType().collect {
                adapterBestSellers = AdapterBestseller(::seeAllButtonClicked)
                binding.rvBestselers.adapter = adapterBestSellers
                adapterBestSellers.differ.submitList(it)
                binding.shimmerViewContainer.visibility = View.GONE
            }
        }
    }

    private fun onProfileClicked() {
        binding.Profile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
    }

    private fun navigatingToSearchFragment() {
        binding.searchCv.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
    }

    private fun setAllCategories() {
        val categoryList = ArrayList<Category>()

        for (i in 0 until Constants.allProductsCategory.size) {
            categoryList.add(
                Category(
                    Constants.allProductsCategory[i],
                    Constants.allProductsCategoryIcon[i]
                ))
        }
        // Passing 'onCategoryIconClicked' as function for seperating the logic
        // Passing the category list to the adapter aswell as the actual 'category' title to be displayed
        binding.rvCategories.adapter = AdapterCategory(categoryList, ::onCategoryIconClicked)
    }

    // When user clicks a category on the home screen, redirect him to all the products of that category
    fun onCategoryIconClicked(category: Category) {
        val bundle = Bundle()
        bundle.putString("category", category.title)
        findNavController().navigate(R.id.action_homeFragment_to_categoryFragment, bundle)
    }

    fun seeAllButtonClicked(productType : Bestseller) {
        val bsSeeAllBinding = BsSeeAllBinding.inflate(LayoutInflater.from(requireContext()))

        val bs = BottomSheetDialog(requireContext())
        bs.setContentView(bsSeeAllBinding.root)

        adapterProduct = AdapterProduct(::onAddButtonClicked, ::onIncrementButtonClicked, ::onDecrementButtonClicked)
        bsSeeAllBinding.rvProducts.adapter = adapterProduct
        adapterProduct.differ.submitList(productType.products)

        bs.show()
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

    private fun SetStatusBarColor() {
        activity?.window?.apply {
            val statusBarColors = ContextCompat.getColor(requireContext(), R.color.orange)
            statusBarColor = statusBarColors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context) // attach fragment to activity

        if(context is CartListener) {
            cartListener = context
        } else {
            throw ClassCastException("Please implement cart listener")
        }
    }
}