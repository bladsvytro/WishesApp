package com.wishesapp.ui.wishes

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wishesapp.data.model.Wish
import com.wishesapp.data.repository.ImageUtil
import com.wishesapp.ui.space.SpaceViewModel
import com.wishesapp.ui.update.UpdateBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishListScreen(
    onAddWish: () -> Unit,
    onWishClick: (Wish) -> Unit,
    onSettings: () -> Unit,
    onNoSpace: () -> Unit,
    viewModel: WishViewModel = hiltViewModel(),
    spaceViewModel: SpaceViewModel = hiltViewModel(),
) {
    val wishes by viewModel.wishes.collectAsState()
    val mySpace by spaceViewModel.mySpace.collectAsState()
    val currentUserId = viewModel.currentUserId
    val spaceChecked = remember { mutableStateOf(false) }

    LaunchedEffect(mySpace) {
        if (!spaceChecked.value) {
            kotlinx.coroutines.delay(1_500)
            spaceChecked.value = true
            if (mySpace == null) onNoSpace()
        }
    }

    val partnerName = remember(mySpace, currentUserId) {
        mySpace?.members?.entries?.firstOrNull { it.key != currentUserId }?.value
    }
    val waitingForPartner = mySpace != null && mySpace!!.members.size < 2

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wishes 🤍")
                        if (partnerName != null) {
                            Text(
                                "with $partnerName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWish) {
                Icon(Icons.Default.Add, contentDescription = "Add wish")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            UpdateBanner()

            // Invite code banner — shown until the partner joins
            AnimatedVisibility(
                visible = waitingForPartner,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                if (mySpace != null) {
                    InviteCodeBanner(inviteCode = mySpace!!.inviteCode)
                }
            }

            if (wishes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (!spaceChecked.value) {
                        CircularProgressIndicator()
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("🌟", style = MaterialTheme.typography.displayMedium)
                            Text("No wishes yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap + to add your first wish",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(wishes, key = { it.id }) { wish ->
                        WishCard(wish = wish, onClick = { onWishClick(wish) })
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteCodeBanner(inviteCode: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Waiting for your partner",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "Invite code: $inviteCode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(inviteCode))
                copied = true
            }) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy invite code",
                    tint = if (copied) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishCard(wish: Wish, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // Use the thumbnail size for list cards; imageFull is used in the detail view.
            val thumbBase64 = wish.imageSmall ?: wish.imageFull
            thumbBase64?.let { base64 ->
                val bitmap by produceState<Bitmap?>(null, wish.id) {
                    withContext(Dispatchers.IO) {
                        value = ImageUtil.getOrDecodeWithCache(context, base64, "${wish.id}_small")
                    }
                }
                bitmap?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (wish.text.isNotBlank()) {
                    Text(wish.text, style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    "by ${wish.authorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
