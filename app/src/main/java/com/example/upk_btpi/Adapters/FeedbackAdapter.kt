package com.example.upk_btpi.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.upk_btpi.Models.Feedback.FeedbackDto
import com.example.upk_btpi.Models.Order.OrderDto
import com.example.upk_btpi.databinding.ItemFeedbackBinding
import com.example.upk_btpi.databinding.ItemOrderBinding
import java.security.PrivateKey

class FeedbackAdapter(
    private  var feedbacks: List<FeedbackDto>,
    private val onItemClick:(FeedbackDto) -> Unit
): RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder>() {

    class FeedbackViewHolder(private val binding: ItemFeedbackBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(feedback: FeedbackDto, onItemClick: (FeedbackDto) -> Unit) {
        binding.textViewComment.text = feedback.comment
            binding.textViewRaitind.text = feedback.raiting.toString()
            binding.root.setOnClickListener { onItemClick(feedback) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val binding = ItemFeedbackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedbackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int){ holder.bind(feedbacks[position], onItemClick) }

    fun updateFeedbacks(newFeedbacks: List<FeedbackDto>) {
        feedbacks = newFeedbacks
        notifyDataSetChanged()
    }

    override fun getItemCount() = feedbacks.size
}