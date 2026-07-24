package com.albumtags.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AlbumRepository(application)
    private val _uiState = MutableStateFlow(
        AlbumUiState(
            tagMap = repository.loadTagMap(),
            tagGroups = repository.loadTagGroups()
        )
    )
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    fun loadAlbums(permissionGranted: Boolean) {
        _uiState.update { it.copy(permissionGranted = permissionGranted) }
        if (!permissionGranted) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val albums = repository.loadAlbums()
            _uiState.update {
                it.copy(
                    albums = albums,
                    tagMap = repository.loadTagMap(),
                    tagGroups = repository.loadTagGroups(),
                    isLoading = false
                )
            }
        }
    }

    fun setQuery(value: String) = _uiState.update { it.copy(query = value) }

    fun toggleTag(tag: String) = _uiState.update {
        val tags = it.selectedTags.toMutableSet()
        if (!tags.add(tag)) tags.remove(tag)
        it.copy(selectedTags = tags, showUntaggedOnly = false)
    }

    fun clearFilters() = _uiState.update {
        it.copy(selectedTags = emptySet(), query = "", showUntaggedOnly = false)
    }

    fun toggleUntagged() = _uiState.update {
        it.copy(showUntaggedOnly = !it.showUntaggedOnly, selectedTags = emptySet())
    }

    fun toggleMatchMode() = _uiState.update {
        it.copy(
            matchMode = if (it.matchMode == TagMatchMode.ALL) {
                TagMatchMode.ANY
            } else {
                TagMatchMode.ALL
            }
        )
    }

    fun saveTags(albumId: String, tags: Set<String>) {
        repository.saveTags(albumId, tags)
        _uiState.update {
            it.copy(tagMap = it.tagMap.toMutableMap().apply {
                if (tags.isEmpty()) remove(albumId) else put(albumId, tags)
            })
        }
    }

    fun renameTag(oldName: String, newName: String): Boolean {
        val normalized = newName.trim().removePrefix("#")
        if (normalized.isEmpty() || normalized == oldName) return false
        val updated = _uiState.value.tagMap.mapValues { (_, tags) ->
            if (oldName in tags) (tags - oldName) + normalized else tags
        }
        repository.saveTagMap(updated)
        val updatedGroups = _uiState.value.tagGroups.mapValues { (_, tags) ->
            if (oldName in tags) (tags - oldName) + normalized else tags
        }
        repository.saveTagGroups(updatedGroups)
        _uiState.update {
            it.copy(
                tagMap = updated,
                tagGroups = updatedGroups,
                selectedTags = it.selectedTags
                    .let { selected -> if (oldName in selected) (selected - oldName) + normalized else selected }
            )
        }
        return true
    }

    fun deleteTag(tagName: String) {
        val updated = _uiState.value.tagMap.mapValues { (_, tags) ->
            tags - tagName
        }
        repository.saveTagMap(updated)
        val updatedGroups = _uiState.value.tagGroups.mapValues { (_, tags) ->
            tags - tagName
        }.filterValues { it.isNotEmpty() }
        repository.saveTagGroups(updatedGroups)
        _uiState.update {
            it.copy(
                tagMap = updated,
                tagGroups = updatedGroups,
                selectedTags = it.selectedTags - tagName
            )
        }
    }

    fun addTagsToAlbums(albumIds: Set<String>, tagsToAdd: Set<String>) {
        if (albumIds.isEmpty() || tagsToAdd.isEmpty()) return
        val updated = _uiState.value.tagMap.toMutableMap()
        albumIds.forEach { albumId ->
            updated[albumId] = updated[albumId].orEmpty() + tagsToAdd
        }
        repository.saveTagMap(updated)
        _uiState.update { it.copy(tagMap = updated) }
    }

    fun saveTagGroup(
        originalName: String?,
        groupName: String,
        tags: Set<String>
    ): Boolean {
        val normalizedName = groupName.trim()
        val validTags = tags.intersect(_uiState.value.allTags.toSet())
        if (normalizedName.isEmpty() || validTags.isEmpty()) return false
        if (normalizedName != originalName &&
            normalizedName in _uiState.value.tagGroups
        ) return false

        val updated = _uiState.value.tagGroups.toMutableMap()
        originalName?.let { updated.remove(it) }
        updated.keys.toList().forEach { name ->
            updated[name] = updated[name].orEmpty() - validTags
            if (updated[name].isNullOrEmpty()) updated.remove(name)
        }
        updated[normalizedName] = validTags
        repository.saveTagGroups(updated)
        _uiState.update { it.copy(tagGroups = updated) }
        return true
    }

    fun deleteTagGroup(groupName: String) {
        val updated = _uiState.value.tagGroups - groupName
        repository.saveTagGroups(updated)
        _uiState.update { it.copy(tagGroups = updated) }
    }

    fun openAlbum(albumId: String) {
        _uiState.update {
            it.copy(
                openedAlbumId = albumId,
                albumPhotos = emptyList(),
                arePhotosLoading = true
            )
        }
        viewModelScope.launch {
            val photos = repository.loadAlbumPhotos(albumId)
            _uiState.update {
                if (it.openedAlbumId == albumId) {
                    it.copy(albumPhotos = photos, arePhotosLoading = false)
                } else {
                    it
                }
            }
        }
    }

    fun closeAlbum() = _uiState.update {
        it.copy(
            openedAlbumId = null,
            albumPhotos = emptyList(),
            arePhotosLoading = false
        )
    }

    fun exportTags(): String = repository.exportTags()

    fun importTags(contents: String): Boolean {
        val imported = repository.importTags(contents)
        if (imported) _uiState.update {
            it.copy(
                tagMap = repository.loadTagMap(),
                tagGroups = repository.loadTagGroups()
            )
        }
        return imported
    }
}
