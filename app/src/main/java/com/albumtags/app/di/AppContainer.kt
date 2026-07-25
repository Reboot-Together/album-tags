package com.albumtags.app.di

import android.content.Context
import com.albumtags.app.data.MediaStoreAlbumRepository
import com.albumtags.app.data.PreferencesTagRepository
import com.albumtags.app.domain.usecase.AddTagsToAlbumsUseCase
import com.albumtags.app.domain.usecase.AlbumUseCases
import com.albumtags.app.domain.usecase.DeleteTagGroupUseCase
import com.albumtags.app.domain.usecase.DeleteTagUseCase
import com.albumtags.app.domain.usecase.ExportTagBackupUseCase
import com.albumtags.app.domain.usecase.ImportTagBackupUseCase
import com.albumtags.app.domain.usecase.LoadAlbumPhotosUseCase
import com.albumtags.app.domain.usecase.LoadAlbumsUseCase
import com.albumtags.app.domain.usecase.LoadTagStateUseCase
import com.albumtags.app.domain.usecase.RenameTagUseCase
import com.albumtags.app.domain.usecase.SaveTagGroupUseCase
import com.albumtags.app.domain.usecase.SetAlbumTagsUseCase
import com.albumtags.app.domain.usecase.TagUseCases

class AppContainer(context: Context) {
    private val albumRepository = MediaStoreAlbumRepository(context)
    private val tagRepository = PreferencesTagRepository(context)

    val albumUseCases = AlbumUseCases(
        loadAlbums = LoadAlbumsUseCase(albumRepository),
        loadPhotos = LoadAlbumPhotosUseCase(albumRepository)
    )

    val tagUseCases = TagUseCases(
        load = LoadTagStateUseCase(tagRepository),
        setAlbumTags = SetAlbumTagsUseCase(tagRepository),
        renameTag = RenameTagUseCase(tagRepository),
        deleteTag = DeleteTagUseCase(tagRepository),
        addTagsToAlbums = AddTagsToAlbumsUseCase(tagRepository),
        saveGroup = SaveTagGroupUseCase(tagRepository),
        deleteGroup = DeleteTagGroupUseCase(tagRepository),
        exportBackup = ExportTagBackupUseCase(tagRepository),
        importBackup = ImportTagBackupUseCase(tagRepository)
    )
}
