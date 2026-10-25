package com.example.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.model.HabitDatabase
import com.example.model.HabitRepository
import com.example.model.OriginalHabitData
import com.example.model.RoomHabitRepository
import com.example.viewmodel.MainViewModel

/** Punto de composición de dependencias: el ViewModel solo recibe el contrato del repositorio. */
class AppContainer(context: Context) {
    private val repository: HabitRepository = RoomHabitRepository(
        HabitDatabase.get(context.applicationContext), originalData = OriginalHabitData
    )

    val mainViewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == MainViewModel::class.java) { "ViewModel no registrado: $modelClass" }
            return MainViewModel(repository) as T
        }
    }
}
