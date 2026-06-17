package com.example.upk_btpi.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.upk_btpi.Models.Ypk.YpksDto
import com.example.upk_btpi.databinding.ItemOrderBinding
import com.example.upk_btpi.databinding.ItemUpkBinding

class UpkAdapter(
    private var upks: List<YpksDto>,
    private var OnItemClick:(YpksDto) -> Unit): RecyclerView.Adapter<UpkAdapter.UpkViewHolder>() {

    class UpkViewHolder(private val binding: ItemUpkBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(upk: YpksDto, onClick:(YpksDto)-> Unit) {
            binding.textViewName.text = upk.ypkName ?: "нет имени"
            binding.root.setOnClickListener { onClick(upk) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpkViewHolder {
        val binding = ItemUpkBinding.inflate(LayoutInflater.from(parent.context),parent, false)
        return UpkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpkViewHolder, position: Int) {  holder.bind(upks[position],OnItemClick) }

    fun updateUpks(newUpks: List<YpksDto>) {
        upks = newUpks
        notifyDataSetChanged()
    }

    override fun getItemCount() = upks.size
}

