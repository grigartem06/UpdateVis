package com.example.upk_btpi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upk_btpi.Adapters.ProductAdapter
import com.example.upk_btpi.Models.Product.ProductDto
import com.example.upk_btpi.Models.Ypk.CreateYpkDto
import com.example.upk_btpi.Models.Ypk.UpdateYpkDto
import com.example.upk_btpi.Models.Ypk.YpksDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.databinding.ActivityYpkDetailBinding
import kotlinx.coroutines.launch

class ypk_detail_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityYpkDetailBinding
    private lateinit var oldUpk: YpksDto
    private var selectedUpkId: String?=null
    private val authRepository = AuthRepository()
    private var EditMode: Boolean = false
    private var productAdapter: ProductAdapter?=null
    private var products: List<ProductDto> = emptyList()
    private var userRole: String?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityYpkDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //получение данных из SharedPreferences
        val userPrefs = getSharedPreferences("ypk_prefs", MODE_PRIVATE)
        selectedUpkId = userPrefs.getString("selected_ypk_id", null)

        val authPrefs =getSharedPreferences("auth_prefs", MODE_PRIVATE)
        userRole = authPrefs.getString("user_role", null)

        if (selectedUpkId.isNullOrEmpty()) {
            Toast.makeText(this, "⚠️ ID УПК не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        display()


        //действия кнопок
        binding.buttonBack.setOnClickListener { back()}
        binding.buttonDelete.setOnClickListener { delete() }


    }

    private fun delete() {
        AlertDialog.Builder(this)
            .setTitle("Удаление упк")
            .setMessage("Вы уверены, что хотите удалить этот упк? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ -> performDeleteUpk(selectedUpkId!!) }
            .setNegativeButton("Отмена", null).show()
    }

    private fun performDeleteUpk(selectedUpkId: String) {
        lifecycleScope.launch {
            try {
                println("🗑️ Удаление заказа: $selectedUpkId")

                val response = RetrofitClient.apiService.deleteYpkById(selectedUpkId)

                println("📥 Ответ сервера: ${response.code()}")

                if (response.isSuccessful) {
                    println("✅ заказ удалён")
                    Toast.makeText(this@ypk_detail_Activity, "✅ упк удалён", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    println("❌ ОШИБКА: ${response.code()} - $error")
                    Toast.makeText(this@ypk_detail_Activity, "❌ Ошибка: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@ypk_detail_Activity, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getInfoAboutUpk(selectedUpkId: String) {
        //получаем данные из api
        lifecycleScope.launch {
            var result = authRepository.getUpkById(selectedUpkId)
            result.onSuccess {upk-> oldUpk = upk; displayUpkInfo(upk)  }
            result.onFailure { Toast.makeText(this@ypk_detail_Activity, "❌ Ошибка получения данных", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun  displayUpkInfo(upk: YpksDto){
        if(EditMode){
            // В режиме редактирования заполняем EditText
            binding.editTextTextName.setText(upk.ypkName?: "")
            binding.editTextTextContent.setText(upk.description)
        }
        else {
            binding.textViewName.text = upk.ypkName?: "без имени"
            binding.textViewContent.text = upk.description?: null
        }
        binding.recyclerViewProductByUpk.adapter = productAdapter
    }

    private fun back(){
        if(EditMode)
        {EditMode= false; displayUpkInfo(oldUpk);
            display()
        }
        else{finish()}
    }

    private fun display(){
        if(selectedUpkId == "new_ypk") {
            binding.recyclerViewProductByUpk.visibility = View.GONE
            binding.textViewName.visibility = View.GONE
            binding.textViewContent.visibility = View.GONE

            binding.buttonEdit.visibility = View.GONE
            binding.buttonDelete.visibility = View.GONE

            binding.editTextTextName.visibility = View.VISIBLE
            binding.editTextTextContent.visibility = View.VISIBLE

            binding.buttonSave.visibility = View.VISIBLE

            binding.buttonSave.setOnClickListener { addnewYpk() }
        }
        else{
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
            binding.textViewContent.visibility = View.VISIBLE

            binding.buttonEdit.setOnClickListener { edit()}
            binding.buttonSave.setOnClickListener { save()}

        }

        if (userRole == "Admin" && selectedUpkId != "new_ypk") {
            binding.buttonDelete.visibility = View.VISIBLE
            binding.buttonEdit.visibility = View.VISIBLE
        }

        if(EditMode) {binding.buttonBack.text = "отменить"}
        else {binding.buttonBack.text = "назад"}
    }

    private fun edit(){
        EditMode = true

        //отображаем поля ввода
        binding.editTextTextName.visibility= View.VISIBLE
        binding.editTextTextContent.visibility= View.VISIBLE
        //скрываем поля вывода
        binding.textViewName.visibility= View.GONE
        binding.textViewContent.visibility= View.GONE

        //выводим данные
        displayUpkInfo(oldUpk)

        //скрываем кнопки
        binding.buttonEdit.visibility = View.GONE
        binding.buttonDelete.visibility = View.GONE
        //раскрываем кнопки
        binding.buttonSave.visibility = View.VISIBLE

        binding.buttonBack.text = "отменить"
    }
    private fun save(){
        lifecycleScope.launch {
            try {
                val name = binding.editTextTextName.text.toString().trim()
                val content = binding.editTextTextContent.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this@ypk_detail_Activity, "⚠️ Введите имя", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val request = UpdateYpkDto(
                    id = oldUpk!!.id,
                    ypkName =name?:oldUpk?.ypkName ?: null,
                    description = content?:oldUpk?.description ?: null)

                val response = RetrofitClient.apiService.updateYpk(request)
                if(response.isSuccessful){
                    Toast.makeText(this@ypk_detail_Activity, "✅ Изменения сохранены", Toast.LENGTH_SHORT).show()
                    finish()
                }
                else {
                    var error = response.errorBody()?.string()?:"Неизвестная ошибка"
                    println("❌ ОШИБКА: ${response.code()} - $error")
                    Toast.makeText(this@ypk_detail_Activity, "❌ Ошибка: $error", Toast.LENGTH_LONG).show()
                    return@launch
                }

            }catch (e: Exception){
                println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@ypk_detail_Activity, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                return@launch
            }
        }
    }

    private fun loadProducts(ypkId: String){
        lifecycleScope.launch {
            val result = authRepository.getProductsByYpk(ypkId)
            result.onSuccess { response-> products = response.products;displayByUpk() }
            result.onFailure { error-> Toast.makeText(this@ypk_detail_Activity,error.message,Toast.LENGTH_LONG).show() }
        }
    }


    private fun displayByUpk(){
        if(productAdapter == null){
            productAdapter = ProductAdapter(products){product-> onProductClick(product)}
            binding.recyclerViewProductByUpk.adapter= productAdapter
        }else{productAdapter?.updateProducts(products)}
    }

    private fun onProductClick(product: ProductDto) {
        val prefs =getSharedPreferences("product_prefs", 0)
        prefs.edit().apply(){ putString("selected_product_id", product.id);apply() }

        //переход на другой activity
        val intent = Intent(this@ypk_detail_Activity, ProductDetailActivity::class.java)
        startActivity(intent)
    }

    fun addnewYpk() {

        val newYpk = CreateYpkDto(binding.editTextTextName.text.toString(),binding.editTextTextContent.text.toString())

        lifecycleScope.launch {
            val result = authRepository.addNewYpk(newYpk)
            result.onSuccess { Toast.makeText(this@ypk_detail_Activity, "успех", Toast.LENGTH_SHORT).show(); finish() }
            result.onFailure {  }
        }

    }
}