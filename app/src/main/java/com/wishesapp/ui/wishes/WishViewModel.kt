package com.wishesapp.ui.wishes

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.wishesapp.data.model.Wish
import com.wishesapp.data.model.WidgetMode
import com.wishesapp.data.repository.ImageUtil
import com.wishesapp.data.repository.SpaceRepository
import com.wishesapp.data.repository.WidgetPrefsRepository
import com.wishesapp.data.repository.WishRepository
import com.wishesapp.widget.WishWidgetWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class WishViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wishRepo: WishRepository,
    private val spaceRepo: SpaceRepository,
    private val widgetPrefsRepo: WidgetPrefsRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _wishes = MutableStateFlow<List<Wish>>(emptyList())
    val wishes: StateFlow<List<Wish>> = _wishes

    private val _addState = MutableStateFlow<AddState>(AddState.Idle)
    val addState: StateFlow<AddState> = _addState

    /** Decoded preview bitmap for the AddWish screen — set on background thread. */
    private val _pendingImageBitmap = MutableStateFlow<Bitmap?>(null)
    val pendingImageBitmap: StateFlow<Bitmap?> = _pendingImageBitmap

    val widgetMode = widgetPrefsRepo.widgetState

    private var lastKnownTopWishId: String? = null

    init {
        viewModelScope.launch {
            wishRepo.observeWishes().collect { list ->
                _wishes.value = list
                // When a new wish from the partner appears, immediately refresh the widget
                val myUid = auth.currentUser?.uid
                val topPartnerWish = list.firstOrNull { it.authorId != myUid }
                if (topPartnerWish != null && topPartnerWish.id != lastKnownTopWishId) {
                    lastKnownTopWishId = topPartnerWish.id
                    WishWidgetWorker.enqueueForPush(context)
                }
            }
        }
    }

    /** Decode a picked image URI in the background for preview. */
    fun setPendingImage(uri: Uri?) {
        if (uri == null) { _pendingImageBitmap.value = null; return }
        viewModelScope.launch(Dispatchers.IO) {
            val encoded = ImageUtil.encodeImageUri(context, uri)
            val bitmap = encoded?.second?.let { ImageUtil.decodeBase64(it) }
            _pendingImageBitmap.value = bitmap
        }
    }

    fun clearPendingImage() { _pendingImageBitmap.value = null }

    /** Add a wish. Encoding happens inside WishRepository on IO dispatcher. */
    fun addWish(text: String, imageUri: Uri?) {
        if (text.isBlank() && imageUri == null) return
        _addState.value = AddState.Loading
        viewModelScope.launch {
            wishRepo.addWish(text, imageUri).fold(
                onSuccess = {
                    _pendingImageBitmap.value = null
                    _addState.value = AddState.Success
                },
                onFailure = { _addState.value = AddState.Error(it.message ?: "Не удалось добавить желание") }
            )
        }
    }

    fun resetAddState() { _addState.value = AddState.Idle }

    fun setWidgetMode(mode: WidgetMode) {
        viewModelScope.launch { widgetPrefsRepo.setWidgetMode(mode) }
    }

    fun deleteWish(spaceId: String, wishId: String) {
        viewModelScope.launch { wishRepo.deleteWish(spaceId, wishId) }
    }

    val currentUserId: String? get() = auth.currentUser?.uid

    /** Called after the user leaves their space. Cancels background jobs and clears local caches. */
    fun onSpaceLeft() {
        WishWidgetWorker.cancelPeriodic(context)
        viewModelScope.launch {
            widgetPrefsRepo.saveWidgetState(com.wishesapp.data.model.WidgetState())
        }
        // Clear decoded image caches so stale thumbnails don't appear in the next space
        File(context.cacheDir, "wish_cache").deleteRecursively()
        File(context.cacheDir, "widget_images").deleteRecursively()
    }

    fun signOut() {
        auth.signOut()
        WishWidgetWorker.cancelPeriodic(context)
    }
}

sealed interface AddState {
    data object Idle : AddState
    data object Loading : AddState
    data object Success : AddState
    data class Error(val message: String) : AddState
}
