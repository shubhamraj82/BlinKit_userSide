package com.example.userblinkit.viewmodels

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.userblinkit.Utils.Constants
import com.example.userblinkit.Utils.Utils
import com.example.userblinkit.api.ApiUtilities
import com.example.userblinkit.models.Bestseller
import com.example.userblinkit.models.Orders
import com.example.userblinkit.models.Product
import com.example.userblinkit.roomdb.CartProductDao
import com.example.userblinkit.roomdb.CartProducts
import com.example.userblinkit.roomdb.CartProductsDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow

// passing application because we will be using sharedPreferences to save the cart locally
class UserViewModel(application: Application) : AndroidViewModel(application) {

    // Initialization
    // Shared Preferences is a lightweight way to store small amounts of key-value data persistently in Android
    val sharedPreferences : SharedPreferences = application.getSharedPreferences("My_Pref", MODE_PRIVATE)
    val cartProductsDao : CartProductDao = CartProductsDatabase.getDatabaseInstance(application).cartProductsDao()

    private val _paymentStatus = MutableStateFlow<Boolean>(false)
    val paymentStatus = _paymentStatus

    // ROOM DB
    suspend fun insertCartProduct(products : CartProducts) {
        cartProductsDao.insertCartProduct(products)
    }

    suspend fun updateCartProduct(products : CartProducts) {
        cartProductsDao.updateCartProduct(products)
    }

    suspend fun deleteCartProduct(productId: String) {
        cartProductsDao.deleteCartProduct(productId)
    }

    // This function is a query to DAO, which loads database when app starts
    fun getAll(): LiveData<List<CartProducts>> {
        return cartProductsDao.getAllCartProducts()
    }

    // Function which allows us to delete the products in the cart when on Ordering Page
    suspend fun deleteCartProducts() {
        cartProductsDao.deleteCartProducts()
    }

    // Firebase call for fetching all the products from the database
    // Flow is used to fetch the data from the database asynchronously and since it emits only when collect is called
    // callbackFlow is used to convert callback-based APIs into Flow since ValueEventListener is is a callback based API
    fun fetchAllTheProducts(): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts")

