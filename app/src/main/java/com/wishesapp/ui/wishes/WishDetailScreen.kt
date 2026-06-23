package com.wishesapp.ui.wishes

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wishesapp.data.model.Wish
import com.wishesapp.data.repository.ImageUtil
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishDetailScreen(wish: Wish, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var saveSuccess by remember { mutableStateOf<Boolean?>(null) }
    var savePending by remember { mutableStateOf(false) }

    LaunchedEffect(wish.id) {
        wish.imageFull?.let { base64 ->
            withContext(Dispatchers.IO) {
                bitmap = ImageUtil.getOrDecodeWithCache(context, base64, "${wish.id}_full")
            }
        }
    }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && savePending) {
            savePending = false
            scope.launch(Dispatchers.IO) {
                val bmp = bitmap ?: return@launch
                val result = ImageUtil.saveToGallery(context, bmp, "wish_${wish.id}")
                withContext(Dispatchers.Main) { saveSuccess = result }
            }
        } else {
            savePending = false
        }
    }

    fun triggerSave() {
        val bmp = bitmap ?: return
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            savePending = true
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            scope.launch(Dispatchers.IO) {
                val result = ImageUtil.saveToGallery(context, bmp, "wish_${wish.id}")
                withContext(Dispatchers.Main) { saveSuccess = result }
            }
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess != null) {
            kotlinx.coroutines.delay(2_500)
            saveSuccess = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Желание") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") }
                },
                actions = {
                    if (bitmap != null) {
                        IconButton(onClick = { triggerSave() }) {
                            Icon(Icons.Default.Download, contentDescription = "Сохранить в галерею")
                        }
                    }
                },
            )
        },
        snackbarHost = {
            if (saveSuccess != null) {
                Snackbar(modifier = Modifier.padding(16.dp)) {
                    Text(if (saveSuccess == true) "Фото сохранено в галерею" else "Не удалось сохранить фото")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            bitmap?.let {
                androidx.compose.foundation.Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    contentScale = ContentScale.Fit,
                )
            } ?: run {
                if (wish.imageFull != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (wish.text.isNotBlank()) {
                    Text(wish.text, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "от ${wish.authorName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                wish.createdAt?.let {
                    Text(
                        dateFormat.format(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
