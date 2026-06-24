package com.example.upk_btpi

import ProductDto
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.upk_btpi.Models.Order.CreateOrderDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Utils.ErrorHandler  // ✅ Импорт обработчика ошибок
import com.example.upk_btpi.databinding.ActivityNewOrderBinding
import kotlinx.coroutines.launch
import kotlin.onSuccess

class NewOrderActivity : AppCompatActivity() {

    // ✅ ViewBinding для безопасной работы с UI
    private lateinit var binding: ActivityNewOrderBinding

    // ✅ Репозиторий для работы с API
    private val authRepository = AuthRepository()

    // ✅ ID текущего продукта (из SharedPreferences)
    private var productId: String? = null

    // ✅ Роль текущего пользователя
    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Инициализация ViewBinding
        binding = ActivityNewOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Получаем данные из SharedPreferences
        val productPrefs = getSharedPreferences("product_prefs", MODE_PRIVATE)
        val authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        val userId = authPrefs.getString("user_id", null)
        productId = productPrefs.getString("selected_product_id", null)
        userRole = authPrefs.getString("user_role", null)  // ✅ Получаем роль

        // ✅ Проверка: найден ли ID продукта
        if (productId.isNullOrEmpty()) {
            ErrorHandler.showDialog(
                context = this,
                title = "Ошибка",
                message = "ID продукта не найден. Попробуйте выбрать товар заново.",
            )
            finish()
            return
        }

        // ✅ Проверка: авторизован ли пользователь
        if (userId.isNullOrEmpty()) {
            ErrorHandler.showDialog(
                context = this,
                title = "Ошибка авторизации",
                message = "Пользователь не авторизован. Пожалуйста, войдите в аккаунт.",
            )
            finish()
            return
        }

        // 🔹 Загружаем информацию о продукте для отображения
        loadInfo(productId!!)

        // 🔹 Настраиваем видимость полей комментариев по роли
        setupCommentFieldsByRole()

        // 🔹 Обработчики кнопок
        binding.buttonBack.setOnClickListener {
            finish()  // Возврат на предыдущий экран
        }

