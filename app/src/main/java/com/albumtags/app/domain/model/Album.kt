package com.albumtags.app.domain.model

data class PhotoAlbum(
    val bucketId: String,
    val name: String,
    val coverUri: String,
    val photoCount: Int,
    val newestDateSeconds: Long,
    val oldestDateSeconds: Long = newestDateSeconds,
    val newestCaptureDateSeconds: Long = newestDateSeconds,
    val relativePath: String = "",
    val videoCount: Int = 0,
    val coverIsVideo: Boolean = false
)

data class AlbumPhoto(
    val id: Long,
    val uri: String,
    val dateTakenMillis: Long,
    val dateAddedMillis: Long = dateTakenMillis,
    val mimeType: String = "image/*",
    val isVideo: Boolean = false
) {
    val stableKey: String get() = uri
}
