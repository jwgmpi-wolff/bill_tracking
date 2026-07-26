package com.medbill.tracker.vm

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medbill.tracker.auth.AuthManager
import com.medbill.tracker.auth.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isAuthenticated: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val authManager: AuthManager) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val result = authManager.signIn(activity)
                SessionStore.setToken(result.accessToken)
                _state.value = AuthState(isAuthenticated = true, loading = false)
            }.onFailure {
                _state.value = AuthState(isAuthenticated = false, loading = false, error = it.message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching {
                authManager.signOut()
                SessionStore.clear()
                _state.value = AuthState(isAuthenticated = false, loading = false)
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.message)
            }
        }
    }
}
