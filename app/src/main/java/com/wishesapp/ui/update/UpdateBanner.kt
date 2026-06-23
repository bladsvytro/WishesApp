package com.wishesapp.ui.update

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val checker: UpdateChecker,
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    init {
        viewModelScope.launch {
            delay(3_000)
            val info = checker.checkForUpdate()
            if (info?.isUpdateAvailable == true) {
                _updateInfo.value = info
            }
        }
    }

    fun downloadUpdate(url: String) { checker.openDownloadUrl(url) }
    fun dismiss() { _updateInfo.value = null }
}

@Composable
fun UpdateBanner(viewModel: UpdateViewModel = hiltViewModel()) {
    val info by viewModel.updateInfo.collectAsState()

    AnimatedVisibility(
        visible = info != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        info?.let { update ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Доступно обновление ${update.latestVersionName}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.downloadUpdate(update.downloadUrl) }) {
                        Text("Скачать")
                    }
                    TextButton(onClick = { viewModel.dismiss() }) {
                        Text("Позже")
                    }
                }
            }
        }
    }
}
