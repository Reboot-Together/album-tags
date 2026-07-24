package com.albumtags.app

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AlbumRepository(private val context: Context) {
    private val preferences =
        context.getSharedPreferences("album_tag_data", Context.MODE_PRIVATE)

    suspend fun loadAlbums(): List<PhotoAlbum> = withContext(Dispatchers.IO) {
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
                        ),
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

    suspend fun loadAlbumPhotos(bucketId: String): List<AlbumPhoto> =
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
                arrayOf(bucketId),
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
                        ),
                        dateTakenMillis = if (taken > 0) taken else addedMillis
                    )
                }
            }
            photos
        }

    fun loadTagMap(): Map<String, Set<String>> {
        val source = preferences.getString(KEY_TAGS, "{}") ?: "{}"
        return runCatching {
            val json = JSONObject(source)
            json.keys().asSequence().associateWith { albumId ->
                val array = json.getJSONArray(albumId)
                buildSet {
                    repeat(array.length()) { add(array.getString(it)) }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun loadTagGroups(): Map<String, Set<String>> {
        val source = preferences.getString(KEY_TAG_GROUPS, "{}") ?: "{}"
        return runCatching {
            val json = JSONObject(source)
            json.keys().asSequence().associateWith { groupName ->
                val array = json.getJSONArray(groupName)
                buildSet {
                    repeat(array.length()) { add(array.getString(it)) }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveTags(albumId: String, tags: Set<String>) {
        val updated = loadTagMap().toMutableMap()
        if (tags.isEmpty()) updated.remove(albumId) else updated[albumId] = tags
        val json = JSONObject()
        updated.forEach { (id, values) ->
            json.put(id, JSONArray(values.sorted()))
        }
        preferences.edit().putString(KEY_TAGS, json.toString()).apply()
    }

    fun saveTagMap(tagMap: Map<String, Set<String>>) {
        val json = JSONObject()
        tagMap.filterValues { it.isNotEmpty() }.forEach { (id, values) ->
            json.put(id, JSONArray(values.sorted()))
        }
        preferences.edit().putString(KEY_TAGS, json.toString()).apply()
    }

    fun saveTagGroups(groups: Map<String, Set<String>>) {
        val json = JSONObject()
        groups.filterValues { it.isNotEmpty() }.forEach { (name, tags) ->
            json.put(name, JSONArray(tags.sorted()))
        }
        preferences.edit().putString(KEY_TAG_GROUPS, json.toString()).apply()
    }

    /** 태그만 내보냅니다. 사진·앨범 원본은 포함하지 않습니다. */
    fun exportTags(): String = JSONObject().apply {
        put("format", BACKUP_FORMAT)
        put("version", 1)
        put("tags", JSONObject(preferences.getString(KEY_TAGS, "{}") ?: "{}"))
        put(
            "tagGroups",
            JSONObject(preferences.getString(KEY_TAG_GROUPS, "{}") ?: "{}")
        )
    }.toString(2)

    /** 올바른 백업일 때만 현재 태그를 교체합니다. */
    fun importTags(contents: String): Boolean = runCatching {
        val backup = JSONObject(contents)
        require(backup.getString("format") == BACKUP_FORMAT)
        val tags = backup.getJSONObject("tags")
        val groups = backup.optJSONObject("tagGroups") ?: JSONObject()
        preferences.edit()
            .putString(KEY_TAGS, tags.toString())
            .putString(KEY_TAG_GROUPS, groups.toString())
            .apply()
        true
    }.getOrDefault(false)

    private data class MutableAlbum(
        val bucketId: String,
        val name: String,
        val coverUri: android.net.Uri,
        var count: Int,
        val newestDateSeconds: Long
    )

    private companion object {
        const val KEY_TAGS = "tags_by_album"
        const val KEY_TAG_GROUPS = "tag_groups"
        const val BACKUP_FORMAT = "album-tags-backup"
    }
}
