package com.wishesapp.ui.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishesapp.data.model.Space
import com.wishesapp.data.repository.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SpaceUiState {
    data object Idle : SpaceUiState
    data object Loading : SpaceUiState
    data class Success(val space: Space) : SpaceUiState
    data class Error(val message: String) : SpaceUiState
}

sealed interface LeaveSpaceState {
    data object Idle : LeaveSpaceState
    data object Loading : LeaveSpaceState
    data object Success : LeaveSpaceState
    data class Error(val message: String) : LeaveSpaceState
}

@HiltViewModel
class SpaceViewModel @Inject constructor(
    private val repo: SpaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SpaceUiState>(SpaceUiState.Idle)
    val uiState: StateFlow<SpaceUiState> = _uiState

    private val _mySpace = MutableStateFlow<Space?>(null)
    val mySpace: StateFlow<Space?> = _mySpace

    private val _leaveState = MutableStateFlow<LeaveSpaceState>(LeaveSpaceState.Idle)
    val leaveState: StateFlow<LeaveSpaceState> = _leaveState

    init {
        viewModelScope.launch {
            repo.observeMySpace().collect { _mySpace.value = it }
        }
    }

    fun createSpace(displayName: String) {
        _uiState.value = SpaceUiState.Loading
        viewModelScope.launch {
            try {
                val space = repo.createSpace(displayName)
                _uiState.value = SpaceUiState.Success(space)
            } catch (e: Exception) {
                _uiState.value = SpaceUiState.Error(e.message ?: "Failed to create space")
            }
        }
    }

    fun joinSpace(code: String, displayName: String) {
        _uiState.value = SpaceUiState.Loading
        viewModelScope.launch {
            try {
                val space = repo.joinSpace(code, displayName)
                _uiState.value = SpaceUiState.Success(space)
            } catch (e: Exception) {
                _uiState.value = SpaceUiState.Error(e.message ?: "Failed to join space")
            }
        }
    }

    fun leaveSpace() {
        _leaveState.value = LeaveSpaceState.Loading
        viewModelScope.launch {
            try {
                repo.leaveSpace()
                _leaveState.value = LeaveSpaceState.Success
            } catch (e: Exception) {
                _leaveState.value = LeaveSpaceState.Error(e.message ?: "Failed to leave space")
            }
        }
    }

    fun resetState() { _uiState.value = SpaceUiState.Idle }
    fun resetLeaveState() { _leaveState.value = LeaveSpaceState.Idle }
}
