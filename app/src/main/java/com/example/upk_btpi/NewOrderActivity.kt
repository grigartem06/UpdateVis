package com.example.upk_btpi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.upk_btpi.Models.Order.CreateOrderDto
import com.example.upk_btpi.Models.Product.ProductDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.databinding.ActivityNewOrderBinding
import kotlinx.coroutines.launch

private val authRepository = AuthRepository()
class NewOrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewOrderBinding
    private val authRepository = AuthRepository()
    private var productId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNewOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("product_prefs", 0)
        val authPrefs = getSharedPreferences("auth_prefs", 0)
        val userId = authPrefs.getString("user_id", null)
        productId = prefs.getString("selected_product_id", null)

        if (productId.isNullOrEmpty()) {
            Toast.makeText(this, "⚠️ ID продукта не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "⚠️ Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadInfo(productId!!)

        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonReliase.setOnClickListener { doNewOrder(userId!!, productId!!) }
    }

    private fun loadInfo(productId: String)  {
        lifecycleScope.launch {
            try {
                val result = authRepository.getProductById(productId)
                result.onSuccess { product -> displayProduct(product) }
                result.onFailure {Toast.makeText(this@NewOrderActivity,"ошибка получения данных", Toast.LENGTH_SHORT).show() }
            }
            catch (e: Exception) {
                Toast.makeText(this@NewOrderActivity, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun displayProduct(product: ProductDto) {
        binding.textViewNameProduct.text = product.productName
        binding.textViewCost.text = "${product.productCost} ₽"
        binding.textViewInfo.text = product.productInfo ?: "Нет описания"
    }

    private fun doNewOrder(userId: String, NewproductId: String) {
        val userComment = binding.editTextTextUserComment.text.toString().trim()

        if (userComment.isEmpty()) {
            Toast.makeText(this, "⚠️ Введите комментарий", Toast.LENGTH_SHORT).show()
            binding.editTextTextUserComment.requestFocus()
            return
        }

            lifecycleScope.launch {
                try {
                    val createOrderDto = CreateOrderDto(
                        productId = NewproductId,
                        statusOrderId = "a3a89ae5-9acb-4fb5-9b88-e6b9ffa5994f",
                        customersComment = userComment,
                        userComment = userComment
                    )

                    val result = authRepository.createNewOrder(createOrderDto)
                    result.onSuccess {
                        Toast.makeText(this@NewOrderActivity, "✅ Заказ оформлен", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    result.onFailure { error -> Toast.makeText(this@NewOrderActivity, "❌ ${error.message}", Toast.LENGTH_LONG).show() }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@NewOrderActivity,
                        "❌ ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }


//                val createOrderDto = CreateOrderDto(
//                    productId = NewproductId,
//                    statusOrderId = "a3a89ae5-9acb-4fb5-9b88-e6b9ffa5994f",
//                    customersComment = null,
//                    userComment = binding.editTextTextUserComment.text.toString()
//                )
//                val result = authRepository.createNewOrder(createOrderDto)
//                result.onSuccess { Toast.makeText(this@NewOrderActivity, "заказ офромлен", Toast.LENGTH_SHORT).show() }
//                result.onFailure { Toast.makeText(this@NewOrderActivity, "ошибка, заказ не оформлен", Toast.LENGTH_SHORT).show() }
            }

    }

}