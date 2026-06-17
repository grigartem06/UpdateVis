package com.example.upk_btpi

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.collection.emptyLongSet
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.upk_btpi.Adapters.UpkAdapter
import com.example.upk_btpi.Models.Ypk.YpksDto
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.databinding.FragmentUpkBinding
import kotlinx.coroutines.launch

class UpkFragment : Fragment() {
  private var _binding: FragmentUpkBinding?= null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()
    private var upkAdapter: UpkAdapter?=null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentUpkBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewUpk.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
        val authPrefs =requireContext().getSharedPreferences("auth_prefs", MODE_PRIVATE)
        var userRole = authPrefs.getString("user_role", null)

        if(userRole != "Admin") {_binding!!.floatingActionButton.visibility = View.GONE}

        // ✅ 4️⃣ Загрузка данных
        loadListOfUpk()

        binding.floatingActionButton.setOnClickListener { newUpk() }
        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }
    }

    private fun refresh() {
        binding.swipeRefreshLayout.isRefreshing = true
        // Перезагружаем данные
        loadListOfUpk()
        // Останавливаем анимацию после загрузки
        // Важно: делаем это в конце loadProducts() или здесь с задержкой
        binding.swipeRefreshLayout.isRefreshing = false
    }

    fun loadListOfUpk() {
        lifecycleScope.launch {
            val result = authRepository.getAllUpks()
            result.onSuccess {
                response -> val upks = response.ypks
                if(upks.isEmpty()) {Toast.makeText(requireContext(),"упк нет", Toast.LENGTH_SHORT).show()}
                else {
                    binding.recyclerViewUpk.apply { layoutManager = LinearLayoutManager(requireContext())
                    setHasFixedSize(true)
                    adapter = UpkAdapter(upks) {upk -> OnUpkClick(upk)}
                    }
                }
            }
            result.onFailure { super.onDestroyView(); _binding = null }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun OnUpkClick(ypk: YpksDto) {
        //сохраняем id упк
        var prefs = requireContext().getSharedPreferences("ypk_prefs",0)
        prefs.edit().apply(){putString("selected_ypk_id",ypk.id);apply()}

        //переход
        val intent = Intent(requireContext(), ypk_detail_Activity::class.java)
        startActivity(intent)
    }

    private fun newUpk(){
        //сохраняем id упк
        var prefs = requireContext().getSharedPreferences("ypk_prefs",0)
        prefs.edit().apply(){putString("selected_ypk_id","new_ypk");apply()}

        //переход
        val intent = Intent(requireContext(), ypk_detail_Activity::class.java)
        startActivity(intent)
    }

}