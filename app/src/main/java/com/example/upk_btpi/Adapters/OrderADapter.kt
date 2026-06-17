package com.example.upk_btpi.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.upk_btpi.Models.Order.OrderDto
import com.example.upk_btpi.databinding.ItemOrderBinding

class OrderADapter(
    private var orders: List<OrderDto>,
    private val onItemClick:(OrderDto) -> Unit
    ): RecyclerView.Adapter<OrderADapter.OrderViewHolder>() {
    class OrderViewHolder(private val binding: ItemOrderBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(order: OrderDto, onClick:(OrderDto)-> Unit) {
            binding.textViewDate.text = order.date ?:"нет даты создания"
            binding.textViewPRoductName.text = order.productDto.productName ?:"нет названия"
            binding.textViewStatus.text = order.statusName

            binding.root.setOnClickListener { onClick(order) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) { holder.bind(orders[position], onItemClick) }

    fun updateOrders(newOrders: List<OrderDto>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    override fun getItemCount()  = orders.size
}