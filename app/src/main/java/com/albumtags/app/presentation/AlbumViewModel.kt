package com.albumtags.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.albumtags.app.di.AppContainer
import com.albumtags.app.domain.model.TagState
import com.albumtags.app.domain.model.AlbumPhoto
import com.albumtags.app.domain.usecase.AlbumCoverUseCases
import com.albumtags.app.domain.usecase.AlbumUseCases
import com.albumtags.app.domain.usecase.TagUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlbumViewModel(
    private val albumUseCases: AlbumUseCases,
    private val albumCoverUseCases: AlbumCoverUseCases,
    private val tagUseCases: TagUseCases
) : ViewModel() {
    private val initialTags = tagUseCases.load()
    private val _uiState = MutableStateFlow(
        AlbumUiState(
            tagMap = initialTags.albumTags,
            tagGroups = initialTags.groups
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
            val albums = albumCoverUseCases.apply(albumUseCases.loadAlbums())
            applyTagState(tagUseCases.load()) {
                it.copy(albums = albums, isLoading = false)
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
        applyTagState(tagUseCases.setAlbumTags(albumId, tags))
    }

    fun renameTag(oldName: String, newName: String): Boolean {
        val state = tagUseCases.renameTag(oldName, newName) ?: return false
        val normalized = newName.trim().removePrefix("#")
        applyTagState(state) {
            it.copy(
                selectedTags = if (oldName in it.selectedTags) {
                    (it.selectedTags - oldName) + normalized
                } else {
                    it.selectedTags
                }
            )
        }
        return true
    }

    fun deleteTag(tagName: String) {
        applyTagState(tagUseCases.deleteTag(tagName)) {
            it.copy(selectedTags = it.selectedTags - tagName)
        }
    }

    fun addTagsToAlbums(albumIds: Set<String>, tagsToAdd: Set<String>) {
        applyTagState(tagUseCases.addTagsToAlbums(albumIds, tagsToAdd))
    }

    fun saveTagGroup(
        originalName: String?,
        groupName: String,
        tags: Set<String>
    ): Boolean {
        val state = tagUseCases.saveGroup(originalName, groupName, tags)
            ?: return false
        applyTagState(state)
        return true
    }

    fun deleteTagGroup(groupName: String) {
        applyTagState(tagUseCases.deleteGroup(groupName))
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
            val photos = albumUseCases.loadPhotos(albumId)
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

    fun refreshOpenedAlbum() {
        val albumId = _uiState.value.openedAlbumId ?: return
        viewModelScope.launch {
            val albums = albumCoverUseCases.apply(albumUseCases.loadAlbums())
            val photos = albumUseCases.loadPhotos(albumId)
            _uiState.update {
                it.copy(
                    albums = albums,
                    albumPhotos = photos,
                    arePhotosLoading = false
                )
            }
        }
    }

    fun setAlbumCover(albumId: String, media: AlbumPhoto) {
        val cover = albumCoverUseCases.set(albumId, media)
        _uiState.update { state ->
            state.copy(
                albums = state.albums.map { album ->
                    if (album.bucketId == albumId) {
                        album.copy(
                            coverUri = cover.uri,
                            coverIsVideo = cover.isVideo
                        )
                    } else {
                        album
                    }
                }
            )
        }
    }

    fun exportTags(): String = tagUseCases.exportBackup()

    fun importTags(contents: String): Boolean {
        val state = tagUseCases.importBackup(contents) ?: return false
        applyTagState(state)
        return true
    }

    private fun applyTagState(
        tagState: TagState,
        transform: (AlbumUiState) -> AlbumUiState = { it }
    ) {
        _uiState.update {
            transform(it).copy(
                tagMap = tagState.albumTags,
                tagGroups = tagState.groups
            )
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AlbumViewModel(
                container.albumUseCases,
                container.albumCoverUseCases,
                container.tagUseCases
            ) as T
    }
}
