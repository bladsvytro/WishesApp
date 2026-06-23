package com.wishesapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Wish(
    @DocumentId
    val id: String = "",
    val spaceId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    /** Base64-encoded JPEG, widget-sized (~200px, ~60KB) */
    val imageSmall: String? = null,
    /** Base64-encoded JPEG, full-sized (~1080px, ~300KB) */
    val imageFull: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    val seenBy: List<String> = emptyList(),
)
