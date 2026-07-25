package com.albumtags.app.domain.usecase

import com.albumtags.app.domain.model.TagState
import com.albumtags.app.domain.repository.TagRepository

class LoadTagStateUseCase(private val repository: TagRepository) {
    operator fun invoke(): TagState = repository.load()
}

class SetAlbumTagsUseCase(private val repository: TagRepository) {
    operator fun invoke(albumId: String, tags: Set<String>): TagState {
        val current = repository.load()
        val updatedTags = current.albumTags.toMutableMap().apply {
            if (tags.isEmpty()) remove(albumId) else put(albumId, tags)
        }
        return current.copy(albumTags = updatedTags).also(repository::save)
    }
}

class RenameTagUseCase(private val repository: TagRepository) {
    operator fun invoke(oldName: String, requestedName: String): TagState? {
        val newName = requestedName.trim().removePrefix("#")
        if (newName.isEmpty() || newName == oldName) return null
        val current = repository.load()
        val albumTags = current.albumTags.mapValues { (_, tags) ->
            if (oldName in tags) (tags - oldName) + newName else tags
        }
        val groups = current.groups.mapValues { (_, tags) ->
            if (oldName in tags) (tags - oldName) + newName else tags
        }
        return TagState(albumTags, groups).also(repository::save)
    }
}

class DeleteTagUseCase(private val repository: TagRepository) {
    operator fun invoke(tagName: String): TagState {
        val current = repository.load()
        val albumTags = current.albumTags.mapValues { (_, tags) -> tags - tagName }
            .filterValues { it.isNotEmpty() }
        val groups = current.groups.mapValues { (_, tags) -> tags - tagName }
            .filterValues { it.isNotEmpty() }
        return TagState(albumTags, groups).also(repository::save)
    }
}

class AddTagsToAlbumsUseCase(private val repository: TagRepository) {
    operator fun invoke(albumIds: Set<String>, tagsToAdd: Set<String>): TagState {
        val current = repository.load()
        if (albumIds.isEmpty() || tagsToAdd.isEmpty()) return current
        val albumTags = current.albumTags.toMutableMap()
        albumIds.forEach { albumId ->
            albumTags[albumId] = albumTags[albumId].orEmpty() + tagsToAdd
        }
        return current.copy(albumTags = albumTags).also(repository::save)
    }
}

class SaveTagGroupUseCase(private val repository: TagRepository) {
    operator fun invoke(
        originalName: String?,
        requestedName: String,
        requestedTags: Set<String>
    ): TagState? {
        val current = repository.load()
        val name = requestedName.trim()
        val tags = requestedTags.intersect(current.allTags)
        if (name.isEmpty() || tags.isEmpty()) return null
        if (name != originalName && name in current.groups) return null

        val groups = current.groups.toMutableMap()
        originalName?.let(groups::remove)
        groups.keys.toList().forEach { groupName ->
            groups[groupName] = groups[groupName].orEmpty() - tags
            if (groups[groupName].isNullOrEmpty()) groups.remove(groupName)
        }
        groups[name] = tags
        return current.copy(groups = groups).also(repository::save)
    }
}

class DeleteTagGroupUseCase(private val repository: TagRepository) {
    operator fun invoke(groupName: String): TagState {
        val current = repository.load()
        return current.copy(groups = current.groups - groupName).also(repository::save)
    }
}

class ExportTagBackupUseCase(private val repository: TagRepository) {
    operator fun invoke(): String = repository.exportBackup()
}

class ImportTagBackupUseCase(private val repository: TagRepository) {
    operator fun invoke(contents: String): TagState? =
        if (repository.importBackup(contents)) repository.load() else null
}

data class TagUseCases(
    val load: LoadTagStateUseCase,
    val setAlbumTags: SetAlbumTagsUseCase,
    val renameTag: RenameTagUseCase,
    val deleteTag: DeleteTagUseCase,
    val addTagsToAlbums: AddTagsToAlbumsUseCase,
    val saveGroup: SaveTagGroupUseCase,
    val deleteGroup: DeleteTagGroupUseCase,
    val exportBackup: ExportTagBackupUseCase,
    val importBackup: ImportTagBackupUseCase
)
