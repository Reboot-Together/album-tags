package com.albumtags.app.domain.repository

import com.albumtags.app.domain.model.AlbumCover

interface AlbumCoverRepository {
    fun getCover(albumId: String): AlbumCover?
    fun setCover(albumId: String, cover: AlbumCover)
}
