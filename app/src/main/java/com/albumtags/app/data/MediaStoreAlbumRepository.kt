package com.albumtags.app.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.albumtags.app.domain.model.AlbumPhoto
import com.albumtags.app.domain.model.PhotoAlbum
import com.albumtags.app.domain.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreAlbumRepository(
    private val context: Context
) : AlbumRepository {
    override suspend fun getAlbums(): List<PhotoAlbum> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.RELATIVE_PATH
        )
        val albums = linkedMapOf<String, MutableAlbum>()
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)",
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            ),
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mediaTypeColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val takenColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketColumn) ?: continue
                val imageId = cursor.getLong(idColumn)
                val date = cursor.getLong(dateColumn)
                val takenSeconds = cursor.getLong(takenColumn).let {
                    if (it > 0) it / 1_000L else date
                }
                val isVideo = cursor.getInt(mediaTypeColumn) ==
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val current = albums[bucketId]
                if (current == null) {
                    albums[bucketId] = MutableAlbum(
                        bucketId = bucketId,
                        name = cursor.getString(nameColumn) ?: "이름 없는 앨범",
                        coverUri = mediaUri(imageId, isVideo),
                        count = 1,
                        newestDateSeconds = date,
                        oldestDateSeconds = takenSeconds,
                        newestCaptureDateSeconds = takenSeconds,
                        relativePath = cursor.getString(pathColumn).orEmpty(),
                        videoCount = if (isVideo) 1 else 0,
                        coverIsVideo = isVideo
                    )
                } else {
                    current.count++
                    current.oldestDateSeconds =
                        minOf(current.oldestDateSeconds, takenSeconds)
                    if (takenSeconds > current.newestCaptureDateSeconds) {
                        current.newestCaptureDateSeconds = takenSeconds
                        current.coverUri = mediaUri(imageId, isVideo)
                        current.coverIsVideo = isVideo
                    }
                    if (isVideo) current.videoCount++
                }
            }
        }
        albums.values.map {
            PhotoAlbum(
                it.bucketId,
                it.name,
                it.coverUri,
                it.count,
                it.newestDateSeconds,
                it.oldestDateSeconds,
                it.newestCaptureDateSeconds,
                it.relativePath,
                it.videoCount,
                it.coverIsVideo
            )
        }.sortedByDescending { it.newestDateSeconds }
    }

    override suspend fun getPhotos(albumId: String): List<AlbumPhoto> =
        withContext(Dispatchers.IO) {
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Images.ImageColumns.DATE_TAKEN
            )
            val photos = mutableListOf<AlbumPhoto>()
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Files.FileColumns.BUCKET_ID} = ? AND " +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)",
                arrayOf(
                    albumId,
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                ),
                "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC, " +
                    "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mediaTypeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val takenColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
                val addedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val mimeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val taken = cursor.getLong(takenColumn)
                    val addedMillis = cursor.getLong(addedColumn) * 1_000L
                    photos += AlbumPhoto(
                        id = id,
                        uri = mediaUri(
                            id,
                            cursor.getInt(mediaTypeColumn) ==
                                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        ),
                        dateTakenMillis = if (taken > 0) taken else addedMillis,
                        dateAddedMillis = addedMillis,
                        mimeType = cursor.getString(mimeColumn).orEmpty(),
                        isVideo = cursor.getInt(mediaTypeColumn) ==
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    )
                }
            }
            photos
        }

    private data class MutableAlbum(
        val bucketId: String,
        val name: String,
        var coverUri: String,
        var count: Int,
        val newestDateSeconds: Long,
        var oldestDateSeconds: Long,
        var newestCaptureDateSeconds: Long,
        val relativePath: String,
        var videoCount: Int,
        var coverIsVideo: Boolean
    )

    private fun mediaUri(id: Long, isVideo: Boolean): String =
        ContentUris.withAppendedId(
            if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            },
            id
        ).toString()
}
