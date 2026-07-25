package com.albumtags.app.domain.repository

import com.albumtags.app.domain.model.TagState

interface TagRepository {
    fun load(): TagState
    fun save(state: TagState)
    fun exportBackup(): String
    fun importBackup(contents: String): Boolean
}
