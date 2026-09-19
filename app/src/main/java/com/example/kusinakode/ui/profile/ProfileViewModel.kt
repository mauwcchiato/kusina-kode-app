package com.example.kusinakode.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.Session
import com.example.kusinakode.data.repository.RemoteProfileRepository
import com.example.kusinakode.domain.model.ProfileStats
import com.example.kusinakode.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val stats: ProfileStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val editError: String? = null,
    val editUsername: String = "",
    val editNickname: String = ""
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: Int) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            profileRepository.profileStats(userId)
                .onSuccess { stats -> _uiState.update { it.copy(isLoading = false, stats = stats, error = null) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun startEdit() {
        _uiState.update { s ->
            s.copy(
                isEditing = true,
                editError = null,
                editUsername = Session.displayName ?: s.stats?.name ?: "",
                editNickname = Session.nickname ?: s.stats?.nickname ?: ""
            )
        }
    }

    fun cancelEdit() = _uiState.update { it.copy(isEditing = false, editError = null) }
    fun onEditUsernameChange(v: String) = _uiState.update { it.copy(editUsername = v, editError = null) }
    fun onEditNicknameChange(v: String) = _uiState.update { it.copy(editNickname = v, editError = null) }

    fun saveProfile(userId: Int) {
        val s = _uiState.value
        val err = validateUsername(s.editUsername)
        if (err != null) {
            _uiState.update { it.copy(editError = err) }
            return
        }
        _uiState.update { it.copy(isSaving = true, editError = null) }
        viewModelScope.launch {
            profileRepository.updateProfile(userId, s.editUsername.trim(), s.editNickname.trim().ifBlank { null })
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, isEditing = false) }
                    loadProfile(userId)
                }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, editError = e.message) } }
        }
    }

    private fun validateUsername(u: String): String? {
        val s = u.trim()
        if (s.isEmpty()) return "Username is required"
        if (s.length < 3) return "Username must be at least 3 characters"
        if (!s.all { it.isLetterOrDigit() || it == '_' }) return "Username may only contain letters, numbers, and _"
        return null
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(RemoteProfileRepository(context)) as T
        }
    }
}
