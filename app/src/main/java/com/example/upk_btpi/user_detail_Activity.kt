package com.example.upk_btpi

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animateDecay
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.upk_btpi.Models.Role.RoleDto
import com.example.upk_btpi.Models.User.UpdateUserDto
import com.example.upk_btpi.Models.User.UpdateUserForAdminDto
import com.example.upk_btpi.Models.User.UserDto
import com.example.upk_btpi.Models.Ypk.YpksDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.databinding.ActivityUserDetailBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class user_detail_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityUserDetailBinding
    private lateinit var oldUser: UserDto
    private  var selectedUserId: String?=null
    private val authRepository = AuthRepository()
    private var EditMode: Boolean = false
    private var listOfRoles:List<RoleDto> = emptyList()
    private var listOfYpk:  List<YpksDto> = emptyList()
    private var selectedRole: String?=null
    private var selectedYpk: String?=null
    private var selectedImageUri: Uri?=null
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent())
    { uri: Uri ?-> uri?.let { selectedImageUri = it;  binding.imageView3.setImageURI(it) } }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //получение данных из SharedPreferences
        val userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        selectedUserId = userPrefs.getString("selected_user_id", null)

        //проверка на получение данных
        if(selectedUserId.isNullOrEmpty()) {finish();return}

        getInfAboutUser(selectedUserId.toString())

        binding.buttonBack.setOnClickListener { back()}
        binding.buttonEdit.setOnClickListener { edit()}
        binding.buttonSave.setOnClickListener { save()}
        binding.buttonDelete.setOnClickListener { delete() }
        binding.imageView3.setOnClickListener{selectImage()}

    }

    private fun getInfAboutUser(userId: String) {
        //получаем данные из апи
        lifecycleScope.launch {
            var result = authRepository.getUserByID(userId)
            result.onSuccess {user->oldUser= user; displayUserInf(user)  }
            result.onFailure { Toast.makeText(this@user_detail_Activity, "❌ Ошибка получения данных", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun displayUserInf(user: UserDto) {
        if(oldUser.role.roleName != "Admin") {
            binding.buttonDelete.visibility = View.VISIBLE
        }

        // ✅ Загружаем аватар, если есть URL
        if (!user.avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.avatarUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(binding.imageView3)
        } else { }

        if (EditMode) {
            // ✅ В режиме редактирования заполняем EditText
            binding.editTextTextName.setText(user.fullname ?: "")
            binding.editTextPhone.setText(user.phoneNumber ?: "")
            binding.editTextTextInfo.setText(user.userInfo ?: "")
        } else {
            // ✅ В режиме просмотра показываем TextView
            binding.textViewName.text = user.fullname ?: "Без имени"
            binding.textViewPhoneNumber.text = user.phoneNumber ?: "Нет телефона"
            binding.textViewInfo.text = user.userInfo ?: "Нет информации"
            binding.textViewIsActive.text = if (user.isActive) "✅ Активный" else "❌ Не активен"

            // ✅ ИСПРАВЛЕНО: безопасный доступ к nullable ypk
            binding.textViewYpk.text = user.ypk?.ypkName ?: "Не назначен"
            // ❌ УДАЛИТЕ эту строку — она дублирует логику и вызывает краш:
            // if(user.ypk.id == null) {binding.textViewYpk.text= "Не назначен"}
            // else{binding.textViewYpk.text = user.ypk?.ypkName }

            // ✅ ИСПРАВЛЕНО: безопасный доступ к nullable role
            binding.textViewRole.text = user.role?.roleName ?: "Не назначена"
        }
    }

    private fun edit() {
        EditMode= true
        // отображаем поля ввода
        binding.editTextTextName.visibility = View.VISIBLE
        binding.editTextPhone.visibility = View.VISIBLE
        binding.editTextTextInfo.visibility = View.VISIBLE
        binding.spinnerYpk.visibility= View.VISIBLE
        binding.spinnerRole.visibility= View.VISIBLE

        loadSpiners()

        //скрываем поля вывода
        binding.textViewName.visibility = View.GONE
        binding.textViewPhoneNumber.visibility = View.GONE
        binding.textViewInfo.visibility = View.GONE
        binding.textViewYpk.visibility = View.GONE
        binding.textViewRole.visibility = View.GONE

        //выводим данные
        displayUserInf(oldUser)

        //скрываем кнопки
        binding.buttonEdit.visibility = View.GONE
        //раскрываем кнопки
        binding.buttonSave.visibility = View.VISIBLE

        binding.buttonBack.text = "отменить"
    }

    private fun save() {
        lifecycleScope.launch {
            try {
                val fullname = binding.editTextTextName.text.toString().trim()
                val phoneNumber = binding.editTextPhone.text.toString().trim()
                val userInfo = binding.editTextTextInfo.text.toString().trim()

                if (fullname.isEmpty()) {
                    Toast.makeText(this@user_detail_Activity, "⚠️ Введите имя", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (phoneNumber.isEmpty()) {
                    Toast.makeText(this@user_detail_Activity, "⚠️ Введите телефон", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ Проверяем доступ к УПК
                val currentRole = listOfRoles.find { it.id == selectedRole }
                val roleName = currentRole?.roleName?.lowercase()?.trim()
                val rolesWithYpkAccess = setOf("менеджер", "manager", "исполнитель", "executor")
                val hasYpkAccess = roleName in rolesWithYpkAccess

                // ✅ Получаем ID УПК
                val ypkIdValue = if (hasYpkAccess) {
                    val selectedPosition = binding.spinnerYpk.selectedItemPosition
                    if (selectedPosition >= 0 && selectedPosition < listOfYpk.size) {
                        listOfYpk[selectedPosition].id
                    } else {
                        oldUser.ypk?.id
                    }
                } else {
                    null
                }

                // ✅ Создаем RequestBody для каждого поля
                val idBody = oldUser.id.toRequestBody("text/plain".toMediaType())
                val fullnameBody = fullname.toRequestBody("text/plain".toMediaType())
                val phoneNumberBody = phoneNumber.toRequestBody("text/plain".toMediaType())
                val roleIdBody = (selectedRole ?: oldUser.role?.id ?: "").toRequestBody("text/plain".toMediaType())

                // ✅ ИСПРАВЛЕНО: userInfoBody всегда будет RequestBody
                val userInfoBody = (if (userInfo.isEmpty()) oldUser.userInfo ?: "" else userInfo)
                    .toRequestBody("text/plain".toMediaType())

                val ypkIdBody = (ypkIdValue ?: "").toRequestBody("text/plain".toMediaType())

                // ✅ Создаем MultipartBody.Part для аватара
                val avatarPart = selectedImageUri?.let { uri ->
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val tempFile = File.createTempFile("avatar_", ".jpg", cacheDir)
                        tempFile.outputStream().use { output ->
                            inputStream?.copyTo(output)
                        }
                        val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
                        MultipartBody.Part.createFormData("Avatar", tempFile.name, requestBody)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        println("❌ Ошибка создания avatar part: ${e.message}")
                        null
                    }
                }

                println("🔍 save(): role='$roleName', ypkId=$ypkIdValue, есть фото: ${avatarPart != null}")
                println("📤 Отправка multipart данных...")

                // ✅ Вызываем API (без !!)
                val response = RetrofitClient.apiService.updateUserForAdmin(
                    id = idBody,
                    fullname = fullnameBody,
                    phoneNumber = phoneNumberBody,
                    roleId = roleIdBody,
                    userInfo = userInfoBody,  // ← Теперь без !!
                    ypkId = ypkIdBody,
                    avatar = avatarPart
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@user_detail_Activity, "✅ Изменения сохранены", Toast.LENGTH_SHORT).show()

                    val updatedResult = authRepository.getUserByID(oldUser.id)
                    updatedResult.onSuccess { updatedUser ->
                        oldUser = updatedUser
                    }
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    println("❌ ОШИБКА: ${response.code()} - $error")
                    Toast.makeText(this@user_detail_Activity, "❌ Ошибка: $error", Toast.LENGTH_LONG).show()
                    return@launch
                }

            } catch (e: Exception) {
                println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@user_detail_Activity, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // ✅ Возвращаем интерфейс в режим просмотра
            EditMode = false
            displayUserInf(oldUser)

            binding.editTextTextName.visibility = View.GONE
            binding.editTextPhone.visibility = View.GONE
            binding.editTextTextInfo.visibility = View.GONE
            binding.spinnerYpk.visibility = View.GONE
            binding.spinnerRole.visibility = View.GONE

            binding.textViewName.visibility = View.VISIBLE
            binding.textViewPhoneNumber.visibility = View.VISIBLE
            binding.textViewInfo.visibility = View.VISIBLE
            binding.textViewYpk.visibility = View.VISIBLE
            binding.textViewRole.visibility = View.VISIBLE

            binding.buttonEdit.visibility = View.VISIBLE
            binding.buttonSave.visibility = View.GONE
            binding.buttonBack.text = "← Назад"
        }
    }


    private fun back() {
        if(EditMode){EditMode= false; displayUserInf(oldUser)}
        else{finish()}
    }

    private fun loadSpiners(){
        lifecycleScope.launch {
            try {
                var responseYpk = RetrofitClient.apiService.getAllYpk()
                var responseRoles = RetrofitClient.apiService.getAllRoles()

                if(responseRoles.isSuccessful && responseRoles.body()!=null) {listOfRoles=responseRoles.body()!!.roles; setupRoleSpinner()}
                else{}

                if(responseYpk.isSuccessful && responseYpk.body()!=null) {listOfYpk=responseYpk.body()!!.ypks; setupYpkSpinner()}
                else{}

            }catch (e: Exception) {}
        }
    }

    private fun setupYpkSpinner() {
        if (listOfYpk.isEmpty()) {
            println("⚠️ listOfYpk пуст, пропускаем setupYpkSpinner")
            return
        }

        println("🔄 setupYpkSpinner: загружено ${listOfYpk.size} УПК")

        val ypkNames = listOfYpk.map { it.ypkName ?: "без названия" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ypkNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYpk.adapter = adapter

        // ✅ Устанавливаем дефолтное значение по ID (не по имени!)
        oldUser?.let { user ->
            val userYpkId = user.ypk?.id
            println("👤 УПК пользователя: $userYpkId")

            if (userYpkId != null) {
                val currentYpkIndex = listOfYpk.indexOfFirst { it.id == userYpkId }

                if (currentYpkIndex >= 0) {
                    binding.spinnerYpk.setSelection(currentYpkIndex)
                    selectedYpk = listOfYpk[currentYpkIndex].id  // ✅ Сохраняем ID!
                    println("✅ Установлен УПК: ${listOfYpk[currentYpkIndex].ypkName}")
                } else {
                    println("⚠️ УПК $userYpkId не найден в списке")
                }
            }
        }

        // ✅ Обработчик выбора
        binding.spinnerYpk.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYpk = listOfYpk[position].id  // ✅ Сохраняем ID при выборе
                println("🔄 Выбран УПК: ${listOfYpk[position].ypkName} (ID: $selectedYpk)")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedYpk = null
                println("⚠️ Ничего не выбрано в spinnerYpk")
            }
        }
    }


    private fun setupRoleSpinner() {
        if (listOfRoles.isEmpty()) return

        val roleNames = listOfRoles.map { it.roleName ?: "без названия" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roleNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRole.adapter = adapter

        // Устанавливаем текущую роль
        val currentUserRoleId = oldUser.role?.id
        if (currentUserRoleId != null) {
            val currentRoleIndex = listOfRoles.indexOfFirst { it.id == currentUserRoleId }
            if (currentRoleIndex >= 0) {
                binding.spinnerRole.setSelection(currentRoleIndex)
                selectedRole = listOfRoles[currentRoleIndex].id
                // ✅ Обновляем видимость УПК
                setupYpkVisibility(selectedRole)
            }
        }

        // Обработчик выбора
        binding.spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedRole = listOfRoles[position].id
                // ✅ Обновляем видимость УПК при смене роли
                setupYpkVisibility(selectedRole)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedRole = null
                hideYpkFields()
            }
        }
    }

    private fun setupYpkVisibility(roleId: String?) {
        if (roleId == null) {
            hideYpkFields()
            return
        }

        // Находим роль по ID в загруженном списке
        val currentRole = listOfRoles.find { it.id == roleId }
        val roleName = currentRole?.roleName?.lowercase()?.trim()

        // ✅ Проверяем по имени роли (не по ID!)
        val rolesWithYpkAccess = setOf("менеджер", "manager", "исполнитель", "executor")
        val hasYpkAccess = roleName in rolesWithYpkAccess

        println("🔍 setupYpkVisibility: role='$roleName', hasAccess=$hasYpkAccess")

        if (hasYpkAccess) { showYpkFields() }
        else { hideYpkFields() }
    }

    // Выносим логику отображения в отдельные методы
    private fun showYpkFields() {
        binding.spinnerYpk.visibility = View.VISIBLE
        binding.textViewYpk?.visibility = View.VISIBLE
    }

    private fun hideYpkFields() {
        binding.spinnerYpk.visibility = View.GONE
        binding.textViewYpk?.visibility = View.GONE
        selectedYpk = null  // Сбрасываем выбор
    }

    private fun delete() {
        AlertDialog.Builder(this)
            .setTitle("Удаление упк")
            .setMessage("Вы уверены, что хотите удалить этого пользователя? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ -> performDeleteUpk(selectedUserId!!) }
            .setNegativeButton("Отмена", null).show()
    }

    private fun performDeleteUpk(selectedUpkId: String) {
        lifecycleScope.launch {
            try {
                println("🗑️ Удаление пользователя: $selectedUpkId")
                val response = RetrofitClient.apiService.deleteUserByID(selectedUpkId)
                println("📥 Ответ сервера: ${response.code()}")
                if (response.isSuccessful) {
                    println("✅ пользователь удалён")
                    Toast.makeText(this@user_detail_Activity, "✅ пользователь удалён", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    println("❌ ОШИБКА: ${response.code()} - $error")
                    Toast.makeText(this@user_detail_Activity, "❌ Ошибка: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@user_detail_Activity, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectImage(){
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            { ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),100) }
            else { imagePickerLauncher.launch("image/*") }
        }
        else{imagePickerLauncher.launch("image/*")}
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode ==100){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            { imagePickerLauncher.launch("image/*") }
            else
            {Toast.makeText(this, "⚠️ Требуется разрешение для выбора фото", Toast.LENGTH_SHORT).show()}
        }
    }

}