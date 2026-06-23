package com.wishesapp.data.repository

import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.wishesapp.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    // Incremented by MainActivity when a magic link arrives via onNewIntent so the
    // NavBackStackEntry-scoped AuthViewModel can re-check SharedPreferences without
    // needing to be re-created.
    private val _signInCheckTrigger = MutableStateFlow(0)
    val signInCheckTrigger: StateFlow<Int> = _signInCheckTrigger

    fun triggerSignInCheck() { _signInCheckTrigger.value++ }

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Send a sign-in link to [email]. */
    suspend fun sendSignInLink(email: String) {
        val settings = ActionCodeSettings.newBuilder()
            .setUrl("https://wishesapp.page.link/signin")
            .setHandleCodeInApp(true)
            .setAndroidPackageName("com.wishesapp", true, null)
            .build()
        auth.sendSignInLinkToEmail(email, settings).await()
    }

    /** Complete sign-in with the [emailLink] received from the deep link. */
    suspend fun signInWithEmailLink(email: String, emailLink: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailLink(email, emailLink).await()
            val user = result.user ?: throw Exception("Auth succeeded but user is null")
            upsertUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isSignInLink(link: String) = auth.isSignInWithEmailLink(link)

    suspend fun signOut() {
        auth.signOut()
    }

    private suspend fun upsertUser(firebaseUser: FirebaseUser) {
        val ref = firestore.collection("users").document(firebaseUser.uid)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            ref.set(
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@') ?: "User",
                )
            ).await()
        }
    }

    suspend fun updateFcmToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("fcmToken", token).await()
    }

    suspend fun getUser(uid: String): User? {
        return try {
            firestore.collection("users").document(uid).get().await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
