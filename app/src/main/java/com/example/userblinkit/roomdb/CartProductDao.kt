package com.example.userblinkit.roomdb

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CartProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCartProduct(cartProducts: CartProducts)

    @Update
    fun updateCartProduct(cartProducts: CartProducts)

    // Room database stores the data based on DAO, so when we close app it stores the data
    // but when we reopen the app, this data isn't loaded by default to our app
    // Therefore we use this function to fetch all the data from the database when app starts
    @Query("SELECT * FROM CartProducts")
    fun getAllCartProducts() : LiveData<List<CartProducts>>

    @Query("DELETE FROM CartProducts WHERE productId = :productId")
    suspend fun deleteCartProduct(productId: String)

    // Delete all the products from the cart after order has been placed
    @Query("DELETE FROM CartProducts")
    suspend fun deleteCartProducts()

}