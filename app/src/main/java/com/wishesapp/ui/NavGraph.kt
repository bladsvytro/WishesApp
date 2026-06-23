package com.wishesapp.ui

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wishesapp.data.model.Wish
import com.wishesapp.ui.auth.AuthScreen
import com.wishesapp.ui.space.SpaceSetupScreen
import com.wishesapp.ui.wishes.AddWishScreen
import com.wishesapp.ui.wishes.SettingsScreen
import com.wishesapp.ui.wishes.WishDetailScreen
import com.wishesapp.ui.wishes.WishListScreen

object Routes {
    const val AUTH = "auth"
    const val SPACE_SETUP = "space_setup"
    const val WISH_LIST = "wish_list"
    const val ADD_WISH = "add_wish"
    const val SETTINGS = "settings"
    const val WISH_DETAIL = "wish_detail"
}

@Composable
fun WishesNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    var selectedWish by remember { mutableStateOf<Wish?>(null) }

    NavHost(navController, startDestination = startDestination) {

        composable(Routes.AUTH) {
            AuthScreen(
                onSignedIn = {
                    navController.navigate(Routes.WISH_LIST) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SPACE_SETUP) {
            SpaceSetupScreen(
                onSpaceReady = {
                    navController.navigate(Routes.WISH_LIST) {
                        popUpTo(Routes.SPACE_SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.WISH_LIST) {
            WishListScreen(
                onAddWish = { navController.navigate(Routes.ADD_WISH) },
                onWishClick = { wish ->
                    selectedWish = wish
                    navController.navigate(Routes.WISH_DETAIL)
                },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onNoSpace = {
                    navController.navigate(Routes.SPACE_SETUP) {
                        popUpTo(Routes.WISH_LIST) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ADD_WISH) {
            AddWishScreen(
                onBack = { navController.popBackStack() },
                onAdded = { navController.popBackStack() },
            )
        }

        composable(Routes.WISH_DETAIL) {
            val wish = selectedWish
            if (wish != null) {
                WishDetailScreen(wish = wish, onBack = { navController.popBackStack() })
            }
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSpaceLeft = {
                    navController.navigate(Routes.SPACE_SETUP) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
