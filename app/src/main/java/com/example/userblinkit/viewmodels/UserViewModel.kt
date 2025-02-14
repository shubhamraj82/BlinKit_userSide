package com.example.userblinkit.viewmodels

import androidx.lifecycle.ViewModel
import com.example.userblinkit.models.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserViewModel : ViewModel() {

    fun fetchAllTheProducts(): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts")

        // ValueEventListener is an interface that listens for data changes in Firebase.
        val eventListener = object : ValueEventListener {

            // This method is triggered every time the data at "AllProducts" changes (new products added, updated, or deleted).
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) { // Iterates through each product entry inside "AllProducts"
                    val prod =
                        product.getValue(Product::class.java) // convert each snapshot into a Project object
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

    fun getCategoryProduct(category: String): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins")
            .child("ProductCategory/${category}")

        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children) { // Iterates through each product entry inside "AllProducts"
                    val prod =
                        product.getValue(Product::class.java) // convert each snapshot into a Project object
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
}