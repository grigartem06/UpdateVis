package com.example.upk_btpi

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.upk_btpi.Models.Feedback.FeedbackDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.databinding.ActivityFeedbackDetailBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class feedback_detail_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackDetailBinding
    private var editMode: Boolean = false
    private var selectedFeedback: String? = null
    private var userRole: String? = null
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityFeedbackDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val feedbackPrefs = getSharedPreferences("feedback_prefs", MODE_PRIVATE)
        selectedFeedback = feedbackPrefs.getString("selected_feedback_id", null)

        val authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        userRole = authPrefs.getString("user_role", null)

        if (selectedFeedback.isNullOrEmpty()) {
            Toast.makeText(this, "⚠️ ID отзыва не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (selectedFeedback == "new_feedback") { newFeedback() }
        else {
            setupUIByRole()
            loadInfoAboutFeedback(selectedFeedback!!)

            binding.buttonEdit.setOnClickListener { enableEditMode() }
            binding.buttonSave.setOnClickListener { saveUpdate(selectedFeedback!!) }
            binding.buttonDelete.setOnClickListener { deleteFeedback(selectedFeedback!!) }
        }

        binding.buttonBack.setOnClickListener { finish() }
    }

    private fun setupUIByRole() {
        if (userRole == "Admin") {
            binding.buttonEdit.visibility = View.VISIBLE
            binding.buttonDelete.visibility = View.VISIBLE
        }
        else { binding.buttonDelete.visibility = View.GONE }
    }

    private fun loadInfoAboutFeedback(feedbackId: String) {
        lifecycleScope.launch {
            val result = authRepository.getFeedbackById(feedbackId)
            result.onSuccess { feedback -> displayFeedback(feedback) }
            result.onFailure { Toast.makeText(this@feedback_detail_Activity, "❌ Ошибка получения данных", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun displayFeedback(feedback: FeedbackDto) {
        binding.textViewComment.text = feedback.comment ?: "Без комментария"

        // ✅ Устанавливаем рейтинг в TextView И в RatingBar
        val rating = feedback.raiting?.toInt() ?: 0
        binding.textViewRaitind.text = "⭐ $rating / 5"
        binding.ratingBar.rating = rating.toFloat()  // ✅ RatingBar принимает Float

        binding.textViewUserName.text = feedback.user?.fullname ?: "Аноним"
    }

    private fun enableEditMode() {
        editMode = true

        // Скрываем TextView
        binding.textViewComment.visibility = View.GONE
        binding.textViewRaitind.visibility = View.GONE

        // Показываем поля ввода
        binding.editTextTextComment.visibility = View.VISIBLE
        binding.ratingBar.visibility = View.VISIBLE  // ✅ Показываем RatingBar

        // Заполняем данными
        binding.editTextTextComment.setText(binding.textViewComment.text)

        // ✅ Получаем рейтинг из TextView и устанавливаем в RatingBar
        val currentRating = binding.textViewRaitind.text.toString()
            .replace("⭐ ", "")
            .replace(" / 5", "")
            .toIntOrNull() ?: 3
        binding.ratingBar.rating = currentRating.toFloat()

        // Кнопки
        binding.buttonSave.visibility = View.VISIBLE
        binding.buttonEdit.visibility = View.GONE
    }

    private fun saveUpdate(feedbackId: String) {
        lifecycleScope.launch {
            try {
                val commentBody = binding.editTextTextComment.text.toString().trim().toRequestBody("text/plain".toMediaType())
                // ✅ Получаем рейтинг из RatingBar (возвращает Float)
                val ratingValue = binding.ratingBar.rating.toInt()  // 1, 2, 3, 4 или 5
                val ratingBody = ratingValue.toString().toRequestBody("text/plain".toMediaType())
                val idBody = feedbackId.toRequestBody("text/plain".toMediaType())

                val response = RetrofitClient.apiService.updateFeedback(
                    id = idBody,
                    comment = commentBody,
                    rating = ratingBody
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@feedback_detail_Activity, "✅ Успешно", Toast.LENGTH_SHORT).show()
                    editMode = false
                    displayFeedback(authRepository.getFeedbackById(feedbackId).getOrNull() ?: return@launch)
                }
                else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    println("❌ ОШИБКА: ${response.code()} - $error")
                    Toast.makeText(this@feedback_detail_Activity, "❌ Ошибка: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
                Toast.makeText(this@feedback_detail_Activity, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun newFeedback() {
        binding.editTextTextComment.visibility = View.VISIBLE
        binding.ratingBar.visibility = View.VISIBLE
        binding.textViewComment.visibility = View.GONE
        binding.textViewRaitind.visibility = View.GONE
        binding.buttonSave.visibility = View.VISIBLE
        binding.buttonSave.text = "Отправить"

        binding.buttonSave.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val comment = binding.editTextTextComment.text.toString().trim()
                    val rating = binding.ratingBar.rating.toInt()

                    if (comment.isEmpty()) {
                        Toast.makeText(this@feedback_detail_Activity, "⚠️ Введите комментарий", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val result = authRepository.addNewFeedback(comment, rating, null, this@feedback_detail_Activity)

                    result.onSuccess {
                        Toast.makeText(this@feedback_detail_Activity, "✅ Отзыв отправлен!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    result.onFailure {
                        Toast.makeText(this@feedback_detail_Activity, "❌ ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@feedback_detail_Activity, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

    private fun performDeleteFeedback(feedbackId: String) {
        lifecycleScope.launch {
            try {
                println("🗑️ Удаление отзыва: $feedbackId")

                val response = RetrofitClient.apiService.deleteFeedbackById(feedbackId)

                println("📥 Ответ сервера: ${response.code()}")

                if (response.isSuccessful) {
                    println("✅ Отзыв удалён")
                    Toast.makeText(this@feedback_detail_Activity, "✅ Отзыв удалён", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    println("❌ ОШИБКА: ${response.code()} - $error")
                    Toast.makeText(this@feedback_detail_Activity, "❌ Ошибка: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@feedback_detail_Activity, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}