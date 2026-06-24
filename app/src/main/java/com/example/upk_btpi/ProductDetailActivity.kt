package com.example.upk_btpi

import ProductDto
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.upk_btpi.Models.StatusProduct.StatusProductDto
import com.example.upk_btpi.Models.Ypk.YpksDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.Utils.ErrorHandler
import com.example.upk_btpi.databinding.ActivityProductDetailBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding
    private val authRepository = AuthRepository()
    private var currentProductId: String? = null
    private var oldProduct: ProductDto? = null
    private var ypksList: List<YpksDto> = emptyList()
    private var statusProductsList: List<StatusProductDto> = emptyList()
    private var selectedYpkId: String? = null
    private var selectedStatusProductId: String? = null
    private var selectedImageUri: Uri? = null

    // ✅ Флаги загрузки справочников
    private var ypksLoaded = false
    private var statusProductsLoaded = false

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            Glide.with(this@ProductDetailActivity)
                .load(it)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(binding.imageViewPhoto)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val productPrefs = getSharedPreferences("product_prefs", MODE_PRIVATE)
        currentProductId = productPrefs.getString("selected_product_id", null)

        if (currentProductId.isNullOrEmpty()) {
            ErrorHandler.showDialog(
                context = this,
                title = "Ошибка",
                message = "ID продукта не найден",
            )
            finish()
            return
        }

        // 🔹 Сначала загружаем справочники
        loadYpks()
        loadStatusProducts()

        // 🔹 Затем загружаем продукт (UI обновится после загрузки справочников)
        if (currentProductId == "add_new_product") {
            setupUIForNewProduct()
        } else {
            loadProductInfo(currentProductId!!)
        }

        // 🔹 Настраиваем видимость по роли
        val authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val userRole = authPrefs.getString("user_role", "DefaultUser") ?: "DefaultUser"
        setupVisibility(userRole)
        setupClickListeners()
    }

    /**
     * Настраивает видимость кнопок в зависимости от роли пользователя
     */
    private fun setupVisibility(userRole: String) {
        // 🔹 Нормализуем роль для надёжного сравнения
        val normalizedRole = userRole.trim().lowercase()

        // 🔹 Для обычных пользователей скрываем кнопки редактирования и удаления
        if (normalizedRole == "user" || normalizedRole == "defaultuser") {
            binding.buttonEdit.visibility = View.GONE
            binding.buttonSave.visibility = View.GONE
            binding.buttonDelete.visibility = View.GONE
        }
        // 🔹 Для Admin и Executor оставляем все кнопки видимыми
    }

    private fun setupClickListeners() {
        binding.buttonEdit.setOnClickListener { enableEditMode() }
        binding.buttonSave.setOnClickListener {
            if (currentProductId == "add_new_product") { addNewProduct() }
            else { updateProduct() }
        }
        binding.buttonOrder.setOnClickListener { placeOrder() }
        binding.buttonBack.setOnClickListener { finish() }
        binding.imageViewPhoto.setOnClickListener {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 100)
                } else {
                    imagePickerLauncher.launch("image/*")
                }
            } else {
                imagePickerLauncher.launch("image/*")
            }
        }
        binding.buttonDelete.setOnClickListener { delete() }
    }

    private fun loadYpks() {
        lifecycleScope.launch {
            try {
                val result = authRepository.getAllYpk()
                result.onSuccess { response ->
                    ypksList = response.ypks
                    ypksLoaded = true  // ✅ Помечаем как загружено
                    if (ypksList.isNotEmpty() && !isFinishing && !isDestroyed) {
                        setupYpkSpinner()
                    }
                    updateProductDisplay()  // ✅ Обновляем UI если продукт уже загружен
                }
                result.onFailure { error ->
                    println("❌ Ошибка загрузки УПК: ${error.message}")
                    ypksList = listOf(
                        YpksDto(id = "default-ypk", ypkName = "УПК по умолчанию", description = "", isActive = true)
                    )
                    ypksLoaded = true
                    setupYpkSpinner()
                    updateProductDisplay()
                }
            } catch (e: Exception) {
                ErrorHandler.showErrorWithMessage(this@ProductDetailActivity, e)
            }
        }
    }

    private fun loadStatusProducts(retryCount: Int = 0) {
        lifecycleScope.launch {
            try {
                val result = authRepository.getAllStatusProduct()
                result.onSuccess { response ->
                    statusProductsList = response.statusProducts
                    statusProductsLoaded = true  // ✅ Помечаем как загружено
                    if (statusProductsList.isNotEmpty() && !isFinishing && !isDestroyed) {
                        setupStatusProductSpinner()
                    }
                    updateProductDisplay()  // ✅ Обновляем UI если продукт уже загружен
                }
                result.onFailure { error ->
                    if (retryCount < 2) {
                        println("🔄 Повторная попытка загрузки статусов (${retryCount + 1}/2)...")
                        kotlinx.coroutines.delay(1000)
                        loadStatusProducts(retryCount + 1)
                    } else {
                        println("❌ Не удалось загрузить статусы после ${retryCount + 1} попыток")
                        statusProductsList = listOf(
                            StatusProductDto(id = "default-editing", statusName = "Черновик"),
                            StatusProductDto(id = "default-publish", statusName = "Опубликовано")
                        )
                        statusProductsLoaded = true
                        setupStatusProductSpinner()
                        updateProductDisplay()
                    }
                }
            } catch (e: Exception) {
                ErrorHandler.showErrorWithMessage(this@ProductDetailActivity, e)
            }
        }
    }

    private fun setupYpkSpinner() {
        val ypkNames = ypksList.map { it.ypkName?.ifBlank { null } ?: "Без названия" }.toMutableList()
        ypkNames.add(0, "Выберите УПК")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ypkNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYpk.adapter = adapter
        binding.spinnerYpk.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && position - 1 < ypksList.size) {
                    selectedYpkId = ypksList[position - 1].id
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupStatusProductSpinner() {
        if (statusProductsList.isEmpty() || !::binding.isInitialized || isFinishing || isDestroyed) {
            return
        }
        val statusNames = statusProductsList.map { it.statusName?.ifBlank { null } ?: "Без названия" }.toMutableList()
        statusNames.add(0, "Выберите статус")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = adapter

        if (oldProduct?.statusProductId != null && currentProductId != "add_new_product") {
            val statusPosition = statusProductsList.indexOfFirst {
                it.id.trim().lowercase() == oldProduct!!.statusProductId!!.trim().lowercase()
            }
            if (statusPosition >= 0) {
                binding.spinnerStatus.setSelection(statusPosition + 1)
                selectedStatusProductId = oldProduct!!.statusProductId
            }
        }

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && position - 1 < statusProductsList.size) {
                    selectedStatusProductId = statusProductsList[position - 1].id
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedStatusProductId = null
            }
        }
    }

    private fun loadProductInfo(productId: String) {
        lifecycleScope.launch {
            val result = authRepository.getProductById(productId)
            result.onSuccess { product ->
                oldProduct = product
                // ✅ Не вызываем displayProductInfo() сразу, ждём загрузки справочников
                updateProductDisplay()  // ✅ Вызываем обновлённый метод
            }
            result.onFailure { error ->
                ErrorHandler.showDialog(
                    context = this@ProductDetailActivity,
                    title = "Ошибка загрузки",
                    message = error.message ?: "Не удалось загрузить продукт",
                )
            }
        }
    }

    /**
     * Обновляет UI с данными продукта (вызывается после загрузки всех справочников)
     */
    private fun updateProductDisplay() {
        // 🔹 Ждём загрузки справочников и наличия продукта
        if (!ypksLoaded || !statusProductsLoaded || oldProduct == null) {
            println("⏳ updateProductDisplay: ожидание данных")
            return
        }

        // 🔹 РЕЖИМ ПРОСМОТРА
        binding.textViewProductName.visibility = View.VISIBLE
        binding.editTextProductName.visibility = View.GONE
        binding.textViewCost.visibility = View.VISIBLE
        binding.editTextCost.visibility = View.GONE
        binding.textViewProductInfo.visibility = View.VISIBLE
        binding.editTextInfo.visibility = View.GONE
        binding.textViewProductOrService.visibility = View.VISIBLE
        binding.checkBoxIsProduct.visibility = View.GONE
        binding.textViewAdress.visibility = View.VISIBLE
        binding.editTextAdress.visibility = View.GONE
        binding.spinnerYpk.visibility = View.GONE
        binding.spinnerStatus.visibility = View.GONE
        binding.textViewProductYpk.visibility = View.VISIBLE
        binding.textViewProductStatus.visibility = View.VISIBLE
        binding.buttonEdit.visibility = View.VISIBLE
        binding.buttonSave.visibility = View.GONE

        binding.textViewProductName.text = oldProduct!!.productName
        binding.textViewCost.text = "${oldProduct!!.productCost} ₽"
        binding.textViewProductInfo.text = oldProduct!!.productInfo ?: "Нет описания"
        binding.textViewProductOrService.text = if (oldProduct!!.isProduct) "Товар" else "Услуга"
        binding.textViewAdress.text = oldProduct!!.address ?: "Нет адреса"

        // ✅ Поиск УПК с безопасным сравнением UUID
        val foundYpk = ypksList.find {
            it.id.trim().lowercase() == oldProduct!!.ypkId.trim().lowercase()
        }

        // ✅ Поиск статуса с безопасным сравнением UUID
        val foundStatus = statusProductsList.find {
            it.id.trim().lowercase() == oldProduct!!.statusProductId?.trim()?.lowercase()
        }

        // ✅ Отображение с fallback
        binding.textViewProductYpk.text = foundYpk?.ypkName?.ifBlank { null } ?: "УПК не найден"
        binding.textViewProductStatus.text = foundStatus?.statusName?.ifBlank { null } ?: "Статус не найден"

        // ✅ Загрузка фото
        val photoUrl = oldProduct!!.photoUrl
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this).load(photoUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(binding.imageViewPhoto)
        } else {
            binding.imageViewPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun setupUIForNewProduct() {
        binding.textViewProductName.visibility = View.GONE
        binding.textViewCost.visibility = View.GONE
        binding.textViewProductInfo.visibility = View.GONE
        binding.textViewProductOrService.visibility = View.GONE
        binding.textViewAdress.visibility = View.GONE
        binding.textViewProductYpk.visibility = View.GONE
        binding.textViewProductStatus.visibility = View.GONE

        binding.editTextProductName.visibility = View.VISIBLE
        binding.editTextCost.visibility = View.VISIBLE
        binding.editTextInfo.visibility = View.VISIBLE
        binding.checkBoxIsProduct.visibility = View.VISIBLE
        binding.editTextAdress.visibility = View.VISIBLE
        binding.spinnerYpk.visibility = View.VISIBLE
        binding.spinnerStatus.visibility = View.VISIBLE

        binding.buttonEdit.visibility = View.GONE
        binding.buttonSave.visibility = View.VISIBLE
        binding.buttonSave.text = "Добавить"
        binding.buttonOrder.visibility = View.GONE
        binding.buttonDelete.visibility = View.GONE
    }

    private fun enableEditMode() {
        // ✅ Проверка роли перед включением режима редактирования
        val authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val userRole = authPrefs.getString("user_role", "DefaultUser") ?: "DefaultUser"
        val normalizedRole = userRole.trim().lowercase()

        if (normalizedRole == "user" || normalizedRole == "defaultuser") {
            ErrorHandler.showDialog(
                context = this,
                title = "Доступ запрещён",
                message = "Только администратор может редактировать продукты",
            )
            return
        }

        if (oldProduct == null) return

        binding.textViewProductName.visibility = View.GONE
        binding.textViewCost.visibility = View.GONE
        binding.textViewProductInfo.visibility = View.GONE
        binding.textViewProductOrService.visibility = View.GONE
        binding.textViewAdress.visibility = View.GONE
        binding.textViewProductYpk.visibility = View.GONE
        binding.textViewProductStatus.visibility = View.GONE

        binding.editTextProductName.visibility = View.VISIBLE
        binding.editTextCost.visibility = View.VISIBLE
        binding.editTextInfo.visibility = View.VISIBLE
        binding.checkBoxIsProduct.visibility = View.VISIBLE
        binding.editTextAdress.visibility = View.VISIBLE
        binding.spinnerYpk.visibility = View.VISIBLE
        binding.spinnerStatus.visibility = View.VISIBLE

        binding.editTextProductName.setText(oldProduct!!.productName)
        binding.editTextCost.setText(oldProduct!!.productCost.toString())
        binding.editTextInfo.setText(oldProduct!!.productInfo ?: "")
        binding.checkBoxIsProduct.isChecked = oldProduct!!.isProduct
        binding.editTextAdress.setText(oldProduct!!.address ?: "")

        val ypkPosition = ypksList.indexOfFirst {
            it.id.trim().lowercase() == oldProduct!!.ypkId.trim().lowercase()
        }
        if (ypkPosition >= 0) {
            binding.spinnerYpk.setSelection(ypkPosition + 1)
            selectedYpkId = oldProduct!!.ypkId
        }
        if (!oldProduct!!.statusProductId.isNullOrEmpty()) {
            val statusPosition = statusProductsList.indexOfFirst {
                it.id.trim().lowercase() == oldProduct!!.statusProductId!!.trim().lowercase()
            }
            if (statusPosition >= 0) {
                binding.spinnerStatus.setSelection(statusPosition + 1)
                selectedStatusProductId = oldProduct!!.statusProductId
            }
        }

        binding.buttonEdit.visibility = View.GONE
        binding.buttonOrder.visibility = View.GONE
        binding.buttonSave.visibility = View.VISIBLE
        binding.buttonSave.text = "Сохранить"
    }

    private fun placeOrder() {
        val prefs = getSharedPreferences("product_prefs", 0)
        prefs.edit().apply { putString("selected_product_id", currentProductId); apply() }
        val intent = Intent(this@ProductDetailActivity, NewOrderActivity::class.java)
        startActivity(intent)
    }

    private fun addNewProduct() {
        val productName = binding.editTextProductName.text.toString().trim()
        val productCost = binding.editTextCost.text.toString().replace(" ₽", "").replace(",", ".").trim().toDoubleOrNull()
        val productInfo = binding.editTextInfo.text.toString().trim()
        val isProduct = binding.checkBoxIsProduct.isChecked
        val address = binding.editTextAdress.text.toString().trim()

        // ✅ Валидация — оставляем inline-ошибки (не диалоги)
        if (productName.isEmpty()) {
            binding.editTextProductName.error = "Введите название"
            binding.editTextProductName.requestFocus()
            return
        }
        if (productCost == null || productCost <= 0) {
            binding.editTextCost.error = "Введите корректную цену"
            binding.editTextCost.requestFocus()
            return
        }
        if (selectedYpkId.isNullOrEmpty()) {
            ErrorHandler.showDialog(this, "Ошибка", "Выберите УПК")
            return
        }
        if (selectedStatusProductId.isNullOrEmpty()) {
            ErrorHandler.showDialog(this, "Ошибка", "Выберите статус")
            return
        }

        lifecycleScope.launch {
            try {
                binding.buttonSave.isEnabled = false
                binding.buttonSave.text = "Добавление..."

                val nameBody = productName.toRequestBody("text/plain".toMediaType())
                val infoBody = productInfo.toRequestBody("text/plain".toMediaType())
                val costBody = productCost.toString().toRequestBody("text/plain".toMediaType())
                val isProductBody = isProduct.toString().toRequestBody("text/plain".toMediaType())
                val addressBody = address.toRequestBody("text/plain".toMediaType())
                val ypkIdBody = selectedYpkId!!.toRequestBody("text/plain".toMediaType())
                val statusProductIdBody = selectedStatusProductId!!.toRequestBody("text/plain".toMediaType())

                val photoPart = selectedImageUri?.let { uri ->
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val tempFile = File.createTempFile("product_image", ".jpg", cacheDir)
                        tempFile.outputStream().use { output -> inputStream?.copyTo(output) }
                        val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
                        MultipartBody.Part.createFormData("Photo", tempFile.name, requestBody)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                val response = RetrofitClient.apiService.createProduct(
                    productName = nameBody,
                    productInfo = infoBody,
                    productCost = costBody,
                    isProduct = isProductBody,
                    address = addressBody,
                    ypkId = ypkIdBody,
                    statusProductId = statusProductIdBody,
                    photo = photoPart
                )

                if (response.isSuccessful) {
                    ErrorHandler.showDialog(
                        context = this@ProductDetailActivity,
                        title = "Успех",
                        message = "✅ Продукт создан",
                    )
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    ErrorHandler.showDialog(
                        context = this@ProductDetailActivity,
                        title = "Ошибка создания",
                        message = ErrorHandler.handleApiError(response),
                    )
                }
            } catch (e: Exception) {
                ErrorHandler.showErrorWithMessage(this@ProductDetailActivity, e)
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "Добавить"
            }
        }
    }

    private fun updateProduct() {
        if (oldProduct == null) return

        val productName = binding.editTextProductName.text.toString().trim()
        val productCost = binding.editTextCost.text.toString()
            .replace(" ₽", "")
            .replace(",", ".")
            .trim()
            .toDoubleOrNull()
        val productInfo = binding.editTextInfo.text.toString().trim()
        val isProduct = binding.checkBoxIsProduct.isChecked
        val address = binding.editTextAdress.text.toString().trim()

        // ✅ Валидация — inline-ошибки
        if (productName.isEmpty()) {
            binding.editTextProductName.error = "Введите название"
            binding.editTextProductName.requestFocus()
            return
        }
        if (productCost == null || productCost <= 0) {
            binding.editTextCost.error = "Введите корректную цену"
            binding.editTextCost.requestFocus()
            return
        }
        if (selectedYpkId.isNullOrEmpty()) {
            ErrorHandler.showDialog(this, "Ошибка", "Выберите УПК")
            return
        }
        if (selectedStatusProductId.isNullOrEmpty()) {
            ErrorHandler.showDialog(this, "Ошибка", "Выберите статус")
            return
        }

        lifecycleScope.launch {
            try {
                binding.buttonSave.isEnabled = false
                binding.buttonSave.text = "Сохранение..."

                val idBody = oldProduct!!.id.toRequestBody("text/plain".toMediaType())
                val nameBody = productName.toRequestBody("text/plain".toMediaType())
                val infoBody = productInfo.toRequestBody("text/plain".toMediaType())
                val costBody = productCost.toString().toRequestBody("text/plain".toMediaType())
                val isProductBody = isProduct.toString().toRequestBody("text/plain".toMediaType())
                val addressBody = address.toRequestBody("text/plain".toMediaType())
                val ypkIdBody = selectedYpkId!!.toRequestBody("text/plain".toMediaType())
                val statusProductIdBody = selectedStatusProductId!!.toRequestBody("text/plain".toMediaType())

                val photoPart = selectedImageUri?.let { uri ->
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val tempFile = File.createTempFile("product_image", ".jpg", cacheDir)
                        tempFile.outputStream().use { output -> inputStream?.copyTo(output) }
                        val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
                        MultipartBody.Part.createFormData("Photo", tempFile.name, requestBody)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                val response = RetrofitClient.apiService.updateProduct(
                    id = idBody,
                    productName = nameBody,
                    productInfo = infoBody,
                    productCost = costBody,
                    isProduct = isProductBody,
                    address = addressBody,  // ✅ Исправлено: было adres
                    ypkId = ypkIdBody,
                    statusProductId = statusProductIdBody,
                    photo = photoPart
                )

                if (response.isSuccessful) {
                    ErrorHandler.showDialog(
                        context = this@ProductDetailActivity,
                        title = "Успех",
                        message = "✅ Изменения сохранены",
                    )
                    // Возвращаем режим просмотра
                    binding.textViewProductName.visibility = View.VISIBLE
                    binding.editTextProductName.visibility = View.GONE
                    binding.textViewCost.visibility = View.VISIBLE
                    binding.editTextCost.visibility = View.GONE
                    binding.textViewProductInfo.visibility = View.VISIBLE
                    binding.editTextInfo.visibility = View.GONE
                    binding.textViewProductOrService.visibility = View.VISIBLE
                    binding.checkBoxIsProduct.visibility = View.GONE
                    binding.textViewAdress.visibility = View.VISIBLE
                    binding.editTextAdress.visibility = View.GONE
                    binding.spinnerYpk.visibility = View.GONE
                    binding.spinnerStatus.visibility = View.GONE
                    binding.textViewProductYpk.visibility = View.VISIBLE
                    binding.textViewProductStatus.visibility = View.VISIBLE
                    binding.buttonEdit.visibility = View.VISIBLE
                    binding.buttonSave.visibility = View.GONE
                    binding.buttonOrder.visibility = View.VISIBLE
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    ErrorHandler.showDialog(
                        context = this@ProductDetailActivity,
                        title = "Ошибка сохранения",
                        message = ErrorHandler.handleApiError(response),
                    )
                }
            } catch (e: Exception) {
                ErrorHandler.showErrorWithMessage(this@ProductDetailActivity, e)
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "Сохранить"
            }
        }
    }

    private fun delete() {
        // ✅ Проверка роли перед удалением
        val authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val userRole = authPrefs.getString("user_role", "DefaultUser") ?: "DefaultUser"
        val normalizedRole = userRole.trim().lowercase()

        if (normalizedRole == "user" || normalizedRole == "defaultuser") {
            ErrorHandler.showDialog(
                context = this,
                title = "Доступ запрещён",
                message = "Только администратор может удалять продукты",
            )
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Удаление продукта")
            .setMessage("Вы уверены, что хотите удалить этот продукт? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ -> performDeleteProduct(currentProductId!!) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performDeleteProduct(selectedProductId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteProductById(selectedProductId)
                if (response.isSuccessful) {
                    ErrorHandler.showDialog(
                        context = this@ProductDetailActivity,
                        title = "Удалено",
                        message = "✅ Продукт удалён",
                    )
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    ErrorHandler.showDialog(
                        context = this@ProductDetailActivity,
                        title = "Ошибка удаления",
                        message = ErrorHandler.handleApiError(response),
                    )
                }
            } catch (e: Exception) {
                ErrorHandler.showErrorWithMessage(this@ProductDetailActivity, e)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                imagePickerLauncher.launch("image/*")
            } else {
                ErrorHandler.showDialog(
                    this,
                    "Разрешение",
                    "Требуется доступ к галерее для выбора фото",
                )
            }
        }
    }
}