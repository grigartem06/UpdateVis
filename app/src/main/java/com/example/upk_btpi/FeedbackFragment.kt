package com.example.upk_btpi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upk_btpi.Adapters.FeedbackAdapter
import com.example.upk_btpi.Models.Feedback.FeedbackDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Utils.ErrorHandler  // ✅ Импорт обработчика ошибок
import com.example.upk_btpi.databinding.FragmentFeedbackBinding
import kotlinx.coroutines.launch

class FeedbackFragment : Fragment() {

    // ✅ ViewBinding для безопасной работы с UI
    private var _binding: FragmentFeedbackBinding? = null
    private val binding get() = _binding!!

    // ✅ Репозиторий для работы с API
    private val authRepository = AuthRepository()

    // ✅ Адаптер для отображения списка отзывов
    private var feedbackAdapter: FeedbackAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Настраиваем RecyclerView
        binding.ListOfFeedbacks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        // 🔹 Загружаем список отзывов
        loadFeedbacks()

        // 🔹 Обработчики кнопок
        binding.floatingActionButton.setOnClickListener { AddNewFeedback() }
        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }
    }

    /**
     * Загружает список отзывов из API и отображает их в RecyclerView
     */
    fun loadFeedbacks() {
        lifecycleScope.launch {
            // ✅ Асинхронный запрос к API
            val result = authRepository.getAllFeedback()

            if (result != null) {
                result.onSuccess { response ->
                    val feedbacks = response.feedbacks

                    if (feedbacks.isEmpty()) {
                        // 🔹 Показываем сообщение, если отзывов нет
                        ErrorHandler.showDialog(
                            context = requireContext(),
                            title = "Информация",
                            message = "Отзывов пока нет. Будьте первым!",
                        )
                    } else {
                        // 🔹 Отображаем список отзывов
                        if (feedbackAdapter == null) {
                            // Первый раз: создаём адаптер
                            feedbackAdapter = FeedbackAdapter(feedbacks) { feedback ->
                                onFeedbackClick(feedback)
                            }
                            binding.ListOfFeedbacks.adapter = feedbackAdapter
                        } else {
                            // Последующие разы: обновляем данные
                            feedbackAdapter?.updateFeedbacks(feedbacks)
                        }
                    }
                }

                result.onFailure { error ->
                    // ✅ Ошибка API: показываем понятное сообщение
                    ErrorHandler.showDialog(
                        context = requireContext(),
                        title = "Ошибка загрузки",
                        message = error.message ?: "Не удалось загрузить отзывы",
                    )
                }
            }
        }
    }

    /**
     * Обработчик клика по отзыву: сохраняет ID и переходит на экран деталей
     */
    private fun onFeedbackClick(feedback: FeedbackDto) {
        // 🔹 Сохраняем ID отзыва в SharedPreferences
        val prefs = requireContext().getSharedPreferences("feedback_prefs", 0)
        prefs.edit().apply {
            putString("selected_feedback_id", feedback.id)
            apply()
        }

        // 🔹 Переход на экран деталей отзыва
        val intent = Intent(requireContext(), feedback_detail_Activity::class.java)
        startActivity(intent)
    }

    /**
     * Переход на экран создания нового отзыва
     */
    private fun AddNewFeedback() {
        // 🔹 Сохраняем флаг "новый отзыв" в SharedPreferences
        val prefs = requireContext().getSharedPreferences("feedback_prefs", 0)
        prefs.edit().apply {
            putString("selected_feedback_id", "new_feedback")
            apply()
        }

        // 🔹 Переход на экран деталей (режим создания)
        val intent = Intent(requireContext(), feedback_detail_Activity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ Освобождаем binding для предотвращения утечек памяти
        _binding = null
    }

    /**
     * Обновление данных с анимацией swipe-to-refresh
     */
    private fun refresh() {
        binding.swipeRefreshLayout.isRefreshing = true
        // Перезагружаем данные
        loadFeedbacks()
        // Останавливаем анимацию после загрузки
        binding.swipeRefreshLayout.isRefreshing = false
    }
}