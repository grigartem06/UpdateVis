package com.example.upk_btpi

import UserDto
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.upk_btpi.Adapters.UserAdapter
import com.example.upk_btpi.Retrofit.AuthInterceptor
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.Utils.ErrorHandler
import com.example.upk_btpi.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ProfileFragment : Fragment() {

    // ✅ ViewBinding для безопасной работы с UI
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // ✅ Репозиторий для работы с API
    private val authRepository = AuthRepository()

    // ✅ Флаги и данные пользователя
    private var isEditMode = false
    private var oldUser: UserDto? = null
    private var nowUserID: String? = null
    private var role: String? = null

    // ✅ Переменная для хранения URI выбранного изображения
    private var selectedImageUri: Uri? = null
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.imageView4.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Получаем базовые данные из SharedPreferences
        val prefs = requireContext().getSharedPreferences("auth_prefs", 0)
        nowUserID = prefs.getString("user_id", null)
        role = prefs.getString("user_role", null)

        println("📦 ProfileFragment: user_id = $nowUserID, role = $role")

        // 🔹 Загружаем данные профиля
        loadUserProfile()

        // 🔹 Обработчик кнопки "Выход" / "Отмена"
        binding.buttonLogOut.setOnClickListener {
            if (!isEditMode) {
                showLogoutConfirmationDialog()
            } else {
                cancelEditMode()
            }
        }

        // 🔹 Обработчик кнопки "Изменить" / "Сохранить"
        binding.buttonEdit.setOnClickListener {
            if (isEditMode) { saveChanges() }
            else { enableEditMode() }
        }
    }

    /**
     * Загружает данные профиля пользователя
     */
    private fun loadUserProfile() {
        // 🔹 Если user_id есть — загружаем профиль как обычно
        if (!nowUserID.isNullOrEmpty()) {
            println("✅ Загрузка профиля для user_id: $nowUserID")
            loadUserInf()
            return
        }
        // 🔹 Если user_id НЕТ, но есть токен — загружаем данные из API
        println("⚠️ user_id не найден, пробуем загрузить из API...")
        loadUserDataFromApi()
    }

    /**
     * Загружает данные пользователя из API и сохраняет их в SharedPreferences
     */
    private fun loadUserDataFromApi() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar?.visibility = View.VISIBLE

                // 🔹 Загружаем данные через /api/auth/me/all
                val result = authRepository.getFullCurrentUser()

                result.onSuccess { user ->
                    println("✅ Данные пользователя получены из API: ${user.fullName}")

                    // ✅ Сохраняем данные в SharedPreferences
                    saveUserDataToPrefs(user)

                    // ✅ Загружаем профиль
                    loadUserInf()
                }

                result.onFailure { error ->
                    println("❌ Ошибка загрузки данных: ${error.message}")
                    ErrorHandler.showDialog(
                        context = requireContext(),
                        title = "Ошибка",
                        message = "Не удалось загрузить данные профиля. Пожалуйста, войдите заново.",
                        onPositive = {
                            val intent = Intent(requireContext(), Entry::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    )
                }

            } catch (e: Exception) {
                ErrorHandler.showErrorWithMessage(requireContext(), e)
            } finally {
                binding.progressBar?.visibility = View.GONE
            }
        }
    }

    /**
     * Сохраняет данные пользователя в SharedPreferences
     */
    private fun saveUserDataToPrefs(user: UserDto) {
        val prefs = requireContext().getSharedPreferences("auth_prefs", 0)
        prefs.edit().apply {
            putString("user_id", user.id)
            putString("user_name", user.fullName ?: "Пользователь")
            putString("user_phone", user.phoneNumber ?: "")
            putString("user_role", role ?: "DefaultUser")  // Роль берём из SharedPreferences
            putString("user_info", user.userInfo ?: "")
            putString("user_avatar", user.avatarUrl ?: "")
            apply()
        }
        println("📦 Данные пользователя сохранены в SharedPreferences")
    }

    /**
     * Загружает данные пользователя из API и отображает их в UI
     */
    private fun loadUserInf() {
        binding.progressBar?.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 🔹 Загружаем свежие данные из API через /api/auth/me/all
                val result = authRepository.getFullCurrentUser()

                result.onSuccess { user ->
                    println("✅ Данные пользователя получены: ${user.fullName ?: user.id}")

                    // 🔹 Заполняем поля данными пользователя
                    // ⚠️ API возвращает "name", а не "fullName" — используем fallback
                    binding.editTextTextName.setText(user.fullName ?: "Пользователь")
                    binding.editTextTextPhone.setText(user.phoneNumber ?: "")
                    binding.editTextTextRole.setText(role ?: "Пользователь")  // Роль из SharedPreferences

                    // 🔹 Загружаем аватар через Glide
                    if (user.avatarUrl.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(android.R.drawable.ic_menu_gallery)
                            .centerCrop()
                            .into(binding.imageView4)
                    } else {
                        Glide.with(requireContext())
                            .load(user.avatarUrl)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.stat_notify_error)
                            .centerCrop()
                            .into(binding.imageView4)
                    }

                    if (!user.userInfo.isNullOrEmpty()) {
                        binding.editTextTextInf.setText(user.userInfo)
                    }

                    // ✅ Сохраняем локальную копию пользователя
                    oldUser = user

                    // ✅ Скрываем индикатор загрузки
                    binding.progressBar?.visibility = View.GONE
                }

                result.onFailure { error ->
                    println("❌ Ошибка загрузки профиля: ${error.message}")
                    ErrorHandler.showDialog(
                        context = requireContext(),
                        title = "Ошибка загрузки",
                        message = error.message ?: "Не удалось загрузить профиль",
                    )
                    binding.progressBar?.visibility = View.GONE
                }

            } catch (e: Exception) {
                println("❌ Исключение: ${e.message}")
                ErrorHandler.showErrorWithMessage(requireContext(), e)
                binding.progressBar?.visibility = View.GONE
            }

            // 🔹 Для Admin: загружаем список всех пользователей
            if (role == "Admin" || role == "Administrator") {
                binding.textView7.visibility = View.VISIBLE
                loadAllUsers()
            } else {
                binding.textView7.visibility = View.GONE
                binding.recyclerViewUsers.visibility = View.GONE
            }
        }
    }

    /**
     * Включает режим редактирования
     */
    private fun enableEditMode() {
        isEditMode = true
        binding.buttonEdit.text = "Сохранить изменения"
        binding.buttonLogOut.text = "Отмена"
        binding.imageView4.setOnClickListener { selectImage() }
        changeOfAccess(isEditMode = true)

        Toast.makeText(requireContext(), "Режим редактирования включён", Toast.LENGTH_SHORT).show()
    }

    /**
     * Отключает режим редактирования
     */
    private fun cancelEditMode() {
        isEditMode = false
        binding.buttonEdit.text = "Изменить"
        binding.buttonLogOut.text = "Выйти"
        binding.imageView4.setOnClickListener(null)
        changeOfAccess(isEditMode = false)

        // Возвращаем старые значения
        oldUser?.let { user ->
            binding.editTextTextName.setText(user.fullName)
            binding.editTextTextPhone.setText(user.phoneNumber)
            binding.editTextTextInf.setText(user.userInfo)
        }

        Toast.makeText(requireContext(), "Изменения отменены", Toast.LENGTH_SHORT).show()
    }

    /**
     * Сохраняет изменения профиля пользователя через API
     */
    private fun saveChanges() {
        val fullname = binding.editTextTextName.text.toString().trim()
        val phoneNumber = binding.editTextTextPhone.text.toString().trim()
        val userInfo = binding.editTextTextInf.text.toString().trim()

        if (fullname.isEmpty()) {
            binding.editTextTextName.error = "Введите ФИО"
            return
        }
        if (phoneNumber.isEmpty()) {
            binding.editTextTextPhone.error = "Введите номер телефона"
            return
        }

        isEditMode = false
        binding.buttonEdit.text = "Изменить"
        binding.buttonLogOut.text = "Выйти"
        binding.imageView4.setOnClickListener(null)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val fullnameBody = fullname.toRequestBody("text/plain".toMediaType())
                val phoneNumberBody = phoneNumber.toRequestBody("text/plain".toMediaType())
                val userInfoBody = userInfo.toRequestBody("text/plain".toMediaType())

                val avatarPart = selectedImageUri?.let { uri ->
                    try {
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        val tempFile = File.createTempFile("avatar_", ".jpg", requireContext().cacheDir)
                        tempFile.outputStream().use { output -> inputStream?.copyTo(output) }
                        val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
                        MultipartBody.Part.createFormData("avatar", tempFile.name, requestBody)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                val response = authRepository.updateCurrentUser(
                    fullname = fullname,
                    phoneNumber = phoneNumber,
                    userInfo = userInfo,
                    avatarUri = selectedImageUri,
                    context = requireContext()
                )

                if (response.isSuccess) {
                    ErrorHandler.showDialog(
                        context = requireContext(),
                        title = "Успех",
                        message = "✅ Изменения сохранены",
                        onPositive = {
                            loadUserInf()
                        }
                    )
                    selectedImageUri = null
                } else {
                    val error = response.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    ErrorHandler.showDialog(
                        context = requireContext(),
                        title = "Ошибка сохранения",
                        message = error,
                    )
                }
            } catch (e: Exception) {
                ErrorHandler.showErrorWithMessage(requireContext(), e)
            }
        }
    }

    /**
     * Включает/отключает возможность редактирования полей ввода
     */
    private fun changeOfAccess(isEditMode: Boolean) {
        binding.editTextTextName.apply {
            isFocusable = isEditMode
            isFocusableInTouchMode = isEditMode
            isClickable = isEditMode
            isCursorVisible = isEditMode
            isEnabled = isEditMode
        }
        binding.editTextTextPhone.apply {
            isFocusable = isEditMode
            isFocusableInTouchMode = isEditMode
            isClickable = isEditMode
            isCursorVisible = isEditMode
            isEnabled = isEditMode
        }
        binding.editTextTextInf.apply {
            isFocusable = isEditMode
            isFocusableInTouchMode = isEditMode
            isClickable = isEditMode
            isCursorVisible = isEditMode
            isEnabled = isEditMode
        }
    }

    /**
     * Загружает список всех пользователей (только для Admin)
     */
    private fun loadAllUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = authRepository.getAllUsers()

            result.onSuccess { response ->
                val users = response.users
                if (users.isEmpty()) {
                    Toast.makeText(requireContext(), "Пользователей пока нет", Toast.LENGTH_SHORT).show()
                } else {
                    binding.recyclerViewUsers.apply {
                        visibility = View.VISIBLE
                        layoutManager = LinearLayoutManager(requireContext())
                        setHasFixedSize(true)
                        adapter = UserAdapter(users) { user -> onUserClick(user) }
                    }
                }
            }

            result.onFailure { error ->
                ErrorHandler.showDialog(
                    context = requireContext(),
                    title = "Ошибка загрузки",
                    message = error.message ?: "Не удалось загрузить список пользователей",
                )
            }
        }
    }

    /**
     * Обработчик клика по пользователю: переход на экран деталей
     */
    private fun onUserClick(user: UserDto) {
        val prefs = requireContext().getSharedPreferences("user_prefs", 0)
        prefs.edit().apply {
            putString("selected_user_id", user.id)
            apply()
        }
        val intent = Intent(requireContext(), user_detail_Activity::class.java)
        startActivity(intent)
    }

    /**
     * Запускает выбор изображения аватара
     */
    private fun selectImage() {
        imagePickerLauncher.launch("image/*")
    }

    /**
     * Показывает диалог подтверждения выхода
     */
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти из аккаунта?")
            .setPositiveButton("Выйти") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Выполняет выход из аккаунта
     */
    private fun performLogout() {
        AuthInterceptor.clearTokens()

        val prefs = requireContext().getSharedPreferences("auth_prefs", 0)
        prefs.edit().clear().apply()

        val intent = Intent(requireContext(), Entry::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}