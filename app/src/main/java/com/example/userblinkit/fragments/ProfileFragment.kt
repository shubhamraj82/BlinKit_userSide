package com.example.userblinkit.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.userblinkit.R
import com.example.userblinkit.Utils.Utils
import com.example.userblinkit.activity.AuthMainActivity
import com.example.userblinkit.databinding.AddressBookLayoutBinding
import com.example.userblinkit.databinding.FragmentProfileBinding
import com.example.userblinkit.viewmodels.UserViewModel

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel : UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(layoutInflater)

        onBackButtonClicked()
        onOrdersLayoutClicked()
        SetStatusBarColor()
        onAddressBookClicked()
        onLogOutButtonClicked()

        return binding.root
    }

    private fun onLogOutButtonClicked() {
        binding.llLogOut.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            val alertDialog = builder.create()

                builder.setTitle("Log Out")
                .setMessage("Do you want to log out?")
                .setPositiveButton("Yes") {_, _ ->
                    viewModel.logOutUser()
                    startActivity(Intent(requireContext(), AuthMainActivity::class.java))
                    requireActivity().finish()
                }
                .setNegativeButton("No") {_, _ ->
                    alertDialog.dismiss()
                }

                    .show()
                    // If you click outside the layout, the alert dialog wont dismiss itself
                    // Force user to click on Yes or No to log out
                    .setCancelable(false)
        }
    }

    private fun onAddressBookClicked() {
        binding.llAddress.setOnClickListener {
            val addressBookLayoutBinding = AddressBookLayoutBinding.inflate(LayoutInflater.from(requireContext()))

            // Fetch the existing address
            viewModel.getUserAddress { address ->
                addressBookLayoutBinding.etAddress.setText(address.toString())
            }

            // Creating the alert dialog where user will edit his address
            val alertDialog = AlertDialog.Builder(requireContext())
                .setView(addressBookLayoutBinding.root)
                .create()
            alertDialog.show()

            // If user clicks on edit button, enable the text field to make it editable
            addressBookLayoutBinding.btnEdit.setOnClickListener {
                addressBookLayoutBinding.etAddress.isEnabled = true
            }

            // When user clicks on save button, save the address in the database
            addressBookLayoutBinding.btnSave.setOnClickListener {
                viewModel.saveAddress(addressBookLayoutBinding.etAddress.text.toString())
                alertDialog.dismiss()
                Utils.showToast(requireContext(), "Address has been updated!")
            }
        }
    }

    private fun onOrdersLayoutClicked() {
        binding.llOrders.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_ordersFragment)
        }
    }

    private fun onBackButtonClicked() {
        binding.tbProfileFragment.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_homeFragment)
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