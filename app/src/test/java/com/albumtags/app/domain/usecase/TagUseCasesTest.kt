package com.albumtags.app.domain.usecase

import com.albumtags.app.domain.model.TagState
import com.albumtags.app.domain.repository.TagRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagUseCasesTest {
    @Test
    fun `rename tag updates albums and groups`() {
        val repository = FakeTagRepository(
            TagState(
                albumTags = mapOf("album" to setOf("여행")),
                groups = mapOf("분류" to setOf("여행")),
            )
        )

        RenameTagUseCase(repository)("여행", "국내여행")

        assertEquals(setOf("국내여행"), repository.state.albumTags["album"])
        assertEquals(setOf("국내여행"), repository.state.groups["분류"])
    }

    @Test
    fun `delete tag removes it everywhere`() {
        val repository = FakeTagRepository(
            TagState(
                albumTags = mapOf("a" to setOf("가족", "여행")),
                groups = mapOf("사람" to setOf("가족")),
            )
        )

        DeleteTagUseCase(repository)("가족")

        assertEquals(setOf("여행"), repository.state.albumTags["a"])
        assertFalse(repository.state.groups.values.flatten().contains("가족"))
    }

    @Test
    fun `saving group moves tags out of other groups`() {
        val repository = FakeTagRepository(
            TagState(
                albumTags = mapOf("a" to setOf("클라이밍", "배그")),
                groups = mapOf("기존" to setOf("클라이밍", "배그")),
            )
        )

        SaveTagGroupUseCase(repository)(null, "취미", setOf("클라이밍"))

        assertEquals(setOf("배그"), repository.state.groups["기존"])
        assertEquals(setOf("클라이밍"), repository.state.groups["취미"])
        assertTrue(repository.state.groups.values.flatten().count { it == "클라이밍" } == 1)
    }
}

private class FakeTagRepository(initial: TagState) : TagRepository {
    var state = initial

    override fun load(): TagState = state

    override fun save(state: TagState) {
        this.state = state
    }

    override fun exportBackup(): String = ""

    override fun importBackup(contents: String): Boolean = true
}
