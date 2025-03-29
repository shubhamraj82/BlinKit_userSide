package com.example.userblinkit.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.userblinkit.Utils.CartListener
import com.example.userblinkit.Utils.Constants
import com.example.userblinkit.R
import com.example.userblinkit.Utils.Utils
import com.example.userblinkit.adapters.AdapterCartProducts
import com.example.userblinkit.databinding.ActivityOrderPlaceBinding
import com.example.userblinkit.databinding.AddressLayoutBinding
import com.example.userblinkit.models.Orders
import com.example.userblinkit.viewmodels.UserViewModel
import com.phonepe.intent.sdk.api.B2BPGRequest
import com.phonepe.intent.sdk.api.B2BPGRequestBuilder
import com.phonepe.intent.sdk.api.PhonePe
import com.phonepe.intent.sdk.api.PhonePeInitException
import com.phonepe.intent.sdk.api.models.PhonePeEnvironment
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.Charset
import java.security.MessageDigest


class OrderPlaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderPlaceBinding
    private val viewModel : UserViewModel by viewModels()
    private lateinit var adapterCartProducts: AdapterCartProducts
    private lateinit var b2BPGRequest: B2BPGRequest
    private var cartListener : CartListener?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SetStatusBarColor()
        backToUserMainActivity()
        getAllCartProducts()
        initializePhonePay()
        onPlaceOrderClicked()

    }

    // Setting up PhonePe using Docs
    private fun initializePhonePay() {
        val data = JSONObject()
        PhonePe.init(this, PhonePeEnvironment.SANDBOX, Constants.MERCHANT_ID, null)

        data.put("merchantId", Constants.MERCHANT_ID)
        data.put("merchantTransactionId", Constants.merchantTransactionId)
        data.put("amount", 200)
        data.put("mobileNumber", "8922979179")
        data.put("callbackUrl", "https://webhook.site/callback-url")

        val paymentInstrument = JSONObject()
        paymentInstrument.put("type", "PAY_PAGE")
        paymentInstrument.put("targetApp", "com.phonepe.simulator") // Not docs

        data.put("paymentInstrument", paymentInstrument) // Not docs

        val deviceContext = JSONObject()
        deviceContext.put("deviceOS", "ANDROID")
        data.put("deviceContext", deviceContext)

        val payloadBase64 = Base64.encodeToString(
            data.toString().toByteArray(Charset.defaultCharset()), Base64.NO_WRAP
        )

        val checksum = sha256(payloadBase64 + Constants.apiEndPoint + Constants.SALT_KEY) + "###1"

        b2BPGRequest = B2BPGRequestBuilder()
            .setData(payloadBase64)
            .setChecksum(checksum)
            .setUrl(Constants.apiEndPoint)
            .build()
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it ->
            str + "%02x".format(it)
        }
    }

    // This function first checks if the user's address has been saved in shared preferences using LiveData
    private fun onPlaceOrderClicked() {
        binding.btnNext.setOnClickListener {
            viewModel.getAddressStatus().observe(this) { status ->
                if(status) {
                    getPaymentView()
                } else { // If user doesn't have address saved, save it in Realtime DB
                    val addressLayoutBinding = AddressLayoutBinding.inflate(layoutInflater)

                    val alertDialog = AlertDialog.Builder(this)
                        .setView(addressLayoutBinding.root)
                        .create()
                    alertDialog.show()

                    addressLayoutBinding.btnAddAddress.setOnClickListener {
                        // pass alertDialog to hide it, and addressLayoutBinding to save the address
                        saveAddress(alertDialog, addressLayoutBinding)
                    }
                }
            }
        }
    }

    val phonePayView = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            checkStatus()
        }
    }

    // Checking if payment is successful if yes, place order - save order in DB
    private fun checkStatus() {
        val xVerify = sha256("/pg/v1/status/${Constants.MERCHANT_ID}/${Constants.merchantTransactionId}${Constants.SALT_KEY}") + "###1"
        val headers = mapOf(
            "Content-Type" to "application/json",
            "X-VERIFY" to xVerify,
            "X-MERCHANT" to Constants.MERCHANT_ID
        )

        lifecycleScope.launch {
            viewModel.checkPayment(headers)
            viewModel.paymentStatus.collect { status ->
                if (status) {
                    Utils.showToast(this@OrderPlaceActivity, "Payment Completed")

                    // Order save, delete products
                    saveOrder()
                    viewModel.deleteCartProducts() // delete cart products from Room DB after order has been saved
                    // After order has been placed, hide the Cart Layout
                    viewModel.savingCartItemCount(0)
                    cartListener?.hideCartLayout()

                    Utils.hideDialog()
                    startActivity(Intent(this@OrderPlaceActivity, UsersMainActivity::class.java))
                    finish()
                } else {
                    Utils.showToast(this@OrderPlaceActivity, "Payment Failed")
                }
            }
        }
    }

    private fun saveOrder() {
        viewModel.getAll().observe(this) {cartProductsList ->
            if (cartProductsList.isNotEmpty()) {
                // address here is a callback, we use callback because DB doesn't return data immediately
                // 'address' here will collect the data async after
                viewModel.getUserAddress { address -> // address is a function parameter which stores the retrieved data
                    val order = Orders(
                        // Now we have both cart products and user's address, set the Order details
                        orderId = Utils.getRandomId(), orderList = cartProductsList,
                        userAddress = address, orderStatus = 0, orderDate = Utils.getCurrentDate(),
                        orderingUserId = Utils.getCurrentUserId()
                    )
                    viewModel.saveOrderProducts(order)
                }
                for (products in cartProductsList) {
                    val count = products.productCount
                    val stock = products.productStock?.minus(count!!)
                    if (stock != null) {
                        viewModel.saveProductsAfterOrder(stock, products)
                    }
                }
            }
        }
    }

    // Get the intent from the apk for payment view
    private fun getPaymentView() {
        try {
            PhonePe.getImplicitIntent(this, b2BPGRequest, "com.phonepe.simulator")
                .let {
                    if (it != null) { // Null check not in video
                        phonePayView.launch(it)
                    }
                }
        }
        catch (e : PhonePeInitException) {
            Utils.showToast(this, e.message.toString())
        }
    }

    // Fetch the address from the layout and then save it in Realtime DB
    private fun saveAddress(alertDialog: AlertDialog, addressLayoutBinding: AddressLayoutBinding) {
        Utils.showDialog(this, "Processing")
        val userPinCode = addressLayoutBinding.etPinCode.text.toString()
        val userPhoneNo = addressLayoutBinding.etPhoneNo.text.toString()
        val userState = addressLayoutBinding.etState.text.toString()
        val userDistrict = addressLayoutBinding.etDistrict.text.toString()
        val userAddress = addressLayoutBinding.etAddress.text.toString()

        val address = "$userPinCode, $userDistrict($userState), $userAddress, $userPhoneNo"

        lifecycleScope.launch {
            viewModel.saveUserAddress(address)
            viewModel.saveAddressStatus()
        }
        Utils.showToast(this,"Saved...")
        alertDialog.dismiss()

        getPaymentView()
    }

    private fun backToUserMainActivity() {
        binding.tbOrderFragment.setNavigationOnClickListener {
            startActivity(Intent(this, UsersMainActivity::class.java))
        }
    }

    // When app starts, fetch all the products from the room database
    private fun getAllCartProducts() {
        // getAll function gets all the cart products from the ROOM db
        viewModel.getAll().observe(this) { cartProductList ->

            // Setting up the adapter for the recycler view
            adapterCartProducts = AdapterCartProducts()
            binding.rvProductsItem.adapter = adapterCartProducts
            adapterCartProducts.differ.submitList(cartProductList)

            var totalPrice = 0

            for(products in cartProductList) {
                // substring to remove the rupee symbol
                val price = products.productPrice?.substring(1)?.toInt()
                val itemCount = products.productCount!!

                totalPrice += (price?.times(itemCount)!!)
            }

            binding.tvSubTotal.text = totalPrice.toString()

            // For the delivery fee
            if (totalPrice < 200) {
                binding.tvDeliveryCharge.text = "₹15"
                totalPrice += 15
            }

            binding.tvGrandTotal.text = totalPrice.toString()
        }
    }

    fun SetStatusBarColor() {
        window?.apply {
            val statusBarColors = ContextCompat.getColor(this@OrderPlaceActivity, R.color.yellow)
            statusBarColor = statusBarColors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}