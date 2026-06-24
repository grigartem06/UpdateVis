package com.example.upk_btpi

import OrderDto
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.upk_btpi.Models.Order.UpdateOrderDto
import com.example.upk_btpi.Models.StatusOrder.StatusOrderDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.databinding.ActivityOrderDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class order_detail_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private val authRepository = AuthRepository()
    private var isEditMode = false
    private var userRole: String? = null
    private lateinit var currentOrderId: String
    private lateinit var oldOrder: OrderDto

    private var statusOrders: List<StatusOrderDto> = emptyList()
    private var selectedStatusId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получение данных из SharedPreferences
        val orderPrefs = getSharedPreferences("order_prefs", 0)
        val authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        currentOrderId = orderPrefs.getString("selected_orderId", null).orEmpty()
        userRole = authPrefs.getString("user_role", null)

        if (currentOrderId.isEmpty()) {
            Toast.makeText(this, "⚠️ ID заказа не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //загрузка данных
        loadOrderDetails(currentOrderId)

        if (userRole != "DefaultUser") {
            binding.editButton.visibility = View.VISIBLE
            binding.buttonDelete.visibility = View.VISIBLE
        }

        binding.editButton.setOnClickListener {
            binding.editButton.visibility = View.GONE
            if (userRole == "Manager") {
                binding.textViewCustomersComment.visibility = View.GONE
                binding.editTextCustomersComment.visibility = View.VISIBLE
                binding.editTextCustomersComment.setText(oldOrder.customersComment.orEmpty())

                //spinner
                binding.textViewStatusName.visibility = View.GONE
                binding.spinnerStatusName.visibility = View.VISIBLE
                loadSpinner()
            } else if (userRole == "Admin") {
                binding.editButton.visibility = View.VISIBLE

                //коментарий выполняющеего
                binding.textViewCustomersComment.visibility = View.GONE
                binding.editTextCustomersComment.visibility = View.VISIBLE
                binding.editTextCustomersComment.setText(oldOrder.customersComment.orEmpty())
                //коментарий заказчика
                binding.textViewUserComment.visibility = View.GONE
                binding.editTextUserComment.visibility = View.VISIBLE
                binding.editTextUserComment.setText(oldOrder.userComment.orEmpty())
                //spinner
                binding.textViewStatusName.visibility = View.GONE
                binding.spinnerStatusName.visibility = View.VISIBLE

                loadSpinner()
            }
            binding.ButtonSave.visibility = View.VISIBLE
        }

        binding.ButtonSave.setOnClickListener { saveChanges() }
        binding.backBtn.setOnClickListener { finish() }
        binding.buttonDelete.setOnClickListener { deleteOrder() }
    }

    private fun loadSpinner() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllStatusOrder()
                if (response.isSuccessful && response.body() != null) {
                    statusOrders = response.body()!!.statusOrders
                    setupSpinner()
                } else {
                    Toast.makeText(
                        this@order_detail_Activity,
                        "❌ Ошибка загрузки статусов",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@order_detail_Activity,
                    "❌ ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupSpinner() {
        if (statusOrders.isEmpty()) return

        val statusNames = statusOrders.map { it.statusName ?: "без названия" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatusName.adapter = adapter

        // ✅ ВАРИАНТ 2: Ищем статус по имени (так как нет statusOrderId в OrderDto)
        oldOrder.let { order ->
            val currentStatusIndex = statusOrders.indexOfFirst {
                it.statusName == order.statusName
            }
            if (currentStatusIndex >= 0) {
                binding.spinnerStatusName.setSelection(currentStatusIndex)
                selectedStatusId = statusOrders[currentStatusIndex].id  // ✅ Сохраняем ID!
            }
        }

        //обработчик выбора
        binding.spinnerStatusName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStatusId = statusOrders[position].id  // ✅ Сохраняем ID при выборе
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedStatusId = null
            }
        }
    }

    private fun loadOrderDetails(currentOrderId: String) {
        lifecycleScope.launch {
            val result = authRepository.getOrderById(currentOrderId)
            result.onSuccess { order ->
                oldOrder = order
                displayOrder(order)
            }
            result.onFailure {
                Toast.makeText(this@order_detail_Activity, "❌ Ошибка получения данных", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayOrder(order: OrderDto) {
        binding.textViewDate.text = "заказ от:" + formatDate(order.date)
        binding.textViewStatusName.text = "статус: " + order.statusName.orEmpty()
        binding.textViewUserComment.text = "Комментарий заказчика:\n${order.userComment.orEmpty()}"
        binding.textViewCustomersComment.text = "Комментарий исполнителя:\n${order.customersComment.orEmpty()}"
        binding.textViewProductName.text = "продукт: " + order.productDto.productName
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

    private fun saveChanges() {
        // ✅ Получаем значения из EditText (если они видимы)
        val workerComment = if (binding.editTextCustomersComment.visibility == View.VISIBLE) {
            binding.editTextCustomersComment.text.toString().trim()
        } else {
            oldOrder.customersComment.orEmpty()
        }

        val customerComment = if (binding.editTextUserComment.visibility == View.VISIBLE) {
            binding.editTextUserComment.text.toString().trim()
        } else {
            oldOrder.userComment.orEmpty()
        }

        // ✅ ИСПРАВЛЕНО: Гарантируем non-null String для statusOrderId
        val statusOrderId = selectedStatusId
            ?: statusOrders.find { it.statusName == oldOrder.statusName }?.id
            ?: oldOrder.statusName
            ?: ""  // ✅ Финальный fallback: пустая строка вместо null

        val editOrder = UpdateOrderDto(
            id = oldOrder.id,
            productId = oldOrder.productDto.id,
            statusOrderId = statusOrderId,  // ✅ Теперь точно String, не String?
            customersComment = workerComment,
            userComment = customerComment
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.updateOrder(editOrder)
                if (response.isSuccessful) {
                    Toast.makeText(this@order_detail_Activity, "✅ Изменения сохранены", Toast.LENGTH_SHORT).show()
                    // ✅ Перезагружаем данные и выходим из режима редактирования
                    loadOrderDetails(currentOrderId)
                    exitEditMode()
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

    // ✅ Возвращаем интерфейс в режим просмотра
    private fun exitEditMode() {
        isEditMode = false
        binding.editButton.visibility = View.VISIBLE
        binding.ButtonSave.visibility = View.GONE

        // Скрываем поля ввода, показываем TextView
        binding.editTextCustomersComment.visibility = View.GONE
        binding.textViewCustomersComment.visibility = View.VISIBLE
        binding.editTextUserComment.visibility = View.GONE
        binding.textViewUserComment.visibility = View.VISIBLE
        binding.spinnerStatusName.visibility = View.GONE
        binding.textViewStatusName.visibility = View.VISIBLE
    }

    private fun deleteOrder() {
        AlertDialog.Builder(this)
            .setTitle("Удаление заказа")
            .setMessage("Вы уверены, что хотите удалить этот заказ? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ -> performDeleteOrder(currentOrderId) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performDeleteOrder(currentOrderId: String) {
        lifecycleScope.launch {
            try {
                println("🗑️ Удаление заказа: $currentOrderId")

                val response = RetrofitClient.apiService.deleteOrderById(currentOrderId)

                println("📥 Ответ сервера: ${response.code()}")

                if (response.isSuccessful) {
                    println("✅ заказ удалён")
                    Toast.makeText(this@order_detail_Activity, "✅ Заказ удалён", Toast.LENGTH_SHORT).show()
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