package com.albumtags.app.domain.usecase

import com.albumtags.app.domain.model.AlbumCover
import com.albumtags.app.domain.model.AlbumPhoto
import com.albumtags.app.domain.model.PhotoAlbum
import com.albumtags.app.domain.repository.AlbumCoverRepository

class ApplyCustomAlbumCoversUseCase(
    private val repository: AlbumCoverRepository
) {
    operator fun invoke(albums: List<PhotoAlbum>): List<PhotoAlbum> =
        albums.map { album ->
            repository.getCover(album.bucketId)?.let { cover ->
                album.copy(
                    coverUri = cover.uri,
                    coverIsVideo = cover.isVideo
                )
            } ?: album
        }
}

class SetCustomAlbumCoverUseCase(
    private val repository: AlbumCoverRepository
) {
    operator fun invoke(albumId: String, media: AlbumPhoto): AlbumCover =
        AlbumCover(media.uri, media.isVideo).also {
            repository.setCover(albumId, it)
        }
}

data class AlbumCoverUseCases(
    val apply: ApplyCustomAlbumCoversUseCase,
    val set: SetCustomAlbumCoverUseCase
)
