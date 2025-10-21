package com.profplay.isbasi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.profplay.isbasi.data.repository.SupabaseRepository

class HumansViewModelFactory(
    private val repository: SupabaseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HumansViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HumansViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
