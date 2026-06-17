package com.example.upk_btpi

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.autoSaver
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.upk_btpi.Adapters.UserAdapter
import com.example.upk_btpi.Models.User.UpdateUserDto
import com.example.upk_btpi.Models.User.UserDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import kotlin.io.path.Path
import kotlin.uuid.ExperimentalUuidApi

class ProfileFragment : Fragment() {
    private  var _binding: FragmentProfileBinding? = null
    private val authRepository = AuthRepository()
    var isEditMode = false
    private var  oldUser :UserDto ?=null
    private  val binding get() = _binding!!
    var nowUserID: String ?= null
    var role: String ?= null
    var token: String ?= null
    // Переменная для хранения URI выбранного изображения
    private var selectedImageUri: Uri? = null
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent())
    { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.imageView4.setImageURI(it)
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentProfileBinding.inflate(inflater,container,false)
        return binding.root
    }

     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

         val prefs = requireContext().getSharedPreferences("auth_prefs", 0)
         nowUserID = prefs.getString("user_id", null)
         role = prefs.getString("user_role", null)
         token = prefs.getString("auth_token", null)

         //загрузка данные об аккаунте
        loadUserProfile()

         //загрузка списка пользователей
         if(role =="Admin") {

         }

        // Кнопка "Выход"
        binding.buttonLogOut.setOnClickListener {
            if(!isEditMode)
            {
                //"выйти из аккаунта
                val prefs = requireContext().getSharedPreferences("auth_prefs", 0)
                prefs.edit().clear().apply()

                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

            }
            else{ //"отмена"
                findNavController().apply { popBackStack()
                navigate(R.id.profileFragment)}
            }

        }

         //кнопка редактирования
         binding.buttonEdit.setOnClickListener {
             if (isEditMode) { // Сохранение изменений
                 isEditMode = false
                 binding.buttonEdit.text = "Изменить"
                 binding.buttonLogOut.text = "Выйти"
                 binding.imageView4.setOnClickListener(null)

                 val prefs = requireContext().getSharedPreferences("auth_prefs", 0)
                 val roleName = prefs.getString("user_role", null)

                 lifecycleScope.launch {
                     try {
                         // ✅ Получаем roleId
                         val rolesResponse = RetrofitClient.apiService.getAllRoles()
                         if (!rolesResponse.isSuccessful || rolesResponse.body() == null) {
                             Toast.makeText(requireContext(), "❌ Ошибка загрузки ролей", Toast.LENGTH_SHORT).show()
                             return@launch
                         }
                         val roles = rolesResponse.body()!!.roles
                         val role = roles.find { it.roleName?.equals(roleName, ignoreCase = true) == true }
                         val roleId = role?.id ?: ""

                         val oldPassword = ""
                         val newPassword = ""

                         // ✅ Создаем RequestBody для каждого поля
                         val idBody = oldUser!!.id.toRequestBody("text/plain".toMediaType())

                         val oldPasswordBody = if (oldPassword.isNotEmpty()) { oldPassword.toRequestBody("text/plain".toMediaType()) }
                         else { null }

                         val newPasswordBody = if (newPassword.isNotEmpty()) { newPassword.toRequestBody("text/plain".toMediaType()) }
                         else { null }

                         val fullnameBody = binding.editTextTextName.text.toString().toRequestBody("text/plain".toMediaType())
                         val phoneNumberBody = binding.editTextTextPhone.text.toString().toRequestBody("text/plain".toMediaType())
                         val userInfoBody = binding.editTextTextInf.text.toString().toRequestBody("text/plain".toMediaType())
                         val isActiveBody = (oldUser?.isActive ?: true)
                             .toString()
                             .toRequestBody("text/plain".toMediaType())

                         val avatarPart = selectedImageUri?.let { uri ->
                             try {
                                 val inputStream = requireContext().contentResolver.openInputStream(uri)
                                 val tempFile = File.createTempFile("avatar_", ".jpg", requireContext().cacheDir)
                                 tempFile.outputStream().use { output ->
                                     inputStream?.copyTo(output)
                                 }
                                 val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
                                 MultipartBody.Part.createFormData("avatar", tempFile.name, requestBody)
                             } catch (e: Exception) {
                                 e.printStackTrace()
                                 null
                             }
                         }

                         // ✅ Вызываем API с multipart-параметрами
                         val response = RetrofitClient.apiService.updateUser(
                             id = idBody,
                             oldPassword = oldPasswordBody,
                             newPassword = newPasswordBody,
                             fullname = fullnameBody,
                             phoneNumber = phoneNumberBody,
                             userInfo = userInfoBody,
                             isActive = isActiveBody,
                             avatar = avatarPart
                         )

                         if (response.isSuccessful) {
                             Toast.makeText(requireContext(), "✅ Изменения сохранены", Toast.LENGTH_SHORT).show()
                             // ✅ Обновляем oldUser
                             val updatedResponse = RetrofitClient.apiService.getUserByID(oldUser!!.id)
                             if (updatedResponse.isSuccessful) {
                                 oldUser = updatedResponse.body()
                             }
                         } else {
                             val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                             println("❌ ОШИБКА: ${response.code()} - $error")
                             Toast.makeText(requireContext(), "❌ Ошибка: $error", Toast.LENGTH_LONG).show()
                         }

                     } catch (e: Exception) {
                         e.printStackTrace()
                         Toast.makeText(requireContext(), "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                     }
                 }
             } else { // Включение режима редактирования
                 isEditMode = true
                 binding.buttonEdit.text = "Сохранить изменения"
                 binding.buttonLogOut.text = "Отмена"
                 binding.imageView4.setOnClickListener { selectImage() }
             }
             changeOfAccess(isEditMode)
         }
    }

    fun changeOfAccess(isEditMode: Boolean){
        //разрешено редактирование на момент нажатия
        binding.editTextTextName.apply {
            isFocusable = isEditMode
            isFocusableInTouchMode = isEditMode
            isClickable = isEditMode
            isCursorVisible = isEditMode
        }
        binding.editTextTextPhone.apply {
            isFocusable = isEditMode
            isFocusableInTouchMode = isEditMode
            isClickable = isEditMode
            isCursorVisible = isEditMode
        }
        binding.editTextTextInf.apply {
            isFocusable = isEditMode
            isFocusableInTouchMode = isEditMode
            isClickable = isEditMode
            isCursorVisible = isEditMode
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun loadUserProfile() {
        if (nowUserID.isNullOrEmpty()) {
            Toast.makeText(requireContext(),"⚠️user_id не найден в SharedPreferences", Toast.LENGTH_LONG).show()
            return
        }
        else { loadUserInf() }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

     fun loadUserInf() {
         viewLifecycleOwner.lifecycleScope.launch {
             try {
                 val response = RetrofitClient.apiService.getUserByID(nowUserID.toString())
                 if (response.isSuccessful && response.body() != null) {
                     val user = response.body()!!
                     // Обновляем UI
                     binding.editTextTextName.setText(user.fullname)
                     binding.editTextTextPhone.setText(user.phoneNumber)
                     binding.editTextTextRole.setText(role.toString())

                     if (user.avatarUrl.isNullOrEmpty()) {
                         // Показываем плейсхолдер
                         Glide.with(requireContext())
                             .load(android.R.drawable.ic_menu_gallery)  // или android.R.drawable.ic_menu_gallery
                             .centerCrop()
                             .into(binding.imageView4)
                     } else {
                         // Загружаем по URL
                         Glide.with(requireContext())
                             .load(user.avatarUrl)
                             .placeholder(android.R.drawable.ic_menu_gallery)
                             .error(android.R.drawable.stat_notify_error)
                             .centerCrop()
                             .into(binding.imageView4)
                     }

                     if (user.userInfo != null) { binding.editTextTextInf.setText(user.userInfo) }
                     oldUser = user

                 } else {
                     val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                     Toast.makeText(requireContext(), "Ошибка: ${error}", Toast.LENGTH_SHORT).show() }
             } catch (e: Exception) {
                 e.printStackTrace()
                 Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
             }

             if(role == "Admin") { loadAllUser() }
             else { binding.recyclerViewUsers.visibility = View.GONE}
         }
    }

    fun loadAllUser(){
        viewLifecycleOwner.lifecycleScope.launch {
            val result =authRepository.getALLUsers()
            result.onSuccess {response ->
                val users= response.users
                if(users.isEmpty()) { Toast.makeText(requireContext(),"пользователей нет", Toast.LENGTH_SHORT).show() }
                else
                {
                    binding.recyclerViewUsers.apply {
                        visibility = View.VISIBLE
                        layoutManager = LinearLayoutManager(requireContext())
                        setHasFixedSize(true)
                        adapter = UserAdapter(users) {user -> OnUserClick(user) }
                    }
                }
            }
            result.onFailure {
                super.onDestroyView()
                _binding= null
            }
        }
    }

    private fun OnUserClick(user: UserDto) {
        val prefs = requireContext().getSharedPreferences("user_prefs", 0)
        prefs.edit().apply(){putString("selected_user_id",user.id);apply()}

        //переход
        val intent = Intent(requireContext(), user_detail_Activity::class.java)
        startActivity(intent)
    }

    private fun selectImage() { imagePickerLauncher.launch("image/*") }





}