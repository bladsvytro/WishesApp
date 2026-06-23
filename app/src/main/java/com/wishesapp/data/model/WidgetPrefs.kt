package com.wishesapp.data.model

enum class WidgetMode { LATEST, RANDOM }

data class WidgetState(
    val mode: WidgetMode = WidgetMode.LATEST,
    val wishId: String = "",
    val wishText: String = "",
    val authorName: String = "",
    /** Absolute file path of the cached widget image, or null */
    val imagePath: String? = null,
)
