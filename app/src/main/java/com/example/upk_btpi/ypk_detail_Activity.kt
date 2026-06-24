package com.example.upk_btpi

import ProductDto
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upk_btpi.Adapters.ProductAdapter
import com.example.upk_btpi.Models.Ypk.CreateYpkDto
import com.example.upk_btpi.Models.Ypk.UpdateYpkDto
import com.example.upk_btpi.Models.Ypk.YpksDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.Utils.ErrorHandler  // ✅ Импорт обработчика ошибок
import com.example.upk_btpi.databinding.ActivityYpkDetailBinding
import kotlinx.coroutines.launch

class ypk_detail_Activity : AppCompatActivity() {

    // ✅ ViewBinding для безопасной работы с UI
    private lateinit var binding: ActivityYpkDetailBinding

    // ✅ Данные УПК и репозиторий
    private lateinit var oldUpk: YpksDto
    private var selectedUpkId: String? = null
    private val authRepository = AuthRepository()

    // ✅ Флаги и адаптеры
    private var EditMode: Boolean = false
    private var productAdapter: ProductAdapter? = null
    private var products: List<ProductDto> = emptyList()
    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Инициализация ViewBinding
        binding = ActivityYpkDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Получаем ID УПК из SharedPreferences
        val userPrefs = getSharedPreferences("ypk_prefs", MODE_PRIVATE)
        selectedUpkId = userPrefs.getString("selected_ypk_id", null)

        // 🔹 Получаем роль пользователя для настройки видимости кнопок
        val authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        userRole = authPrefs.getString("user_role", null)

        // ✅ Проверка: найден ли ID УПК
        if (selectedUpkId.isNullOrEmpty()) {
            ErrorHandler.showDialog(
                context = this,
                title = "Ошибка",
                message = "ID УПК не найден. Попробуйте выбрать УПК заново.",
            )
            finish()
            return
        }

        // 🔹 Настраиваем UI в зависимости от режима (новый УПК или существующий)
        display()

