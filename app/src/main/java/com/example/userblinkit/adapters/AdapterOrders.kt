package com.example.userblinkit.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.userblinkit.R
import com.example.userblinkit.databinding.ItemViewOrdersBinding
import com.example.userblinkit.models.OrderedItems

class AdapterOrders(val context: Context, val onOrderItemViewClicked: (OrderedItems) -> Unit) : RecyclerView.Adapter<AdapterOrders.OrdersViewHolder>() {
    class OrdersViewHolder(val binding : ItemViewOrdersBinding) : ViewHolder(binding.root)

    val diffutil = object : DiffUtil.ItemCallback<OrderedItems>() {
        override fun areItemsTheSame(oldItem: OrderedItems, newItem: OrderedItems): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: OrderedItems, newItem: OrderedItems): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffutil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdersViewHolder {
        return OrdersViewHolder(ItemViewOrdersBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    // holder here represents each order since its of type OrdersViewHolder
    override fun onBindViewHolder(holder: OrdersViewHolder, position: Int) {
        val order = differ.currentList[position]

        holder.binding.apply {
            tvOrderTitle.text = order.itemTitle
            tvOrderDate.text = order.itemDate
            tvOrderAmount.text = "₹${order.itemPrice}"

            when (order.itemStatus) {
                0 -> {
                    tvOrderStatus.text = "Ordered"
                    // ContextCompat is a safe way to set colors
                    tvOrderStatus.backgroundTintList = ContextCompat.getColorStateList(context, R.color.yellow)
                }
                1 -> {
                    tvOrderStatus.text = "Recived"
                    tvOrderStatus.backgroundTintList = ContextCompat.getColorStateList(context, R.color.blue)
                }
                2 -> {
                    tvOrderStatus.text = "Dispatched"
                    tvOrderStatus.backgroundTintList = ContextCompat.getColorStateList(context, R.color.green)
                }
                3 -> {
                    tvOrderStatus.text = "Delivered"
                    tvOrderStatus.backgroundTintList = ContextCompat.getColorStateList(context, R.color.orange)
                }
            }
        }
        holder.itemView.setOnClickListener {
            onOrderItemViewClicked(order)
        }
    }
}