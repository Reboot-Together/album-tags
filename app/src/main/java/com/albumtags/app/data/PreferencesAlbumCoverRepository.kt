package com.albumtags.app.data

import android.content.Context
import com.albumtags.app.domain.model.AlbumCover
import com.albumtags.app.domain.repository.AlbumCoverRepository

class PreferencesAlbumCoverRepository(context: Context) : AlbumCoverRepository {
    private val preferences = context.getSharedPreferences(
        "album_custom_covers",
        Context.MODE_PRIVATE
    )

    override fun getCover(albumId: String): AlbumCover? {
        val uri = preferences.getString("${albumId}_uri", null) ?: return null
        return AlbumCover(
            uri = uri,
            isVideo = preferences.getBoolean("${albumId}_video", false)
        )
    }

    override fun setCover(albumId: String, cover: AlbumCover) {
        preferences.edit()
            .putString("${albumId}_uri", cover.uri)
            .putBoolean("${albumId}_video", cover.isVideo)
            .apply()
    }
}
