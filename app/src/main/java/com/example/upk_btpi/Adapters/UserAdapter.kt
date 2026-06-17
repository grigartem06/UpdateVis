package com.example.upk_btpi.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewParent
import androidx.recyclerview.widget.RecyclerView
import com.example.upk_btpi.Models.User.UserDto
import com.example.upk_btpi.databinding.ItemOrderBinding
import com.example.upk_btpi.databinding.ItemUserBinding
import kotlinx.serialization.BinaryFormat

class UserAdapter(
    private var users: List<UserDto>,
    private val onItemClick: (UserDto) -> Unit
 ): RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class  UserViewHolder(private val binding: ItemUserBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserDto, onClick:(UserDto)-> Unit) {
            binding.textViewName.text = user.fullname?:"нет имени"
            binding.textViewPhoneNumber.text = user.phoneNumber?: "нет номер телефона"
            if(user.isActive) {binding.textViewIsActive.text = "активный пользователь"}
                else binding.textViewIsActive.text = "не активный пользователь"
            binding.root.setOnClickListener { onClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder{
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) { holder.bind(users[position],onItemClick) }

     fun updateUsers(newUsers: List<UserDto>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun getItemCount() = users.size

 }