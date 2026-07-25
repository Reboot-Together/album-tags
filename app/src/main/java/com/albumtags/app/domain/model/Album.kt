package com.albumtags.app.domain.model

data class PhotoAlbum(
    val bucketId: String,
    val name: String,
    val coverUri: String,
    val photoCount: Int,
    val newestDateSeconds: Long
)

data class AlbumPhoto(
    val id: Long,
    val uri: String,
    val dateTakenMillis: Long
)
