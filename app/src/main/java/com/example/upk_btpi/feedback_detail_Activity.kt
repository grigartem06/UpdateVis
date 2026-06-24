package com.example.upk_btpi

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.upk_btpi.Models.Feedback.FeedbackDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.Utils.ErrorHandler  // ✅ Импорт обработчика ошибок
import com.example.upk_btpi.databinding.ActivityFeedbackDetailBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class feedback_detail_Activity : AppCompatActivity() {

    // ✅ ViewBinding для безопасной работы с UI
    private lateinit var binding: ActivityFeedbackDetailBinding

    // ✅ Флаги и данные для работы с отзывом
    private var editMode: Boolean = false
    private var selectedFeedback: String? = null
    private var userRole: String? = null

    // ✅ Репозиторий для работы с API
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Инициализация ViewBinding
        binding = ActivityFeedbackDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Получаем данные из SharedPreferences
        val feedbackPrefs = getSharedPreferences("feedback_prefs", MODE_PRIVATE)
        selectedFeedback = feedbackPrefs.getString("selected_feedback_id", null)

        val authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        userRole = authPrefs.getString("user_role", null)

        // ✅ Проверка: найден ли ID отзыва
        if (selectedFeedback.isNullOrEmpty()) {
            ErrorHandler.showDialog(
                context = this,
                title = "Ошибка",
                message = "ID отзыва не найден. Попробуйте выбрать отзыв заново.",
            )
            finish()
            return
        }

        // 🔹 Обработка: новый отзыв или существующий
        if (selectedFeedback == "new_feedback") {
            newFeedback()  // Режим создания нового отзыва
        }
        else {
            setupUIByRole()  // Настройка видимости кнопок по роли
            loadInfoAboutFeedback(selectedFeedback!!)  // Загрузка данных отзыва

            // ✅ Обработчики кнопок для существующего отзыва
            binding.buttonEdit.setOnClickListener { enableEditMode() }
            binding.buttonSave.setOnClickListener { saveUpdate(selectedFeedback!!) }
            binding.buttonDelete.setOnClickListener { deleteFeedback(selectedFeedback!!) }
        }

        binding.buttonBack.setOnClickListener { finish() }
    }

    /**
     * Настраивает видимость кнопок в зависимости от роли пользователя
     * Admin: может редактировать и удалять
     * DefaultUser: может только просматривать
     */
    private fun setupUIByRole() {
        if (userRole == "Admin") {
            binding.buttonEdit.visibility = View.VISIBLE
            binding.buttonDelete.visibility = View.VISIBLE
        }
        else {
            binding.buttonDelete.visibility = View.GONE
        }
    }

    /**
     * Загружает данные отзыва по ID и отображает их в UI
     */
    private fun loadInfoAboutFeedback(feedbackId: String) {
        lifecycleScope.launch {
            val result = authRepository.getFeedbackById(feedbackId)

            result.onSuccess { feedback ->
                displayFeedback(feedback)  // Успех: отображаем данные
            }

            result.onFailure { error ->
                // Ошибка API: показываем понятное сообщение
                ErrorHandler.showDialog(
                    context = this@feedback_detail_Activity,
                    title = "Ошибка загрузки",
                    message = error.message ?: "Не удалось загрузить данные отзыва",
                )
            }
        }
    }

    /**
     * Отображает данные отзыва в TextView и RatingBar
     */
    private fun displayFeedback(feedback: FeedbackDto) {
        binding.textViewComment.text = feedback.comment ?: "Без комментария"

        // ✅ Устанавливаем рейтинг в TextView И в RatingBar
        val rating = feedback.raiting?.toInt() ?: 0
        binding.textViewRaitind.text = "⭐ $rating / 5"
        binding.ratingBar.rating = rating.toFloat()  // ✅ RatingBar принимает Float

        binding.textViewUserName.text = feedback.user?.fullName ?: "Аноним"
    }

    /**
     * Включает режим редактирования: скрывает TextView, показывает поля ввода
     */
    private fun enableEditMode() {
        editMode = true

        // Скрываем TextView (режим просмотра)
        binding.textViewComment.visibility = View.GONE
        binding.textViewRaitind.visibility = View.GONE

        // Показываем поля ввода (режим редактирования)
        binding.editTextTextComment.visibility = View.VISIBLE
        binding.ratingBar.visibility = View.VISIBLE  // ✅ Показываем RatingBar

        // Заполняем поля текущими данными
        binding.editTextTextComment.setText(binding.textViewComment.text)

        // ✅ Получаем рейтинг из TextView и устанавливаем в RatingBar
        val currentRating = binding.textViewRaitind.text.toString()
            .replace("⭐ ", "")
            .replace(" / 5", "")
            .toIntOrNull() ?: 3
        binding.ratingBar.rating = currentRating.toFloat()

        // Переключаем кнопки
        binding.buttonSave.visibility = View.VISIBLE
        binding.buttonEdit.visibility = View.GONE
    }

    /**
     * Сохраняет изменения в отзыве через API
     */
    private fun saveUpdate(feedbackId: String) {
        lifecycleScope.launch {
            try {
                // ✅ Создаём RequestBody для текстовых полей
                val commentBody = binding.editTextTextComment.text.toString().trim()
                    .toRequestBody("text/plain".toMediaType())

                // ✅ Получаем рейтинг из RatingBar (возвращает Float)
                val ratingValue = binding.ratingBar.rating.toInt()  // 1, 2, 3, 4 или 5
                val ratingBody = ratingValue.toString().toRequestBody("text/plain".toMediaType())

                val idBody = feedbackId.toRequestBody("text/plain".toMediaType())

                // ✅ Вызываем API для обновления отзыва
                val response = RetrofitClient.apiService.updateFeedback(
                    id = idBody,
                    comment = commentBody,
                    rating = ratingBody
                )

                if (response.isSuccessful) {
                    // Успех: показываем диалог и обновляем UI
                    ErrorHandler.showDialog(
                        context = this@feedback_detail_Activity,
                        title = "Успех",
                        message = "✅ Изменения сохранены",
                    )
                    editMode = false
                    // Перезагружаем данные отзыва
                    val updated = authRepository.getFeedbackById(feedbackId).getOrNull()
                    if (updated != null) { displayFeedback(updated) }
                }
                else {
                    // Ошибка API: показываем понятное сообщение
                    ErrorHandler.showDialog(
                        context = this@feedback_detail_Activity,
                        title = "Ошибка сохранения",
                        message = ErrorHandler.handleApiError(response),
                    )
                }
            } catch (e: Exception) {
                // Сетевая или системная ошибка
                ErrorHandler.showErrorWithMessage(this@feedback_detail_Activity, e)
            }
        }
    }

    /**
     * Настраивает UI для создания нового отзыва
     */
    private fun newFeedback() {
        // Показываем поля ввода, скрываем TextView
        binding.editTextTextComment.visibility = View.VISIBLE
        binding.ratingBar.visibility = View.VISIBLE
        binding.textViewComment.visibility = View.GONE
        binding.textViewRaitind.visibility = View.GONE
        binding.buttonSave.visibility = View.VISIBLE
        binding.buttonSave.text = "Отправить"

        // ✅ Обработчик отправки нового отзыва
        binding.buttonSave.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val comment = binding.editTextTextComment.text.toString().trim()
                    val rating = binding.ratingBar.rating.toInt()

                    // ✅ Валидация: комментарий не должен быть пустым
                    if (comment.isEmpty()) {
                        // Показываем ошибку прямо в поле ввода (inline-валидация)
                        binding.editTextTextComment.error = "Введите комментарий"
                        binding.editTextTextComment.requestFocus()
                        return@launch
                    }

                    // ✅ Отправляем новый отзыв через репозиторий
                    val result = authRepository.addNewFeedback(comment, rating, null, this@feedback_detail_Activity)

                    result.onSuccess {
                        ErrorHandler.showDialog(
                            context = this@feedback_detail_Activity,
                            title = "Отправлено",
                            message = "✅ Ваш отзыв успешно отправлен",
                        )
                        finish()
                    }

                    result.onFailure { error ->
                        ErrorHandler.showDialog(
                            context = this@feedback_detail_Activity,
                            title = "Ошибка отправки",
                            message = error.message ?: "Не удалось отправить отзыв",
                        )
                    }
                } catch (e: Exception) {
                    ErrorHandler.showErrorWithMessage(this@feedback_detail_Activity, e)
                }
            }
        }
    }

    /**
     * Показывает диалог подтверждения удаления отзыва
     */
    private fun deleteFeedback(feedbackId: String) {
        AlertDialog.Builder(this)
            .setTitle("Удаление отзыва")
            .setMessage("Вы уверены, что хотите удалить этот отзыв? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                performDeleteFeedback(feedbackId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Выполняет удаление отзыва через API
     */
    private fun performDeleteFeedback(feedbackId: String) {
        lifecycleScope.launch {
            try {
                println("🗑️ Удаление отзыва: $feedbackId")

                val response = RetrofitClient.apiService.deleteFeedbackById(feedbackId)

                println("📥 Ответ сервера: ${response.code()}")

                if (response.isSuccessful) {
                    println("✅ Отзыв удалён")
                    ErrorHandler.showDialog(
                        context = this@feedback_detail_Activity,
                        title = "Удалено",
                        message = "✅ Отзыв успешно удалён",
                    )
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    println("❌ ОШИБКА: ${response.code()} - $error")
                    ErrorHandler.showDialog(
                        context = this@feedback_detail_Activity,
                        title = "Ошибка удаления",
                        message = ErrorHandler.handleApiError(response),
                    )
                }
            } catch (e: Exception) {
                println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
                e.printStackTrace()
                ErrorHandler.showErrorWithMessage(this@feedback_detail_Activity, e)
            }
        }
    }
}