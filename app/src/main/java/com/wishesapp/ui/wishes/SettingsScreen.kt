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

    // Navigate out once leave-space succeeds
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
            title = { Text("Sign out?") },
            text = { Text("You can sign back in with your email link. Your wishes stay safe in the cloud.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    wishViewModel.signOut()
                    onSignedOut()
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showLeaveSpaceDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveSpaceDialog = false },
            title = { Text("Leave this space?") },
            text = { Text("Your wishes will stay in the cloud. You can create or join a new space afterwards.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveSpaceDialog = false
                        spaceViewModel.leaveSpace()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveSpaceDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Space info ──────────────────────────────────────────────────
            mySpace?.let { space ->
                val partnerName = space.members.entries
                    .firstOrNull { it.key != currentUserId }?.value

                Text("Your space", style = MaterialTheme.typography.titleMedium)

                if (partnerName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SuggestionChip(onClick = {}, label = { Text("You + $partnerName") })
                    }
                } else {
                    Text(
                        "Waiting for your partner to join",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Invite code card — useful to reshare
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
                                "Invite code",
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
                                contentDescription = "Copy invite code",
                                tint = if (inviteCodeCopied) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                HorizontalDivider()
            }

            // ── Widget mode ─────────────────────────────────────────────────
            Text("Widget", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose what the home-screen widget shows",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WidgetModeButton(
                    label = "Latest wish",
                    description = "Updates instantly when your partner adds a new wish",
                    selected = widgetState?.mode == WidgetMode.LATEST,
                    onClick = { wishViewModel.setWidgetMode(WidgetMode.LATEST) },
                    modifier = Modifier.weight(1f),
                )
                WidgetModeButton(
                    label = "Random wish",
                    description = "Rotates to a random wish every 30 minutes + tap to change",
                    selected = widgetState?.mode == WidgetMode.RANDOM,
                    onClick = { wishViewModel.setWidgetMode(WidgetMode.RANDOM) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Leave space ─────────────────────────────────────────────────
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
                        Text("Leave space")
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

            // ── Sign out ─────────────────────────────────────────────────────
            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Sign out")
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
