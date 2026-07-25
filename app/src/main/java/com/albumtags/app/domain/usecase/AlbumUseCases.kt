package com.albumtags.app.domain.usecase

import com.albumtags.app.domain.model.AlbumPhoto
import com.albumtags.app.domain.model.PhotoAlbum
import com.albumtags.app.domain.repository.AlbumRepository

class LoadAlbumsUseCase(private val repository: AlbumRepository) {
    suspend operator fun invoke(): List<PhotoAlbum> = repository.getAlbums()
}

class LoadAlbumPhotosUseCase(private val repository: AlbumRepository) {
    suspend operator fun invoke(albumId: String): List<AlbumPhoto> =
        repository.getPhotos(albumId)
}

data class AlbumUseCases(
    val loadAlbums: LoadAlbumsUseCase,
    val loadPhotos: LoadAlbumPhotosUseCase
)
