package com.example.userblinkit.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.userblinkit.Utils.CartListener
import com.example.userblinkit.adapters.AdapterCartProducts
import com.example.userblinkit.databinding.ActivityUsersMainBinding
import com.example.userblinkit.databinding.BsCartProductsBinding
import com.example.userblinkit.roomdb.CartProducts
import com.example.userblinkit.viewmodels.UserViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

// Since activity_users_main can only be used here since its an activity, we make an interface to add
// the cart functionality

class UsersMainActivity : AppCompatActivity(), CartListener {

    private lateinit var binding: ActivityUsersMainBinding
    private val viewModel : UserViewModel by viewModels()
    private lateinit var cartProductList: List<CartProducts> // CartProducts is the room table
    private lateinit var adapterCartProducts: AdapterCartProducts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getAllCartProducts()
        getTotalItemCount() // Show cart if it had items previously stored in it
        onCartClicked()
        onNextButtonClicked()
    }

    private fun onNextButtonClicked() {
        binding.btnNext.setOnClickListener {
            startActivity(Intent(this, OrderPlaceActivity::class.java))
        }
    }

    // When app starts, fetch all the products from the room database
    private fun getAllCartProducts() {
        viewModel.getAll().observe(this){
            cartProductList = it
        }
    }

    // When cart is clicked, show the cart layout using bottom sheet
    override fun onCartClicked() {
        binding.llItemCart.setOnClickListener {
            val bsCartProductsBinding = BsCartProductsBinding.inflate(LayoutInflater.from(this))

            //BottomSheet is a UI component that slides up from the bottom of the screen to display additional content
            val bs = BottomSheetDialog(this)
            bs.setContentView(bsCartProductsBinding.root)

            bsCartProductsBinding.tvNumberOfProductCount.text = binding.tvNumberOfProductCount.text

            // When the BottomSheet shows, if user clicks on next button take him to Order activity
            bsCartProductsBinding.btnNext.setOnClickListener {
                startActivity(Intent(this, OrderPlaceActivity::class.java))
                finish()
            }

            adapterCartProducts = AdapterCartProducts()
            bsCartProductsBinding.rvProductItems.adapter = adapterCartProducts
            adapterCartProducts.differ.submitList(cartProductList)
            bs.show()


        }
    }

    // After order has been placed, this function will be called to make the cart layout invisible
    override fun hideCartLayout() {
        binding.llCart.visibility = View.GONE
        binding.tvNumberOfProductCount.text = "0"
    }

    // When app is opened, fetch if shared preferences has any cart product count stored
    // if yes then show the cart layout
    private fun getTotalItemCount() {
        // fetchTotalItemCount fetches the cart item count from the sharedPreferences and returns LiveData
        // This LiveData is observed for changes
        // The item count is stored in sharedPreferences
        viewModel.fetchTotalItemCount().observe(this) {
            if (it > 0) {
                binding.llCart.visibility = View.VISIBLE
                binding.tvNumberOfProductCount.text = it.toString()
            } else {
                binding.llCart.visibility = View.GONE
            }
        }
    }

    // Display the cart layout based on the the number of items in the cart
    override fun showCartLayout(itemCount : Int) {
        val previousCount = binding.tvNumberOfProductCount.text.toString().toInt()
        val updatedCount  = previousCount + itemCount

        if (updatedCount > 0) {
            binding.llCart.visibility = View.VISIBLE
            binding.tvNumberOfProductCount.text = updatedCount.toString()
        } else {
            binding.llCart.visibility = View.GONE
            binding.tvNumberOfProductCount.text = "0"
        }
    }

    // Whenever 'add', '+' or '-' button is clicked, this function is called which
    // save the item count in sharedPreferences
    override fun savingCartItemCount(itemCount: Int) {
        // first fetch the value from the sharedPreference
        viewModel.fetchTotalItemCount().observe(this) {
            // then save if the value changes
            viewModel.savingCartItemCount(it + itemCount)
        }
    }
}