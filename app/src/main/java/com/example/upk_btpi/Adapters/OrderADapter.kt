package com.example.upk_btpi.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.upk_btpi.Models.Order.OrderDto
import com.example.upk_btpi.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderADapter(
    private var orders: List<OrderDto>,
    private val onItemClick:(OrderDto) -> Unit
    ): RecyclerView.Adapter<OrderADapter.OrderViewHolder>() {
    class OrderViewHolder(private val binding: ItemOrderBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(order: OrderDto, onClick:(OrderDto)-> Unit) {
            binding.textViewDate.text = "заказ от:"+formatDate(order.date)
            binding.textViewPRoductName.text = order.productDto.productName ?:"нет названия"
            binding.textViewStatus.text = order.statusName

            binding.root.setOnClickListener { onClick(order) }
        }

        // Вспомогательная функция для форматирования даты
        private fun formatDate(dateString: String?): String {
            return try {
                if (dateString.isNullOrEmpty()) return "нет даты создания"

                // Парсим дату из ISO-формата (например: "2024-05-12T14:30:00")
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(dateString)

                // Форматируем в нужный вид: dd:MM:yyyy HH:mm
                val outputFormat = SimpleDateFormat("dd:MM:yyyy HH:mm", Locale.getDefault())
                outputFormat.format(date ?: Date())

            } catch (e: Exception) {
                // Если парсинг не удался — возвращаем исходную строку
                dateString ?: "нет даты создания"
            }
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