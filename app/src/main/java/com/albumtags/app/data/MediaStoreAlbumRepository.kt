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
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val albums = linkedMapOf<String, MutableAlbum>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketColumn) ?: continue
                val imageId = cursor.getLong(idColumn)
                val date = cursor.getLong(dateColumn)
                val current = albums[bucketId]
                if (current == null) {
                    albums[bucketId] = MutableAlbum(
                        bucketId = bucketId,
                        name = cursor.getString(nameColumn) ?: "이름 없는 앨범",
                        coverUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            imageId
                        ).toString(),
                        count = 1,
                        newestDateSeconds = date
                    )
                } else {
                    current.count++
                }
            }
        }
        albums.values.map {
            PhotoAlbum(it.bucketId, it.name, it.coverUri, it.count, it.newestDateSeconds)
        }.sortedByDescending { it.newestDateSeconds }
    }

    override suspend fun getPhotos(albumId: String): List<AlbumPhoto> =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED
            )
            val photos = mutableListOf<AlbumPhoto>()
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Images.Media.BUCKET_ID} = ?",
                arrayOf(albumId),
                "${MediaStore.Images.Media.DATE_TAKEN} DESC, " +
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val takenColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val addedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val taken = cursor.getLong(takenColumn)
                    val addedMillis = cursor.getLong(addedColumn) * 1_000L
                    photos += AlbumPhoto(
                        id = id,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString(),
                        dateTakenMillis = if (taken > 0) taken else addedMillis
                    )
                }
            }
            photos
        }

    private data class MutableAlbum(
        val bucketId: String,
        val name: String,
        val coverUri: String,
        var count: Int,
        val newestDateSeconds: Long
    )
}
