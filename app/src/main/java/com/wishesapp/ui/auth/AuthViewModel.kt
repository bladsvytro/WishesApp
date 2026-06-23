package com.wishesapp.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishesapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class LinkSent(val email: String) : AuthUiState
    data object SignedIn : AuthUiState
    data class Error(val message: String) : AuthUiState
}

private const val PREFS_NAME = "auth"
private const val KEY_PENDING_EMAIL = "pending_email"
private const val KEY_PENDING_LINK = "pending_email_link"

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: AuthRepository,
) : ViewModel() {

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        if (repo.currentUser != null) {
            _uiState.value = AuthUiState.SignedIn
        } else {
            // Cold-start case: link was saved to prefs before this ViewModel was created.
            tryCompletePendingSignIn()
            // onNewIntent case: MainActivity increments the trigger after saving a new link.
            // drop(1) skips the value that was already set before collection started.
            viewModelScope.launch {
                repo.signInCheckTrigger.drop(1).collect { tryCompletePendingSignIn() }
            }
        }
    }

    private fun tryCompletePendingSignIn() {
        val link = prefs.getString(KEY_PENDING_LINK, null) ?: return
        if (!repo.isSignInLink(link)) return
        val email = prefs.getString(KEY_PENDING_EMAIL, null) ?: run {
            // Link arrived on a different device — no stored email.
            _uiState.value = AuthUiState.Error("Введите email для завершения входа на этом устройстве")
            return
        }
        completeSignIn(email, link)
    }

    fun sendSignInLink(email: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                repo.sendSignInLink(email)
                // Persist email so it survives process death
                prefs.edit().putString(KEY_PENDING_EMAIL, email).apply()
                _uiState.value = AuthUiState.LinkSent(email)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Не удалось отправить ссылку")
            }
        }
    }

    fun completeSignIn(email: String, emailLink: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repo.signInWithEmailLink(email, emailLink).fold(
                onSuccess = {
                    prefs.edit()
                        .remove(KEY_PENDING_EMAIL)
                        .remove(KEY_PENDING_LINK)
                        .apply()
                    _uiState.value = AuthUiState.SignedIn
                },
                onFailure = {
                    _uiState.value = AuthUiState.Error(it.message ?: "Ошибка входа")
                }
            )
        }
    }

    fun resetState() { _uiState.value = AuthUiState.Idle }
}
