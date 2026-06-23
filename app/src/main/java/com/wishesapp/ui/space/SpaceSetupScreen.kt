package com.wishesapp.ui.space

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SpaceSetupScreen(
    onSpaceReady: () -> Unit,
    viewModel: SpaceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val mySpace by viewModel.mySpace.collectAsState()

    // Guard against calling onSpaceReady() twice (from observer + Continue button)
    val hasNavigated = remember { mutableStateOf(false) }
    fun navigate() { if (!hasNavigated.value) { hasNavigated.value = true; onSpaceReady() } }

    LaunchedEffect(mySpace) {
        if (mySpace != null) navigate()
    }

    var tab by remember { mutableIntStateOf(0) }
    var displayName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

    val isLoading = uiState is SpaceUiState.Loading
    val errorMsg = (uiState as? SpaceUiState.Error)?.message

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Set up your space", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Create a private space and share the invite code with your partner.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0; viewModel.resetState() },
                text = { Text("Create space") })
            Tab(selected = tab == 1, onClick = { tab = 1; viewModel.resetState() },
                text = { Text("Join space") })
        }

        if (tab == 0) {
            // Create
            Button(
                onClick = { viewModel.createSpace(displayName.ifBlank { "Partner" }) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && displayName.isNotBlank(),
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Create a new space")
            }
        } else {
            // Join
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase().take(6) },
                label = { Text("Invite code (6 characters)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.joinSpace(inviteCode, displayName.ifBlank { "Partner" }) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && displayName.isNotBlank() && inviteCode.length == 6,
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Join space")
            }
        }

        // Show invite code after creation
        val createdSpace = (uiState as? SpaceUiState.Success)?.space
        if (createdSpace != null) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Space created! 🎉", style = MaterialTheme.typography.titleMedium)
                    Text("Share this code with your partner:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        createdSpace.inviteCode,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedButton(onClick = { navigate() }) { Text("Continue") }
                }
            }
        }

        errorMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
