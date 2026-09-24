package com.petbreath.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.petbreath.app.AppContainer
import com.petbreath.app.PetBreathApp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY

/** Builds a ViewModel factory with access to the [AppContainer] and saved state. */
inline fun <reified VM : ViewModel> appViewModelFactory(
    crossinline create: CreationExtras.(container: AppContainer, handle: SavedStateHandle) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as PetBreathApp
        create(app.container, createSavedStateHandle())
    }
}
