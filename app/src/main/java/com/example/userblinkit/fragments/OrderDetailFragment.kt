package com.example.userblinkit.fragments

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
import com.example.userblinkit.R
import com.example.userblinkit.adapters.AdapterCartProducts
import com.example.userblinkit.databinding.FragmentOrderDetailBinding
import com.example.userblinkit.roomdb.CartProducts
import com.example.userblinkit.viewmodels.UserViewModel
import kotlinx.coroutines.launch

class OrderDetailFragment : Fragment() {

    private val viewModel : UserViewModel by viewModels()
    private lateinit var binding : FragmentOrderDetailBinding
    private lateinit var adapterCartProducts: AdapterCartProducts
    private var status : Int = 0
    private var orderId : String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderDetailBinding.inflate(layoutInflater)

        getValues() // Getting order status and id from the OrdersFragment
        SetStatusBarColor()
        settingStatus()
        onBackButtonClicked()
        lifecycleScope.launch { getOrderedProducts() }

        return binding.root
    }

    // Function to get the ordered products and display them in recycler view
    suspend fun getOrderedProducts() {
        viewModel.getOrderedProducts(orderId).collect { cartList ->
            adapterCartProducts = AdapterCartProducts()
            binding.rvProductItems.adapter = adapterCartProducts
            adapterCartProducts.differ.submitList(cartList)
        }
    }

    // setting the color of the imageView in the OrderDetailFragment based on the status
    private fun settingStatus() {
        val statusToViews = mapOf(
            0 to listOf(binding.iv1),
            1 to listOf(binding.iv1, binding.iv2, binding.view1),
            2 to listOf(binding.iv1, binding.iv2, binding.view1, binding.iv3, binding.view2),
            3 to listOf(
                binding.iv1,
                binding.iv2,
                binding.view1,
                binding.iv3,
                binding.view2,
                binding.iv4,
                binding.view3
            )
        )

        val viewsToTint = statusToViews.getOrDefault(status, emptyList())
        for (view in viewsToTint) {
            view.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.blue)
        }
    }

    // Getting order status and id from the OrdersFragment
    private fun getValues() {
        val bundle = arguments
        status = bundle?.getInt("status")!!
        orderId = bundle?.getString("orderId")!!
    }

    private fun onBackButtonClicked() {
        binding.tbOrderDetailFragment.setOnClickListener {
            findNavController().navigate(R.id.action_orderDetailFragment_to_ordersFragment)
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
}