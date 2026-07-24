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
        AlbumUiState(tagMap = repository.loadTagMap())
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

    fun exportTags(): String = repository.exportTags()

    fun importTags(contents: String): Boolean {
        val imported = repository.importTags(contents)
        if (imported) _uiState.update { it.copy(tagMap = repository.loadTagMap()) }
        return imported
    }
}
