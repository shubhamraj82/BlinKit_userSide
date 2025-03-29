package com.example.userblinkit.Utils

interface CartListener {

    fun showCartLayout(itemCount : Int)
    fun savingCartItemCount(itemCount : Int)
    fun onCartClicked()
    fun hideCartLayout()
}