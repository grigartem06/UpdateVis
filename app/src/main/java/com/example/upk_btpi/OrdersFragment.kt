package com.example.upk_btpi

import OrderDto
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upk_btpi.Adapters.OrderADapter
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Utils.ErrorHandler  // ✅ Импорт обработчика ошибок
import com.example.upk_btpi.databinding.FragmentOrdersBinding
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {

    // ✅ ViewBinding для безопасной работы с UI
    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    // ✅ Репозиторий для работы с API
    private val authRepository = AuthRepository()

    // ✅ Адаптер для отображения списка заказов
    private var orderAdapter: OrderADapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Получаем роль пользователя из SharedPreferences
        val authPrefs = requireContext().getSharedPreferences("auth_prefs", 0)
        val role = authPrefs.getString("user_role", null)

        // 🔹 Настраиваем RecyclerView
        binding.ListOfOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        // ✅ Проверка: авторизован ли пользователь
        if (role.isNullOrEmpty()) {
            ErrorHandler.showDialog(
                context = requireContext(),
                title = "Ошибка авторизации",
                message = "Пользователь не авторизован. Пожалуйста, войдите в аккаунт.",
            )
            return
        }

        // 🔹 Загружаем заказы для текущей роли
        loadOrders(role)
    }

    /**
     * Загружает список заказов из API в зависимости от роли пользователя
     * @param role Роль пользователя: "Admin", "Manager" или "DefaultUser"
     */
    fun loadOrders(role: String) {
        lifecycleScope.launch {
            // ✅ Асинхронный запрос к API
            val result = authRepository.getOrdersForUser()

            if (result != null) {
                result.onSuccess { response ->
                    val orders = response.orders
                    val binding = _binding ?: return@onSuccess

                    if (orders.isEmpty()) {
                        // 🔹 Показываем сообщение, если заказов нет
                        binding.ListOfOrders.visibility = View.GONE
                        binding.emptyOrdersMessage.visibility = View.VISIBLE
                        binding.emptyOrdersMessage.text = "Заказов нет"
                    } else {
                        // 🔹 Показываем список и обновляем адаптер
                        binding.ListOfOrders.visibility = View.VISIBLE
                        binding.emptyOrdersMessage.visibility = View.GONE

                        if (orderAdapter == null) {
                            // Первый раз: создаём адаптер
                            orderAdapter = OrderADapter(orders) { order -> onOrderClick(order) }
                            binding.ListOfOrders.adapter = orderAdapter
                        } else {
                            // Последующие разы: обновляем данные
                            orderAdapter?.updateOrders(orders)
                        }
                    }
                }

                result.onFailure { error ->
                    // ✅ Ошибка API: показываем понятное сообщение
                    ErrorHandler.showDialog(
                        context = requireContext(),
                        title = "Ошибка загрузки",
                        message = error.message ?: "Не удалось загрузить заказы",
                    )
                }
            }
        }
    }

    /**
     * Обработчик клика по заказу: сохраняет ID и переходит на экран деталей
     */
    private fun onOrderClick(order: OrderDto) {
        // 🔹 Сохраняем ID заказа в SharedPreferences
        val prefs = requireContext().getSharedPreferences("order_prefs", 0)
        prefs.edit().apply {
            putString("selected_orderId", order.id)
            apply()
        }

        // 🔹 Переход на экран деталей заказа
        val intent = Intent(requireContext(), order_detail_Activity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ Освобождаем binding для предотвращения утечек памяти
        _binding = null
    }
}