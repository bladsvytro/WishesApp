package com.wishesapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.wishesapp.data.model.Space
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpaceRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {

    private val spaces get() = firestore.collection("spaces")
    private val users get() = firestore.collection("users")

    /** Create a new space; returns the created [Space]. */
    suspend fun createSpace(displayName: String): Space {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val code = generateInviteCode()
        val space = Space(
            inviteCode = code,
            createdBy = uid,
            members = mapOf(uid to displayName),
        )
        val ref = spaces.add(space).await()
        users.document(uid).update("spaceId", ref.id).await()
        return space.copy(id = ref.id)
    }

    /** Join a space by [inviteCode]; returns the joined [Space] or throws. */
    suspend fun joinSpace(inviteCode: String, displayName: String): Space {
        val uid = auth.currentUser?.uid ?: error("Not authenticated")
        val query = spaces.whereEqualTo("inviteCode", inviteCode).limit(1).get().await()
        if (query.isEmpty) error("Код приглашения не найден")
        val doc = query.documents.first()
        val space = doc.toObject(Space::class.java) ?: error("Некорректные данные пространства")
        if (space.members.size >= 2) error("Это пространство уже заполнено")
        if (space.members.containsKey(uid)) error("Вы уже состоите в этом пространстве")
        doc.reference.update("members.$uid", displayName).await()
        users.document(uid).update("spaceId", doc.id).await()
        return space.copy(id = doc.id, members = space.members + (uid to displayName))
    }

    /** Observe the space the current user belongs to. Emits null if not in any space. */
    fun observeMySpace(): Flow<Space?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(null); close(); return@callbackFlow }
        var spaceListener: com.google.firebase.firestore.ListenerRegistration? = null
        val userListener = users.document(uid).addSnapshotListener { snap, _ ->
            val spaceId = snap?.getString("spaceId")
            if (spaceId == null) {
                spaceListener?.remove()
                spaceListener = null
                trySend(null)
            } else {
                // Replace any previous space listener (spaceId shouldn't change, but be safe)
                spaceListener?.remove()
                spaceListener = spaces.document(spaceId).addSnapshotListener { spaceSnap, _ ->
                    trySend(spaceSnap?.toObject(Space::class.java)?.copy(id = spaceId))
                }
            }
        }
        awaitClose {
            spaceListener?.remove()
            userListener.remove()
        }
    }

    suspend fun getMySpace(): Space? {
        val uid = auth.currentUser?.uid ?: return null
        val userDoc = users.document(uid).get().await()
        val spaceId = userDoc.getString("spaceId") ?: return null
        val spaceDoc = spaces.document(spaceId).get().await()
        return spaceDoc.toObject(Space::class.java)?.copy(id = spaceId)
    }

    /**
     * Remove the current user from their space.
     * Uses a batch write so both operations are atomic:
     *   1. Remove uid key from space.members map
     *   2. Delete spaceId field from user document
     */
    suspend fun leaveSpace() {
        val uid = auth.currentUser?.uid ?: return
        val spaceId = users.document(uid).get().await().getString("spaceId") ?: return
        val batch = firestore.batch()
        batch.update(spaces.document(spaceId), "members.$uid", FieldValue.delete())
        batch.update(users.document(uid), "spaceId", FieldValue.delete())
        batch.commit().await()
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
