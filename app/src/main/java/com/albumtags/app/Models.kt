package com.albumtags.app

import android.net.Uri

data class PhotoAlbum(
    val bucketId: String,
    val name: String,
    val coverUri: Uri,
    val photoCount: Int,
    val newestDateSeconds: Long
)

data class AlbumPhoto(
    val id: Long,
    val uri: Uri,
    val dateTakenMillis: Long
)

enum class TagMatchMode { ALL, ANY }

data class AlbumUiState(
    val albums: List<PhotoAlbum> = emptyList(),
    val tagMap: Map<String, Set<String>> = emptyMap(),
    val selectedTags: Set<String> = emptySet(),
    val query: String = "",
    val matchMode: TagMatchMode = TagMatchMode.ALL,
    val showUntaggedOnly: Boolean = false,
    val isLoading: Boolean = true,
    val permissionGranted: Boolean = false,
    val openedAlbumId: String? = null,
    val albumPhotos: List<AlbumPhoto> = emptyList(),
    val arePhotosLoading: Boolean = false
) {
    val allTags: List<String>
        get() = tagMap.values.flatten().distinct().sorted()

    val visibleAlbums: List<PhotoAlbum>
        get() = albums.filter { album ->
            val tags = tagMap[album.bucketId].orEmpty()
            val matchesQuery = query.isBlank() ||
                album.name.contains(query, ignoreCase = true) ||
                tags.any { it.contains(query, ignoreCase = true) }
            val matchesTags = selectedTags.isEmpty() ||
                if (matchMode == TagMatchMode.ALL) tags.containsAll(selectedTags)
                else tags.any { it in selectedTags }
            val matchesUntagged = !showUntaggedOnly || tags.isEmpty()
            matchesQuery && matchesTags && matchesUntagged
        }

    val openedAlbum: PhotoAlbum?
        get() = albums.firstOrNull { it.bucketId == openedAlbumId }
}
