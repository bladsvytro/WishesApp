package com.wishesapp.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.wishesapp.data.model.Wish
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WishRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val spaceRepository: SpaceRepository,
) {

    private fun wishesCollection(spaceId: String) =
        firestore.collection("spaces").document(spaceId).collection("wishes")

    /** Observe all wishes in the current user's space, newest first. */
    fun observeWishes(): Flow<List<Wish>> = callbackFlow {
        val space = spaceRepository.getMySpace()
            ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val listener = wishesCollection(space.id)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val wishes = snap?.toObjects(Wish::class.java) ?: emptyList()
                trySend(wishes)
            }
        awaitClose { listener.remove() }
    }

    /** Get a single wish by id. */
    suspend fun getWish(spaceId: String, wishId: String): Wish? = try {
        wishesCollection(spaceId).document(wishId).get().await()
            .toObject(Wish::class.java)?.copy(id = wishId)
    } catch (e: Exception) { null }

    /** Get wishes for the widget (up to 50, newest first). */
    suspend fun getWishesForWidget(spaceId: String): List<Wish> = try {
        wishesCollection(spaceId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get().await()
            .toObjects(Wish::class.java)
    } catch (e: Exception) { emptyList() }

    /**
     * Add a new wish. [imageUri] is optional; if provided it's encoded to JPEG base64
     * (small thumbnail + full size) on the IO dispatcher.
     *
     * Widget refresh for the partner is handled automatically:
     *  - While partner's app is open: Firestore real-time snapshot triggers WishViewModel
     *    which calls WishWidgetWorker.enqueueForPush().
     *  - While partner's app is closed: periodic WorkManager job refreshes every 30 min.
     *
     * A full push-on-write requires a Cloud Function with the FCM Admin SDK —
     * see README for the optional upgrade path.
     */
    suspend fun addWish(text: String, imageUri: Uri?): Result<Wish> = try {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val space = spaceRepository.getMySpace() ?: error("Not in a space")
        val userDoc = firestore.collection("users").document(uid).get().await()
        val displayName = userDoc.getString("displayName") ?: "Партнёр"

        var imageSmall: String? = null
        var imageFull: String? = null
        if (imageUri != null) {
            val encoded = withContext(Dispatchers.IO) {
                ImageUtil.encodeImageUri(context, imageUri)
            } ?: error("Failed to encode image — check that the file is a valid JPEG/PNG")
            imageSmall = encoded.first   // ~200px thumbnail for widget
            imageFull = encoded.second  // ~1080px for detail view

            // Guard: Firestore doc limit is 1 MiB. Our two base64 strings + text should be
            // well under that, but validate to give a clear error on edge cases.
            val estimatedBytes = imageSmall.length + imageFull.length + text.length
            if (estimatedBytes > 900_000) {
                error("Фото слишком большое. Попробуйте выбрать фото с меньшим разрешением.")
            }
        }

        val wish = Wish(
            spaceId = space.id,
            authorId = uid,
            authorName = displayName,
            text = text,
            imageSmall = imageSmall,
            imageFull = imageFull,
        )
        val ref = wishesCollection(space.id).add(wish).await()
        Result.success(wish.copy(id = ref.id))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Mark a wish as seen by the current user. */
    suspend fun markSeen(spaceId: String, wishId: String) {
        val uid = auth.currentUser?.uid ?: return
        wishesCollection(spaceId).document(wishId)
            .update("seenBy", FieldValue.arrayUnion(uid))
            .await()
    }

    /** Delete a wish — Firestore rules enforce only the author can do this. */
    suspend fun deleteWish(spaceId: String, wishId: String) {
        wishesCollection(spaceId).document(wishId).delete().await()
        ImageUtil.deleteCachedWish(context, wishId)
    }
}
