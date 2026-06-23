package com.wishesapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Space(
    @DocumentId
    val id: String = "",
    val inviteCode: String = "",
    val createdBy: String = "",
    val members: Map<String, String> = emptyMap(), // uid -> displayName
    @ServerTimestamp
    val createdAt: Date? = null,
)
