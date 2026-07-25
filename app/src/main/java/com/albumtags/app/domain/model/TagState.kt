package com.albumtags.app.domain.model

data class TagState(
    val albumTags: Map<String, Set<String>> = emptyMap(),
    val groups: Map<String, Set<String>> = emptyMap()
) {
    val allTags: Set<String>
        get() = albumTags.values.flatten().toSet()
}
