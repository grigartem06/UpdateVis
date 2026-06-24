package com.example.upk_btpi

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upk_btpi.Adapters.UpkAdapter
import com.example.upk_btpi.Models.Ypk.YpksDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Utils.ErrorHandler  // ✅ Импорт обработчика ошибок
import com.example.upk_btpi.databinding.FragmentUpkBinding
import kotlinx.coroutines.launch

class UpkFragment : Fragment() {

    // ✅ ViewBinding для безопасной работы с UI
    private var _binding: FragmentUpkBinding? = null
    private val binding get() = _binding!!

    // ✅ Репозиторий для работы с API
    private val authRepository = AuthRepository()

    // ✅ Адаптер для отображения списка УПК
    private var upkAdapter: UpkAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Настраиваем RecyclerView
        binding.recyclerViewUpk.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        // 🔹 Получаем роль пользователя для настройки видимости кнопки добавления
        val authPrefs = requireContext().getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val userRole = authPrefs.getString("user_role", null)

        // ✅ Скрываем кнопку добавления для всех, кроме Admin
        if (userRole != "Admin") {
            binding.floatingActionButton.visibility = View.GONE
        }

        // 🔹 Загружаем список УПК
        loadListOfUpk()

        // 🔹 Обработчики кнопок
        binding.floatingActionButton.setOnClickListener { newUpk() }
        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }
    }

    /**
     * Обновление данных с анимацией swipe-to-refresh
     */
    private fun refresh() {
        binding.swipeRefreshLayout.isRefreshing = true
        // Перезагружаем данные
        loadListOfUpk()
        // Останавливаем анимацию после загрузки
        binding.swipeRefreshLayout.isRefreshing = false
    }

    /**
     * Загружает список УПК из API и отображает их в RecyclerView
     */
    fun loadListOfUpk() {
        lifecycleScope.launch {
            // ✅ Асинхронный запрос к API
            val result = authRepository.getAllYpk()

            result.onSuccess { response ->
                val upks = response.ypks

                if (upks.isEmpty()) {
                    // 🔹 Показываем сообщение, если УПК нет
                    ErrorHandler.showDialog(
                        context = requireContext(),
                        title = "Информация",
                        message = "УПК пока нет. Создайте первый!",
                    )
                } else {
                    // 🔹 Отображаем список УПК
                    binding.recyclerViewUpk.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        setHasFixedSize(true)
                        adapter = UpkAdapter(upks) { upk -> OnUpkClick(upk) }
                    }
                }
            }

            result.onFailure { error ->
                // ✅ Ошибка API: показываем понятное сообщение
                ErrorHandler.showDialog(
                    context = requireContext(),
                    title = "Ошибка загрузки",
                    message = error.message ?: "Не удалось загрузить список УПК",
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ Освобождаем binding для предотвращения утечек памяти
        _binding = null
    }

    /**
     * Обработчик клика по УПК: сохраняет ID и переходит на экран деталей
     */
    private fun OnUpkClick(ypk: YpksDto) {
        // 🔹 Сохраняем ID УПК в SharedPreferences
        val prefs = requireContext().getSharedPreferences("ypk_prefs", 0)
        prefs.edit().apply {
            putString("selected_ypk_id", ypk.id)
            apply()
        }

        // 🔹 Переход на экран деталей УПК
        val intent = Intent(requireContext(), ypk_detail_Activity::class.java)
        startActivity(intent)
    }

    /**
     * Переход на экран создания нового УПК
     */
    private fun newUpk() {
        // 🔹 Сохраняем флаг "новый УПК" в SharedPreferences
        val prefs = requireContext().getSharedPreferences("ypk_prefs", 0)
        prefs.edit().apply {
            putString("selected_ypk_id", "new_ypk")
            apply()
        }

        // 🔹 Переход на экран деталей (режим создания)
        val intent = Intent(requireContext(), ypk_detail_Activity::class.java)
        startActivity(intent)
    }
}