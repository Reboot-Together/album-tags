package com.albumtags.app.domain.usecase

import com.albumtags.app.domain.model.AlbumCover
import com.albumtags.app.domain.model.AlbumPhoto
import com.albumtags.app.domain.model.PhotoAlbum
import com.albumtags.app.domain.repository.AlbumCoverRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumCoverUseCasesTest {
    @Test
    fun `custom cover overrides media store cover`() {
        val repository = FakeAlbumCoverRepository()
        val media = AlbumPhoto(
            id = 2,
            uri = "content://custom/video",
            dateTakenMillis = 10,
            isVideo = true
        )
        SetCustomAlbumCoverUseCase(repository)("album", media)

        val result = ApplyCustomAlbumCoversUseCase(repository)(
            listOf(
                PhotoAlbum(
                    bucketId = "album",
                    name = "여행",
                    coverUri = "content://default",
                    photoCount = 2,
                    newestDateSeconds = 10
                )
            )
        ).single()

        assertEquals(media.uri, result.coverUri)
        assertTrue(result.coverIsVideo)
    }
}

private class FakeAlbumCoverRepository : AlbumCoverRepository {
    private val covers = mutableMapOf<String, AlbumCover>()

    override fun getCover(albumId: String): AlbumCover? = covers[albumId]

    override fun setCover(albumId: String, cover: AlbumCover) {
        covers[albumId] = cover
    }
}
