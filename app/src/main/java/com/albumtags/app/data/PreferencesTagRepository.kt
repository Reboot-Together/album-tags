package com.albumtags.app.data

import android.content.Context
import com.albumtags.app.domain.model.TagState
import com.albumtags.app.domain.repository.TagRepository
import org.json.JSONArray
import org.json.JSONObject

class PreferencesTagRepository(context: Context) : TagRepository {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): TagState = TagState(
        albumTags = readSetMap(KEY_TAGS),
        groups = readSetMap(KEY_TAG_GROUPS)
    )

    override fun save(state: TagState) {
        preferences.edit()
            .putString(KEY_TAGS, state.albumTags.toJson().toString())
            .putString(KEY_TAG_GROUPS, state.groups.toJson().toString())
            .apply()
    }

    override fun exportBackup(): String = JSONObject().apply {
        put("format", BACKUP_FORMAT)
        put("version", 1)
        put("tags", load().albumTags.toJson())
        put("tagGroups", load().groups.toJson())
    }.toString(2)

    override fun importBackup(contents: String): Boolean = runCatching {
        val backup = JSONObject(contents)
        require(backup.getString("format") == BACKUP_FORMAT)
        val state = TagState(
            albumTags = backup.getJSONObject("tags").toSetMap(),
            groups = (backup.optJSONObject("tagGroups") ?: JSONObject()).toSetMap()
        )
        save(state)
        true
    }.getOrDefault(false)

    private fun readSetMap(key: String): Map<String, Set<String>> =
        runCatching {
            JSONObject(preferences.getString(key, "{}") ?: "{}").toSetMap()
        }.getOrDefault(emptyMap())

    private fun JSONObject.toSetMap(): Map<String, Set<String>> =
        keys().asSequence().associateWith { name ->
            val array = getJSONArray(name)
            buildSet {
                repeat(array.length()) { add(array.getString(it)) }
            }
        }

    private fun Map<String, Set<String>>.toJson(): JSONObject =
        JSONObject().apply {
            filterValues { it.isNotEmpty() }.forEach { (name, values) ->
                put(name, JSONArray(values.sorted()))
            }
        }

    private companion object {
        const val PREFERENCES_NAME = "album_tag_data"
        const val KEY_TAGS = "tags_by_album"
        const val KEY_TAG_GROUPS = "tag_groups"
        const val BACKUP_FORMAT = "album-tags-backup"
    }
}
