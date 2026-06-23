package com.wishesapp.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(
    onSignedIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.SignedIn) onSignedIn()
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = uiState) {
            is AuthUiState.Loading -> CircularProgressIndicator()
            is AuthUiState.LinkSent -> LinkSentContent(email = state.email)
            else -> EmailInputContent(
                error = if (state is AuthUiState.Error) state.message else null,
                onSend = { viewModel.sendSignInLink(it) },
            )
        }
    }
}

@Composable
private fun EmailInputContent(error: String?, onSend: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Желания 🤍",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Введите email для входа",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (email.isNotBlank()) onSend(email) }),
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
        )
        Button(
            onClick = { if (email.isNotBlank()) onSend(email) },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank(),
        ) {
            Text("Отправить ссылку для входа")
        }
    }
}

@Composable
private fun LinkSentContent(email: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("📧", style = MaterialTheme.typography.displaySmall)
        Text(
            text = "Проверьте почту",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Мы отправили ссылку для входа на\n$email\n\nНажмите на ссылку, чтобы открыть приложение и войти.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
