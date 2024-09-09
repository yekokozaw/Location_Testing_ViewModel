package com.ykz.locationtesting.ui.theme.viewmodel

sealed class UiState {
    object Loading : UiState()
    data class Success(val latitude: Double, val longitude: Double) : UiState()
    data class Error(val message: String) : UiState()
}