package com.example.userblinkit.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.userblinkit.R
import com.example.userblinkit.adapters.AdapterProduct
import com.example.userblinkit.databinding.FragmentHomeBinding
import com.example.userblinkit.databinding.FragmentSearchBinding
import com.example.userblinkit.models.Product
import com.example.userblinkit.viewmodels.UserViewModel
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    val viewModel : UserViewModel by viewModels()
    private lateinit var binding : FragmentSearchBinding
    private lateinit var adapterProduct: AdapterProduct

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
                adapterProduct = AdapterProduct() // adapter for the recycler view which shows the product information
                binding.rvProducts.adapter = adapterProduct // set the adapter
                adapterProduct.differ.submitList(it) // pass the list of products to the adapter
                adapterProduct.originalList = it as ArrayList<Product> // passing the original list for searching purpose
                binding.shimmerViewContainer.visibility = View.GONE
            }
        }
    }
}