package com.example.userblinkit.auth

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.userblinkit.R
import com.example.userblinkit.Utils
import com.example.userblinkit.databinding.FragmentSigninBinding

class signinFragment : Fragment() {

    private lateinit var binding: FragmentSigninBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        SetStatusBarColor()
        binding = FragmentSigninBinding.inflate(layoutInflater)
        SetStatusBarColor()
        getUserNumber()
        onContinueButtonClick()

        return binding.root
    }

    // functionality to move to the next fragment i.e OTP fragment
    private fun onContinueButtonClick() {
        binding.continuebtn.setOnClickListener() {
            val number = binding.etUserNumber.text.toString()

            if(number.isEmpty() || number.length != 10) {
                Utils.showToast(requireContext(), "Please enter a valid phone number")
            } else {
                val bundle = Bundle()
                bundle.putString("number", number)
                findNavController().navigate(R.id.action_signinFragment_to_OTPFragment, bundle)
            }

        }
    }

    // functionality to change 'continue' button color when user enters a valid number
    private fun getUserNumber() {
        binding.etUserNumber.addTextChangedListener ( object : TextWatcher {
            // Not needed but had to  implement because of Text Watcher interface
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            // Whenever text inside  an ET changes this function is called
                override fun onTextChanged(number : CharSequence?, start: Int, before: Int, count: Int) {
                if (number?.length == 10) {
                    // Context compat - helps in handling context-related operations safely across different API levels.
                    // first get the color from the required context and then set it as backgroung
                    binding.continuebtn.setBackgroundColor(ContextCompat.getColor(requireContext(),
                        R.color.green
                    ))
                } else {
                    binding.continuebtn.setBackgroundColor(ContextCompat.getColor(requireContext(),
                        R.color.grayish_blue
                    ))
                }
            }
            // Called after text has changed
            override fun afterTextChanged(s: Editable?) {}
        })
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