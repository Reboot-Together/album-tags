package com.albumtags.app.domain.repository

import com.albumtags.app.domain.model.AlbumPhoto
import com.albumtags.app.domain.model.PhotoAlbum

interface AlbumRepository {
    suspend fun getAlbums(): List<PhotoAlbum>
    suspend fun getPhotos(albumId: String): List<AlbumPhoto>
}
