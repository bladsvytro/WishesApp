package com.wishesapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.wishesapp.data.repository.AuthRepository
import com.wishesapp.ui.Routes
import com.wishesapp.ui.WishesNavGraph
import com.wishesapp.ui.theme.WishesTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var authRepo: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        val startDestination = when {
            auth.currentUser == null -> Routes.AUTH
            else -> Routes.WISH_LIST
        }
        setContent {
            WishesTheme {
                WishesNavGraph(startDestination = startDestination)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val link = intent?.data?.toString() ?: return
        if (!auth.isSignInWithEmailLink(link)) return
        // Save directly to SharedPreferences so the NavBackStackEntry-scoped AuthViewModel
        // picks it up — either via its init (cold start) or via signInCheckTrigger (onNewIntent).
        getSharedPreferences("auth", MODE_PRIVATE)
            .edit().putString("pending_email_link", link).apply()
        authRepo.triggerSignInCheck()
    }
}
