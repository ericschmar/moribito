package com.moribito.gui.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds and manages the application state.
 * Shared between different ViewModels to ensure consistency.
 */
class AppStateHolder {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    /**
     * Updates the application state.
     */
    fun update(function: (AppState) -> AppState) {
        _state.update(function)
    }

    /**
     * Gets the current application state.
     */
    val value: AppState get() = _state.value
}
