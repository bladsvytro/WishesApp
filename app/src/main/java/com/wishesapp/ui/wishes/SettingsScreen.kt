package com.wishesapp.ui.wishes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wishesapp.data.model.WidgetMode
import com.wishesapp.ui.space.LeaveSpaceState
import com.wishesapp.ui.space.SpaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onSpaceLeft: () -> Unit,
    wishViewModel: WishViewModel = hiltViewModel(),
    spaceViewModel: SpaceViewModel = hiltViewModel(),
) {
    val widgetState by wishViewModel.widgetMode.collectAsState(initial = null)
    val mySpace by spaceViewModel.mySpace.collectAsState()
    val leaveState by spaceViewModel.leaveState.collectAsState()
    val currentUserId = wishViewModel.currentUserId
    val clipboard = LocalClipboardManager.current

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showLeaveSpaceDialog by remember { mutableStateOf(false) }
    var inviteCodeCopied by remember { mutableStateOf(false) }

    LaunchedEffect(leaveState) {
        if (leaveState is LeaveSpaceState.Success) {
            wishViewModel.onSpaceLeft()
            spaceViewModel.resetLeaveState()
            onSpaceLeft()
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Вы сможете войти снова по ссылке на email. Желания останутся в облаке.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    wishViewModel.signOut()
                    onSignedOut()
                }) { Text("Выйти") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Отмена") }
            },
        )
    }

    if (showLeaveSpaceDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveSpaceDialog = false },
            title = { Text("Покинуть пространство?") },
            text = { Text("Желания останутся в облаке. Вы сможете создать или вступить в новое пространство.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveSpaceDialog = false
                        spaceViewModel.leaveSpace()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Покинуть") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveSpaceDialog = false }) { Text("Отмена") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") } },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            mySpace?.let { space ->
                val partnerName = space.members.entries
                    .firstOrNull { it.key != currentUserId }?.value

                Text("Ваше пространство", style = MaterialTheme.typography.titleMedium)

                if (partnerName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SuggestionChip(onClick = {}, label = { Text("Вы + $partnerName") })
                    }
                } else {
                    Text(
                        "Ожидаем партнёра",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                "Код приглашения",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                space.inviteCode,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(space.inviteCode))
                            inviteCodeCopied = true
                        }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Скопировать код",
                                tint = if (inviteCodeCopied) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                HorizontalDivider()
            }

            Text("Виджет", style = MaterialTheme.typography.titleMedium)
            Text(
                "Выберите, что показывать на виджете",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WidgetModeButton(
                    label = "Последнее",
                    description = "Обновляется сразу, когда партнёр добавляет желание",
                    selected = widgetState?.mode == WidgetMode.LATEST,
                    onClick = { wishViewModel.setWidgetMode(WidgetMode.LATEST) },
                    modifier = Modifier.weight(1f),
                )
                WidgetModeButton(
                    label = "Случайное",
                    description = "Меняется каждые 30 минут + тап для смены",
                    selected = widgetState?.mode == WidgetMode.RANDOM,
                    onClick = { wishViewModel.setWidgetMode(WidgetMode.RANDOM) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.weight(1f))

            if (mySpace != null) {
                OutlinedButton(
                    onClick = { showLeaveSpaceDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = leaveState !is LeaveSpaceState.Loading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    if (leaveState is LeaveSpaceState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Покинуть пространство")
                    }
                }
                if (leaveState is LeaveSpaceState.Error) {
                    Text(
                        (leaveState as LeaveSpaceState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Выйти из аккаунта")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetModeButton(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = if (selected)
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    else
        CardDefaults.cardColors()

    Card(onClick = onClick, modifier = modifier, colors = colors) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButton(selected = selected, onClick = onClick)
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
