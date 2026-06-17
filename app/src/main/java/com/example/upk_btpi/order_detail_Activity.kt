package com.example.upk_btpi

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.transition.Visibility
import com.example.upk_btpi.Models.Order.OrderDto
import com.example.upk_btpi.Models.Order.UpdateOrderDto
import com.example.upk_btpi.Models.StatusOrder.StatusOrderDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.databinding.ActivityOrderDetailBinding
import kotlinx.coroutines.launch
import okhttp3.internal.threadFactory

class order_detail_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private val authRepository = AuthRepository()
    private var isEditMode = false
    private var userRole: String?=null
    private lateinit var currentOrderId: String
    private lateinit var oldOrder: OrderDto

    private var statusOrders:List<StatusOrderDto> = emptyList()
    private var selectedStatusId: String ?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получение данных из SharedPreferences
        val orderPrefs = getSharedPreferences("order_prefs",0)
        val authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        currentOrderId = orderPrefs.getString("selected_orderId", null).toString()
        userRole = authPrefs.getString("user_role", null)

        if(currentOrderId == null) {finish();return}

        //загрузка данных
        loadOrderDetails(currentOrderId)
        if(userRole !="DefaultUser") {
            binding.editButton.visibility = View.VISIBLE
            binding.buttonDelete.visibility = View.VISIBLE
        }

        binding.editButton.setOnClickListener {
            if(userRole == "Manager"){
                binding.textViewCustomersComment.visibility = View.GONE
                binding.editTextCustomersComment.visibility = View.VISIBLE
                binding.editTextCustomersComment.setText( oldOrder.customersComment.toString())

                //spinner
                binding.textViewStatusName.visibility = View.GONE
                binding.spinnerStatusName.visibility = View.VISIBLE
            }
            else if(userRole == "Admin"){
                binding.editButton.visibility = View.VISIBLE

                //коментарий выполняющиего
                binding.textViewCustomersComment.visibility = View.GONE
                binding.editTextCustomersComment.visibility = View.VISIBLE
                binding.editTextCustomersComment.setText(oldOrder.customersComment.toString())
                //коментарий заказчика
                binding.textViewUserComment.visibility = View.GONE
                binding.editTextUserComment.visibility = View.VISIBLE
                binding.editTextUserComment.setText(oldOrder.userComment.toString())
                //spinner
                binding.textViewStatusName.visibility = View.GONE
                binding.spinnerStatusName.visibility = View.VISIBLE

                loadSpinner()
            }
            binding.ButtonSave.visibility = View.VISIBLE
        }

        binding.ButtonSave.setOnClickListener { saveChanges() }
        binding.backBtn.setOnClickListener {  finish()}
        binding.buttonDelete.setOnClickListener { deleteOrder() }
    }


    private fun loadSpinner() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllStatusOrder()
                if(response.isSuccessful && response.body()!=null) {
                    statusOrders =response.body()!!.statusOrders
                    setupSpinner()
                }else{

                }
            }catch (e: Exception) {}
        }
    }

    private fun setupSpinner(){
        if(statusOrders.isEmpty()) return

        val statusNames = statusOrders.map { it.statusName?: "без названия" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerStatusName.adapter = adapter

        oldOrder?.let { order->
            val currentStatusIndex  = statusOrders.indexOfFirst { it.statusName == order.statusName }
            if(currentStatusIndex >= 0) {binding.spinnerStatusName.setSelection(currentStatusIndex)
            selectedStatusId = order.statusName}
        }

        //обработчик выбора
        binding.spinnerStatusName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStatusId = statusOrders[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { selectedStatusId = null }
        }
    }




    private fun loadOrderDetails(currentOrderId: String) {
        lifecycleScope.launch {
            val result = authRepository.getOrderById(currentOrderId)
            result.onSuccess {
                order -> oldOrder=order
                displayOrder(order)
            }
            result.onFailure {Toast.makeText(this@order_detail_Activity, "❌ Ошибка получения данных", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun displayOrder(order: OrderDto){
        //binding.textViewId.text = order.id
        binding.textViewExecutorId.text = order.executorId
        binding.textViewCustomerId.text = order.customerId
        binding.textViewDate.text = order.date
        binding.textViewStatusName.text = order.statusName
        binding.textViewUserComment.text = order.userComment
        binding.textViewCustomersComment.text = order.customersComment
        binding.textViewProductName.text = order.productDto.productName
    }

    private fun  saveChanges() {
        val editOrder = UpdateOrderDto(
            id = oldOrder.id,
            productId = oldOrder.productDto.id,
            statusOrderId = selectedStatusId.toString() ?: oldOrder.statusName.toString(),
            customersComment = binding.editTextCustomersComment.text.toString() ?: binding.textViewCustomersComment.text.toString(),
            userComment = binding.editTextUserComment.text.toString() ?: binding.editTextUserComment.text.toString()
        )
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updateOrder(editOrder)
                if (response.isSuccessful) {
                    Toast.makeText(this@order_detail_Activity, "успех", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this@order_detail_Activity, response.errorBody()?.string(), Toast.LENGTH_SHORT).show()
                }

            }catch (e: Exception) {}
        }
    }


    private fun deleteOrder() {
        AlertDialog.Builder(this)
            .setTitle("Удаление заказа")
            .setMessage("Вы уверены, что хотите удалить этот заказ? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ -> performDeleteOrder(currentOrderId) }
            .setNegativeButton("Отмена", null).show()
    }

    private fun performDeleteOrder(currentOrderId: String) {
        lifecycleScope.launch {
            try {
                println("🗑️ Удаление заказа: $currentOrderId")

                val response = RetrofitClient.apiService.deleteOrderById(currentOrderId)

                println("📥 Ответ сервера: ${response.code()}")

                if (response.isSuccessful) {
                    println("✅ заказ удалён")
                    Toast.makeText(this@order_detail_Activity, "✅ Отзыв удалён", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    println("❌ ОШИБКА: ${response.code()} - $error")
                    Toast.makeText(this@order_detail_Activity, "❌ Ошибка: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@order_detail_Activity, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }




}