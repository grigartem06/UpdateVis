package com.example.upk_btpi

import ProductDto
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upk_btpi.Adapters.ProductAdapter
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Utils.ErrorHandler  // ✅ Импорт обработчика ошибок
import com.example.upk_btpi.databinding.FragmentMainBinding
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    // ✅ ViewBinding для безопасной работы с UI
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    // ✅ Репозиторий для работы с API
    private val authRepository = AuthRepository()

    // ✅ Адаптер и данные для RecyclerView
    private var productAdapter: ProductAdapter? = null
    private var searchQuery: String = ""
    private var allProducts: List<ProductDto> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // ✅ Перезагружаем данные при возврате на фрагмент
        if (isAdded && _binding != null) { loadProducts() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Настраиваем RecyclerView
        binding.recyclerViewProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        // 🔹 Загружаем список продуктов
        loadProducts()

        // 🔹 Обработчики кнопок фильтрации
        binding.buttonCheckAll.setOnClickListener { filterProducts(FilterType.ALL) }
        binding.buttonCheckOrders.setOnClickListener { filterProducts(FilterType.PRODUCTS) }
        binding.buttonCheckServices.setOnClickListener { filterProducts(FilterType.SERVICES) }
        updateButtonStates(FilterType.ALL)

        // 🔹 Поиск: фильтрация по вводу текста
        binding.editTextText3.doAfterTextChanged {
            val query = binding.editTextText3.text.toString().trim()
            searchQuery = if (query.isEmpty()) "" else query
            filterProducts(getCurrentFilterType())
        }

        // 🔹 Чекбокс "Мастерская" — переключает режим загрузки продуктов
        binding.checkBox.setOnClickListener { loadProducts() }

        // 🔹 Кнопка добавления нового продукта
        binding.floatingActionButton.setOnClickListener { addNewProduct() }

        // 🔹 Swipe-to-refresh
        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }
    }

    // ✅ Текущий тип фильтра (для поиска)
    private var currentFilterType: FilterType = FilterType.ALL
    private fun getCurrentFilterType(): FilterType = currentFilterType

    /**
     * Обновление данных с анимацией swipe
     */
    private fun refresh() {
        binding.swipeRefreshLayout.isRefreshing = true
        loadProducts()
        // ✅ Останавливаем анимацию после загрузки
        binding.swipeRefreshLayout.isRefreshing = false
    }

    // ✅ Типы фильтрации для кнопок
    private enum class FilterType { ALL, PRODUCTS, SERVICES }

    /**
     * Фильтрация списка продуктов по типу и поисковому запросу
     */
    private fun filterProducts(type: FilterType) {
        // ✨ Обновляем визуальное состояние кнопок
        updateButtonStates(type)

        // 1️⃣ Фильтруем по типу (товар/услуга/все)
        val filteredByType = when (type) {
            FilterType.ALL -> allProducts
            FilterType.PRODUCTS -> allProducts.filter { it.isProduct }
            FilterType.SERVICES -> allProducts.filter { !it.isProduct }
        }

        // 2️⃣ Фильтруем по поисковому запросу
        val finalList = if (searchQuery.isNotEmpty()) {
            filteredByType.filter { product ->
                product.productName?.contains(searchQuery, ignoreCase = true) == true ||
                        product.productInfo?.contains(searchQuery, ignoreCase = true) == true
            }
        } else {
            filteredByType
        }

        // 3️⃣ Если поиск не дал результатов — показываем сообщение
        if (finalList.isEmpty() && searchQuery.isNotEmpty()) {
            // ✅ Это UX-сообщение, а не ошибка — можно оставить Toast или использовать Snackbar
            ErrorHandler.showDialog(
                context = requireContext(),
                title = "Поиск",
                message = "📭 Ничего не найдено по запросу \"$searchQuery\"",
            )
        }

        // 4️⃣ Обновляем адаптер с новым списком
        productAdapter = ProductAdapter(finalList) { product -> onProductClick(product) }
        binding.recyclerViewProducts.adapter = productAdapter
    }

    /**
     * Обновляет состояние кнопок фильтра (активная/неактивная)
     */
    private fun updateButtonStates(activeType: FilterType) {
        currentFilterType = activeType
        binding.buttonCheckAll.isSelected = (activeType == FilterType.ALL)
        binding.buttonCheckOrders.isSelected = (activeType == FilterType.PRODUCTS)
        binding.buttonCheckServices.isSelected = (activeType == FilterType.SERVICES)
    }

    /**
     * Загружает список продуктов из API в зависимости от роли пользователя
     */
    private fun loadProducts() {
        val authPrefs = requireContext().getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val userRole = authPrefs.getString("user_role", null)

        viewLifecycleOwner.lifecycleScope.launch {
            if (userRole == "DefaultUser") {
                // 🔹 Обычный пользователь: видит только публичные продукты
                binding.floatingActionButton.visibility = View.GONE
                binding.checkBox.visibility = View.GONE

                val result = authRepository.getAllProducts()
                result.onSuccess { response ->
                    allProducts = response.products
                    filterProducts(FilterType.ALL)
                }
                result.onFailure { error ->
                    // ✅ Ошибка API: показываем понятное сообщение
                    ErrorHandler.showDialog(
                        context = requireContext(),
                        title = "Ошибка загрузки",
                        message = error.message ?: "Не удалось загрузить продукты",
                    )
                }
            } else {
                // 🔹 Admin/Manager: видят кнопку "Мастерская" и черновики
                binding.checkBox.visibility = View.VISIBLE

                val result = if (binding.checkBox.isChecked) {
                    authRepository.getAllEditingProducts()  // Только черновики
                } else {
                    authRepository.getAllProducts()  // Все продукты
                }

                result.onSuccess { response ->
                    allProducts = response.products
                    filterProducts(FilterType.ALL)
                }
                result.onFailure { error ->
                    ErrorHandler.showDialog(
                        context = requireContext(),
                        title = "Ошибка загрузки",
                        message = error.message ?: "Не удалось загрузить продукты",
                    )
                }
            }
        }
    }

    /**
     * Обработчик клика по продукту: переход на экран деталей
     */
    private fun onProductClick(product: ProductDto) {
        val prefs = requireContext().getSharedPreferences("product_prefs", 0)
        prefs.edit().apply {
            putString("selected_product_id", product.id)
            apply()
        }
        val intent = Intent(requireContext(), ProductDetailActivity::class.java)
        startActivity(intent)
    }

    /**
     * Переход на экран создания нового продукта
     */
    private fun addNewProduct() {
        val prefs = requireContext().getSharedPreferences("product_prefs", 0)
        prefs.edit().apply {
            putString("selected_product_id", "add_new_product")
            apply()
        }
        val intent = Intent(requireContext(), ProductDetailActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // ✅ Освобождаем binding для предотвращения утечек памяти
    }
}