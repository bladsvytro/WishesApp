package com.wishesapp.ui.wishes

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWishScreen(
    onBack: () -> Unit,
    onAdded: () -> Unit,
    viewModel: WishViewModel = hiltViewModel(),
) {
    val addState by viewModel.addState.collectAsState()
    val previewBitmap by viewModel.pendingImageBitmap.collectAsState()

    var text by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
        viewModel.setPendingImage(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) imagePickerLauncher.launch("image/*")
    }

    LaunchedEffect(addState) {
        if (addState is AddState.Success) {
            viewModel.resetAddState()
            onAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New wish") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.addWish(text, imageUri) },
                        enabled = (text.isNotBlank() || imageUri != null) && addState !is AddState.Loading,
                    ) { Text("Post") }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe your wish...") },
                minLines = 4,
            )

            // Image preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            imagePickerLauncher.launch("image/*")
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (previewBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    IconButton(
                        onClick = {
                            imageUri = null
                            viewModel.clearPendingImage()
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    ) {
                        Icon(Icons.Default.Close, "Remove image",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Add a photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (addState is AddState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            (addState as? AddState.Error)?.let {
                Text(it.message, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
