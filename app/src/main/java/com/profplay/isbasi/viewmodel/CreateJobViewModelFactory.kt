package com.profplay.isbasi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.profplay.isbasi.data.repository.SupabaseRepository

class CreateJobViewModelFactory(
    private val repository: SupabaseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateJobViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateJobViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}