        // 🔹 Обработчики кнопок
        binding.buttonBack.setOnClickListener { back() }
        binding.buttonDelete.setOnClickListener { delete() }
    }

    /**
     * Показывает диалог подтверждения удаления УПК
     */
    private fun delete() {
        AlertDialog.Builder(this)
            .setTitle("Удаление УПК")
            .setMessage("Вы уверены, что хотите удалить этот УПК? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ -> performDeleteUpk(selectedUpkId!!) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Выполняет удаление УПК через API
     */
    private fun performDeleteUpk(selectedUpkId: String) {
        lifecycleScope.launch {
            try {
                println("🗑️ Удаление УПК: $selectedUpkId")

                val response = RetrofitClient.apiService.deleteYpkById(selectedUpkId)

                println("📥 Ответ сервера: ${response.code()}")

                if (response.isSuccessful) {
                    println("✅ УПК удалён")
                    ErrorHandler.showDialog(
                        context = this@ypk_detail_Activity,
                        title = "Удалено",
                        message = "✅ УПК успешно удалён",
                    )
                    finish()
                } else {
                    // Ошибка API: показываем понятное сообщение
                    ErrorHandler.showDialog(
                        context = this@ypk_detail_Activity,
                        title = "Ошибка удаления",
                        message = ErrorHandler.handleApiError(response),
                    )
                }
            } catch (e: Exception) {
                // Сетевая или системная ошибка
                ErrorHandler.showErrorWithMessage(this@ypk_detail_Activity, e)
            }
        }
    }

    /**
     * Загружает данные УПК по ID из API и отображает их в UI
     */
    private fun getInfoAboutUpk(selectedUpkId: String) {
        lifecycleScope.launch {
            val result = authRepository.getYpkById(selectedUpkId)

            result.onSuccess { upk ->
                oldUpk = upk
                displayUpkInfo(upk)  // Успех: отображаем данные
            }

            result.onFailure { error ->
                // Ошибка API: показываем понятное сообщение
                ErrorHandler.showDialog(
                    context = this@ypk_detail_Activity,
                    title = "Ошибка загрузки",
                    message = error.message ?: "Не удалось загрузить данные УПК",
                )
            }
        }
    }

    /**
     * Отображает данные УПК в TextView или EditText в зависимости от режима
     */
    private fun displayUpkInfo(upk: YpksDto) {
        if (EditMode) {
            // 🔹 РЕЖИМ РЕДАКТИРОВАНИЯ: заполняем EditText
            binding.editTextTextName.setText(upk.ypkName ?: "")
            binding.editTextTextContent.setText(upk.description)
        } else {
            // 🔹 РЕЖИМ ПРОСМОТРА: показываем TextView
            binding.textViewName.text = upk.ypkName ?: "без имени"
            binding.textViewContent.text = upk.description ?: "Нет описания"
        }
        // ✅ Устанавливаем адаптер для списка продуктов
        binding.recyclerViewProductByUpk.adapter = productAdapter
    }

    /**
     * Возврат: отменяет редактирование или закрывает активность
     */
    private fun back() {
        if (EditMode) {
            EditMode = false
            displayUpkInfo(oldUpk)
            display()
        } else {
            finish()
        }
    }

    /**
     * Настраивает видимость элементов UI в зависимости от режима (новый/существующий УПК)
     */
    private fun display() {
        if (selectedUpkId == "new_ypk") {
            // 🔹 РЕЖИМ СОЗДАНИЯ НОВОГО УПК
            binding.recyclerViewProductByUpk.visibility = View.GONE
            binding.textViewName.visibility = View.GONE
            binding.textViewContent.visibility = View.GONE
            binding.buttonEdit.visibility = View.GONE
            binding.buttonDelete.visibility = View.GONE

            binding.editTextTextName.visibility = View.VISIBLE
            binding.editTextTextContent.visibility = View.VISIBLE
            binding.buttonSave.visibility = View.VISIBLE

            binding.buttonSave.setOnClickListener { addnewYpk() }
        } else {
            // 🔹 РЕЖИМ ПРОСМОТРА/РЕДАКТИРОВАНИЯ СУЩЕСТВУЮЩЕГО УПК
            binding.recyclerViewProductByUpk.apply {
                layoutManager = LinearLayoutManager(this@ypk_detail_Activity)
                setHasFixedSize(true)
            }
            getInfoAboutUpk(selectedUpkId!!)
            loadProducts(selectedUpkId.toString())

            binding.buttonEdit.visibility = View.GONE
            binding.buttonSave.visibility = View.GONE
            binding.editTextTextName.visibility = View.GONE
            binding.editTextTextContent.visibility = View.GONE
            binding.textViewName.visibility = View.VISIBLE
            binding.textViewContent.visibility = View.VISIBLE

            binding.buttonEdit.setOnClickListener { edit() }
            binding.buttonSave.setOnClickListener { save() }
        }

        // ✅ Показываем кнопки удаления/редактирования только для Admin
        if (userRole == "Admin" && selectedUpkId != "new_ypk") {
            binding.buttonDelete.visibility = View.VISIBLE
            binding.buttonEdit.visibility = View.VISIBLE
        }

        // ✅ Меняем текст кнопки "Назад" в режиме редактирования
        binding.buttonBack.text = if (EditMode) "отменить" else "назад"
    }

    /**
     * Включает режим редактирования: показывает поля ввода
     */
    private fun edit() {
        EditMode = true

        // 🔹 Показываем поля ввода
        binding.editTextTextName.visibility = View.VISIBLE
        binding.editTextTextContent.visibility = View.VISIBLE

        // 🔹 Скрываем TextView
        binding.textViewName.visibility = View.GONE
        binding.textViewContent.visibility = View.GONE

        // 🔹 Заполняем поля текущими данными
        displayUpkInfo(oldUpk)

        // 🔹 Переключаем кнопки
        binding.buttonEdit.visibility = View.GONE
        binding.buttonDelete.visibility = View.GONE
        binding.buttonSave.visibility = View.VISIBLE
        binding.buttonBack.text = "отменить"
    }

    /**
     * Сохраняет изменения УПК через API
     */
    private fun save() {
        lifecycleScope.launch {
            try {
                val name = binding.editTextTextName.text.toString().trim()
                val content = binding.editTextTextContent.text.toString().trim()

                // ✅ Inline-валидация: показываем ошибку прямо в поле
                if (name.isEmpty()) {
                    binding.editTextTextName.error = "Введите название УПК"
                    binding.editTextTextName.requestFocus()
                    return@launch
                }

                // ✅ Создаём DTO с обновлёнными данными
                val request = UpdateYpkDto(
                    id = oldUpk.id,
                    ypkName = name ?: oldUpk.ypkName,
                    description = content ?: oldUpk.description
                )

                // ✅ Вызываем API для обновления УПК
                val response = RetrofitClient.apiService.updateYpk(request)

                if (response.isSuccessful) {
                    ErrorHandler.showDialog(
                        context = this@ypk_detail_Activity,
                        title = "Успех",
                        message = "✅ Изменения сохранены",
                    )
                    finish()
                } else {
                    // Ошибка API: показываем понятное сообщение
                    ErrorHandler.showDialog(
                        context = this@ypk_detail_Activity,
                        title = "Ошибка сохранения",
                        message = ErrorHandler.handleApiError(response),
                    )
                }
            } catch (e: Exception) {
                // Сетевая или системная ошибка
                ErrorHandler.showErrorWithMessage(this@ypk_detail_Activity, e)
            }
        }
    }

    /**
     * Загружает список продуктов, связанных с данным УПК
     */
    private fun loadProducts(ypkId: String) {
        lifecycleScope.launch {
            val result = authRepository.getProductsByYpk(ypkId)

            result.onSuccess { response ->
                products = response.products
                displayByUpk()  // Успех: отображаем список
            }

            result.onFailure { error ->
                ErrorHandler.showDialog(
                    context = this@ypk_detail_Activity,
                    title = "Ошибка загрузки",
                    message = error.message ?: "Не удалось загрузить продукты",
                )
            }
        }
    }

    /**
     * Отображает список продуктов в RecyclerView
     */
    private fun displayByUpk() {
        if (productAdapter == null) {
            productAdapter = ProductAdapter(products) { product -> onProductClick(product) }
            binding.recyclerViewProductByUpk.adapter = productAdapter
        } else {
            productAdapter?.updateProducts(products)
        }
    }

    /**
     * Обработчик клика по продукту: переход на экран деталей продукта
     */
    private fun onProductClick(product: ProductDto) {
        val prefs = getSharedPreferences("product_prefs", 0)
        prefs.edit().apply {
            putString("selected_product_id", product.id)
            apply()
        }
        val intent = Intent(this@ypk_detail_Activity, ProductDetailActivity::class.java)
        startActivity(intent)
    }

    /**
     * Создаёт новый УПК через API
     */
    fun addnewYpk() {
        val name = binding.editTextTextName.text.toString().trim()
        val content = binding.editTextTextContent.text.toString().trim()

        // ✅ Inline-валидация
        if (name.isEmpty()) {
            binding.editTextTextName.error = "Введите название УПК"
            binding.editTextTextName.requestFocus()
            return
        }

        val newYpk = CreateYpkDto(ypkName = name, description = content)

        lifecycleScope.launch {
            val result = authRepository.addNewYpk(newYpk)

            result.onSuccess {
                ErrorHandler.showDialog(
                    context = this@ypk_detail_Activity,
                    title = "Успех",
                    message = "✅ УПК успешно создан",
                )
                finish()
            }

            result.onFailure { error ->
                ErrorHandler.showDialog(
                    context = this@ypk_detail_Activity,
                    title = "Ошибка создания",
                    message = error.message ?: "Не удалось создать УПК",
                )
            }
        }
    }
}