        binding.buttonReliase.setOnClickListener {
            // ✅ Оформление заказа: передаём ID пользователя и продукта
            doNewOrder(userId!!, productId!!)
        }
    }

    /**
     * Настраивает видимость полей комментариев в зависимости от роли пользователя:
     * - DefaultUser (заказчик): видит только "Ваш комментарий"
     * - Manager/Executor (работник): видит только "Комментарий работника"
     * - Admin: видит оба поля
     */
    private fun setupCommentFieldsByRole() {
        when (userRole) {
            "DefaultUser" -> {
                // 🔹 Заказчик: видит только свой комментарий
                binding.textView1.text = "Ваш комментарий:"  // Меняем подпись
                binding.editTextTextCustomerComment.visibility = View.VISIBLE
                binding.editTextTextCustomerComment.hint = "Введите ваш комментарий"

                // Скрываем поле для работника
                binding.textView2.visibility = View.GONE
                binding.editTextTextUserComment.visibility = View.GONE
            }
            "Manager", "Executor" -> {
                // 🔹 Работник: видит только комментарий для исполнения
                binding.textView1.text = "Комментарий работника:"  // Меняем подпись
                binding.editTextTextCustomerComment.visibility = View.VISIBLE
                binding.editTextTextCustomerComment.hint = "Введите комментарий для исполнения"

                // Скрываем поле для заказчика
                binding.textView2.visibility = View.GONE
                binding.editTextTextUserComment.visibility = View.GONE
            }
            "Admin" -> {
                // 🔹 Админ: видит оба поля
                binding.textView1.text = "Комментарий работника:"
                binding.editTextTextCustomerComment.visibility = View.VISIBLE

                binding.textView2.visibility = View.VISIBLE
                binding.editTextTextUserComment.visibility = View.VISIBLE
            }
            else -> {
                // 🔹 По умолчанию: показываем как для заказчика
                binding.textView1.text = "Ваш комментарий:"
                binding.editTextTextCustomerComment.visibility = View.VISIBLE
                binding.textView2.visibility = View.GONE
                binding.editTextTextUserComment.visibility = View.GONE
            }
        }
    }

    /**
     * Загружает данные о продукте по ID и отображает их в UI
     */
    private fun loadInfo(productId: String) {
        lifecycleScope.launch {
//            try {
//                // ✅ Асинхронный запрос к API
//                val result = authRepository.getProductById(productId)
//
//                result.onSuccess { product ->
//                    // Успех: отображаем данные продукта
//                    displayProduct(product)
//                }
//
//                result.onFailure { error ->
//                    // Ошибка API: показываем понятное сообщение
//                    ErrorHandler.showDialog(
//                        context = this@NewOrderActivity,
//                        title = "Ошибка загрузки",
//                        message = error.message ?: "Не удалось загрузить данные продукта"
//                    )
//                }
//            } catch (e: Exception) {
//                // Сетевая или системная ошибка
//                ErrorHandler.showErrorWithMessage(this@NewOrderActivity, e)
//            }
        }
    }

    /**
     * Отображает данные продукта в TextView
     */
    private fun displayProduct(product: ProductDto) {
        binding.textViewNameProduct.text = product.productName
        binding.textViewCost.text = "${product.productCost} ₽"
        binding.textViewInfo.text = product.productInfo ?: "Нет описания"
    }

    /**
     * Оформляет новый заказ через API с учётом роли пользователя
     * @param userId ID текущего пользователя
     * @param NewproductId ID продукта для заказа
     */
    private fun doNewOrder(userId: String, NewproductId: String) {
        // 🔹 Получаем комментарии в зависимости от видимости полей
        val workerComment = if (binding.editTextTextCustomerComment.visibility == View.VISIBLE) {
            binding.editTextTextCustomerComment.text.toString().trim()
        } else {
            null
        }

        val customerComment = if (binding.editTextTextUserComment.visibility == View.VISIBLE) {
            binding.editTextTextUserComment.text.toString().trim()
        } else {
            null
        }

        // ✅ Валидация: хотя бы один комментарий должен быть заполнен
        if (workerComment.isNullOrEmpty() && customerComment.isNullOrEmpty()) {
            ErrorHandler.showDialog(
                context = this,
                title = "Ошибка",
                message = "Введите комментарий",
            )
            return
        }

        // 🔹 Отправляем запрос на создание заказа
        lifecycleScope.launch {
            try {
                // ✅ Создаем DTO для запроса
                val createOrderDto = CreateOrderDto(
                    productId = NewproductId,
                    statusOrderId = "a3a89ae5-9acb-4fb5-9b88-e6b9ffa5994f",  // ID статуса "Новый"
                    customersComment = workerComment,    // Комментарий для исполнителя
                    userComment = customerComment        // Комментарий от заказчика
                )

                // ✅ Асинхронный вызов API
                val result = authRepository.addNewOrder(createOrderDto)

                result.onSuccess {
                    // Успех: показываем диалог и закрываем активность
                    ErrorHandler.showDialog(
                        context = this@NewOrderActivity,
                        title = "Заказ оформлен",
                        message = "✅ Ваш заказ успешно создан. Ожидайте подтверждения.",
                    )
                    finish()
                }

                result.onFailure { error ->
                    // Ошибка API: показываем понятное сообщение
                    ErrorHandler.showDialog(
                        context = this@NewOrderActivity,
                        title = "Ошибка заказа",
                        message = error.message ?: "Не удалось оформить заказ",
                    )
                }

            } catch (e: Exception) {
                // Сетевая или системная ошибка
                ErrorHandler.showErrorWithMessage(this@NewOrderActivity, e)
            }
        }
    }
}