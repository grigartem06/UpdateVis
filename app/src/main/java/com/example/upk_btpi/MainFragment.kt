package com.example.upk_btpi

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.semantics.Role
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upk_btpi.Adapters.ProductAdapter
import com.example.upk_btpi.Models.Product.ProductDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.databinding.FragmentMainBinding
import kotlinx.coroutines.launch


class MainFragment : Fragment() {
    private  var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()
    private var productAdapter: ProductAdapter? = null
    private var searchQuery: String = ""
    private var allProducts : List<ProductDto> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater,container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Простая проверка: фрагмент добавлен и binding существует
        if (isAdded && _binding != null) { loadProducts() }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        loadProducts()



        binding.buttonCheckAll.setOnClickListener {filterProducts(FilterType.ALL) }
        binding.buttonCheckOrders.setOnClickListener {filterProducts(FilterType.PRODUCTS) }
        binding.buttonCheckServices.setOnClickListener {filterProducts(FilterType.SERVICES) }
        updateButtonStates(FilterType.ALL)
        binding.editTextText3.doAfterTextChanged {
            val query = binding.editTextText3.text.toString().trim()

            if (query.isEmpty()) {
                // Если запрос пустой — сбрасываем поиск
                searchQuery = ""
            } else {
                // Сохраняем запрос и применяем фильтрацию
                searchQuery = query
            }
            // Перефильтровываем список с учётом нового запроса
            // Определяем текущий тип фильтра (можно сохранить в переменной)
            val currentFilter = getCurrentFilterType() // см. ниже
            filterProducts(currentFilter)
        }


        binding.checkBox.setOnClickListener {loadProducts()  }
        binding.floatingActionButton.setOnClickListener { addNewProduct() }

        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }


    }

    private var currentFilterType: FilterType = FilterType.ALL

    private fun getCurrentFilterType(): FilterType = currentFilterType

    private fun refresh() {
        binding.swipeRefreshLayout.isRefreshing = true
        // Перезагружаем данные
        loadProducts()
        // Останавливаем анимацию после загрузки
        // Важно: делаем это в конце loadProducts() или здесь с задержкой
        binding.swipeRefreshLayout.isRefreshing = false
    }
    private enum class FilterType { ALL, PRODUCTS, SERVICES }

    private fun filterProducts(type: FilterType) {
        // ✨ Обновляем визуальное состояние кнопок
        updateButtonStates(type)

        // 1️⃣ Фильтруем по типу
        val filteredByType = when (type) {
            FilterType.ALL -> allProducts
            FilterType.PRODUCTS -> allProducts.filter { it.isProduct }
            FilterType.SERVICES -> allProducts.filter { !it.isProduct }
        }

        // 2️⃣ Фильтруем по поиску
        val finalList = if (searchQuery.isNotEmpty()) {
            filteredByType.filter { product ->
                product.productName?.contains(searchQuery, ignoreCase = true) == true ||
                        product.productInfo?.contains(searchQuery, ignoreCase = true) == true
            }
        } else {
            filteredByType
        }

        // 3️⃣ Показываем результат
        if (finalList.isEmpty() && searchQuery.isNotEmpty()) {
            Toast.makeText(requireContext(), "📭 Ничего не найдено", Toast.LENGTH_SHORT).show()
        }

        // 4️⃣ Обновляем адаптер
        productAdapter = ProductAdapter(finalList) { product -> onProductClick(product) }
        binding.recyclerViewProducts.adapter = productAdapter
    }

    private fun updateButtonStates(activeType: FilterType) {
        currentFilterType = activeType

        // Обновляем состояние selected для каждой кнопки
        binding.buttonCheckAll.isSelected = (activeType == FilterType.ALL)
        binding.buttonCheckOrders.isSelected = (activeType == FilterType.PRODUCTS)
        binding.buttonCheckServices.isSelected = (activeType == FilterType.SERVICES)
    }
    private fun  loadProducts(){
        val authPrefs =requireContext().getSharedPreferences("auth_prefs", MODE_PRIVATE)
        var userRole = authPrefs.getString("user_role", null)

        viewLifecycleOwner.lifecycleScope.launch {
            if(userRole == "DefaultUser") {
                //скоываем кнопку добавления
                binding.floatingActionButton.visibility = View.GONE
                //скрываем мастерскую
                binding.checkBox.visibility = View.GONE


                val result = authRepository.getAllProducts()
                result.onSuccess { response ->
                    allProducts = response.products
                    filterProducts(FilterType.ALL)
                }
                result.onFailure { error-> Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show() }
            }
            else {
                binding.checkBox.visibility = View.VISIBLE
                if (binding.checkBox.isChecked) {
                    val result = authRepository.getAllEdetingProducts()
                    result.onSuccess { response -> allProducts = response.products
                        filterProducts(FilterType.ALL) }

                    result.onFailure { error-> Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show() }
                }
                else {
                    val result = authRepository.getAllProducts()
                    result.onSuccess { response -> allProducts = response.products
                        filterProducts(FilterType.ALL) }

                    result.onFailure { error-> Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    private fun onProductClick(product: ProductDto) {
        val prefs = requireContext().getSharedPreferences("product_prefs", 0)
        prefs.edit().apply(){ putString("selected_product_id", product.id);apply() }

        //переход на другой activity
        val intent = Intent(requireContext(), ProductDetailActivity::class.java)
        startActivity(intent)
    }

    private fun addNewProduct(){
        val prefs = requireContext().getSharedPreferences("product_prefs", 0)
        prefs.edit().apply(){ putString("selected_product_id", "add_new_product");apply() }

        //переход на другой activity
        val intent = Intent(requireContext(), ProductDetailActivity::class.java)
        startActivity(intent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}