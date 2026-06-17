package com.example.upk_btpi

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upk_btpi.Adapters.FeedbackAdapter
import com.example.upk_btpi.Models.Feedback.FeedbackDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.databinding.FragmentFeedbackBinding
import kotlinx.coroutines.launch

class FeedbackFragment : Fragment() {

    private var _binding: FragmentFeedbackBinding ?= null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()
    private var feedbackAdapter: FeedbackAdapter ?= null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ListOfFeedbacks.apply { layoutManager = LinearLayoutManager(requireContext())
        setHasFixedSize(true)}

        loadFeedbacks()

        binding.floatingActionButton.setOnClickListener { AddNewFeedback() }
        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }
    }

    fun loadFeedbacks() {
        lifecycleScope.launch {
            val result = authRepository.getAllFeedbacks()
            if(result != null) {
                result.onSuccess { response ->
                    val feedbacks = response.feedbacks
                    if(feedbacks.isEmpty()) { Toast.makeText(requireContext(),"Отзывов нет", Toast.LENGTH_LONG).show() }
                    else {
                        if(feedbackAdapter == null) {
                            feedbackAdapter = FeedbackAdapter(feedbacks) {feedback -> onFeedbackClick(feedback)}
                            binding.ListOfFeedbacks.adapter = feedbackAdapter }
                        else { feedbackAdapter?.updateFeedbacks(feedbacks)}
                        }
                    }
                }
                result.onFailure {  }
            }
        }

    private fun onFeedbackClick(feedback: FeedbackDto) {
        val prefs = requireContext().getSharedPreferences("feedback_prefs",0)
        prefs.edit().apply(){putString("selected_feedback_id", feedback.id); apply()}

        //переход
        val intent = Intent(requireContext(), feedback_detail_Activity::class.java)
        startActivity(intent)
    }

    private fun AddNewFeedback(){
        val prefs = requireContext().getSharedPreferences("feedback_prefs",0)
        prefs.edit().apply(){putString("selected_feedback_id", "new_feedback"); apply()}

        //переход
        val intent = Intent(requireContext(), feedback_detail_Activity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refresh() {
        binding.swipeRefreshLayout.isRefreshing = true
        // Перезагружаем данные
        loadFeedbacks()
        // Останавливаем анимацию после загрузки
        // Важно: делаем это в конце loadProducts() или здесь с задержкой
        binding.swipeRefreshLayout.isRefreshing = false
    }
}




