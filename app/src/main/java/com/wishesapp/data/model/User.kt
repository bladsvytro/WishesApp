package com.wishesapp.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val spaceId: String? = null,
    val fcmToken: String? = null,
)