        // ValueEventListener is an interface that listens for data changes in Firebase in realtime
        val eventListener = object : ValueEventListener {

            // This method is triggered every time the data at "AllProducts" changes (new products added, updated, or deleted).
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) { // Iterates through each product entry inside "AllProducts"
                    val prod = product.getValue(Product::class.java) // convert each snapshot into a Project object
                    products.add(prod!!)
                }
                // Since callbackFlow works asynchronously, trySend(products) ensures that new data is immediately
                // available to whoever is collecting the Flow.
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) { }
        }

        // This attaches eventListener to db, meaning Firebase will continuously listen for changes in "AllProducts"
        db.addValueEventListener(eventListener)

        //  ensures that when the Flow collector stops listening, we remove the Firebase listener.
        awaitClose {
            db.removeEventListener(eventListener)
        }
    }

    // This function is used in Orders Fragment to get all the Orders from the user
    // callbackFlow is used to convert callback-based APIs into Flow since ValueEventListener is is a callback based API
    fun getAllOrders() : Flow<List<Orders>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("Orders").orderByChild("orderStatus")

        // ValueEventListener is an interface that listens for data changes in Firebase in realtime
        val eventListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val orderList = ArrayList<Orders>()

                for (orders in snapshot.children) {
                    val order = orders.getValue(Orders::class.java)
                    // Only get the orders of the current user
                    if (order?.orderingUserId == Utils.getCurrentUserId()) {
                        orderList.add(order!!)
                    }
                }
                trySend(orderList)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }
        db.addValueEventListener(eventListener)
        awaitClose { // remove the event listener
            db.removeEventListener(eventListener)
        }
    }

    // function call to get the products of a specific category
    // callbackFlow is used to convert callback-based APIs into Flow since ValueEventListener is is a callback based API
    fun getCategoryProduct(category: String): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins")
            .child("ProductCategory/${category}")

        // Using eventListener so that everytime a new product is added to that category by admin our category is also updated
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) { // Iterates through each product entry inside "AllProducts"
                    val prod = product.getValue(Product::class.java) // convert each snapshot into a Project object
                    products.add(prod!!)
                }
                // Since callbackFlow works asynchronously, trySend(products) ensures that new data is immediately
                // available to whoever is collecting the Flow.
                trySend(products)
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }

        // This attaches eventListener to db, meaning Firebase will continuously listen for changes in "AllProducts"
        db.addValueEventListener(eventListener)

        //  ensures that when the Flow collector stops listening, we remove the Firebase listener.
        awaitClose {
            db.removeEventListener(eventListener)
        }
    }

    // When user clicks on the order in the orderFragment give him the order details
    fun getOrderedProducts(orderId : String) : Flow<List<CartProducts>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(orderId)
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(Orders::class.java)
                trySend(order?.orderList!!)
            }

            override fun onCancelled(error: DatabaseError) { }
        }

        db.addValueEventListener(eventListener)
        awaitClose {
            db.removeEventListener(eventListener)
        }
    }

    // Update the item count in the Firebase realtime database
    fun updateItemCount(product: Product, itemCount: Int) {
        FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts/${product.productRandomId}").child("itemCount").setValue(itemCount)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${product.productCategory}/${product.productRandomId}").child("itemCount").setValue(itemCount)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductType/${product.productType}/${product.productRandomId}").child("itemCount").setValue(itemCount)
    }

    fun saveProductsAfterOrder(stock : Int, product : CartProducts) {
        // First, setting the itemCount of that item as zero after order has been placed
        FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts/${product.productId}").child("itemCount").setValue(0)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${product.productCategory}/${product.productId}").child("itemCount").setValue(0)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductType/${product.productType}/${product.productId}").child("itemCount").setValue(0)

        // Secondly, setting the updated stock in the database
        FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts/${product.productId}").child("productStock").setValue(stock)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${product.productCategory}/${product.productId}").child("productStock").setValue(stock)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductType/${product.productType}/${product.productId}").child("productStock").setValue(stock)

    }

    // Save the address in the Realtime DB
    fun saveUserAddress(address : String) {
        FirebaseDatabase.getInstance().getReference("All Users")
            .child("Users")
            .child(Utils.getCurrentUserId()!!)
            .child("userAddress")
            .setValue(address)
    }

    // callback is used here because DB doesn't return data immediately, therefore whenever DB returns the
    // data, it gets stored in 'callback'
    // callback here is a function which takes in String parameter and has a Unit return type
    fun getUserAddress(callback : (String?) -> Unit) {
        val db = FirebaseDatabase.getInstance().getReference("All Users")
            .child("Users")
            .child(Utils.getCurrentUserId()!!)
            .child("userAddress")

        // read the data once using 'addListenerForSingleValueEvent'
        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val address = snapshot.getValue(String::class.java)
                    callback(address)
                } else {
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    fun saveAddress(address : String) {
        FirebaseDatabase.getInstance().getReference("All Users")
            .child("Users")
            .child(Utils.getCurrentUserId()!!)
            .child("userAddress")
            .setValue(address)
    }

    fun logOutUser() {
        FirebaseAuth.getInstance().signOut()
    }

    fun saveOrderProducts(order : Orders) {
        FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(order.orderId!!).setValue(order)
    }

    fun fetchProductType() : Flow<List<Bestseller>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins/ProductType")

        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productTypeList = ArrayList<Bestseller>()
                for (productType in snapshot.children) {

                    // Since we have to display the productType of bestselling products ex. Ice Cream
                    val productTypeName = productType.key

                    // To store all the products of that productType
                    val productList = ArrayList<Product>()

                    for (products in productType.children) {
                        val product = products.getValue(Product::class.java)
                        productList.add(product!!)
                    }

                    val bestseller = Bestseller(productType = productTypeName, products = productList)
                    productTypeList.add(bestseller)
                }

                trySend(productTypeList)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }
        db.addValueEventListener(eventListener)
        awaitClose{db.removeEventListener(eventListener)}
    }


    // sharePreferences
    fun savingCartItemCount(itemCount : Int) {
        sharedPreferences.edit().putInt("itemCount", itemCount).apply()
    }

    // using livedata to fetch the data from the sharedPreference file which is saved on our mobile
    fun fetchTotalItemCount(): MutableLiveData<Int> {
        val totalItemCount = MutableLiveData<Int>()
        totalItemCount.value = sharedPreferences.getInt("itemCount", 0)
        return totalItemCount
    }

    fun saveAddressStatus() {
        sharedPreferences.edit().putBoolean("addressStatus", true).apply()
    }

    // Function to check if user already has his address saved in sharedPreference or not
    fun getAddressStatus() : MutableLiveData<Boolean> {
        val status = MutableLiveData<Boolean>()
        status.value = sharedPreferences.getBoolean("addressStatus", false)
        return status
    }

    // Retrofit call for checking the status of the payment

    suspend fun checkPayment(headers : Map<String, String>) {
        val res = ApiUtilities.statusApi.checkStatus(headers, Constants.MERCHANT_ID, Constants.merchantTransactionId)
        _paymentStatus.value = res.body() != null && res.body()!!.success
    }
}