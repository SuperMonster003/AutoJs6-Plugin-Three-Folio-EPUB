package io.github.supermonster003.autojs6.plugin.readium.epub.reader.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseInfoTest {

    private fun github(
        tag: String? = "v1.1.0",
        url: String? = "https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases/tag/v1.1.0",
        draft: Boolean = false,
        body: String? = "Notes",
    ): String {
        val fields = ArrayList<String>()
        tag?.let { fields += "\"tag_name\": \"$it\"" }
        url?.let { fields += "\"html_url\": \"$it\"" }
        fields += "\"name\": \"Readium EPUB Reader 1.1.0\""
        fields += "\"published_at\": \"2026-10-01T08:00:00Z\""
        fields += "\"prerelease\": false"
        fields += "\"draft\": $draft"
        body?.let { fields += "\"body\": \"$it\"" }
        fields += "\"assets\": [{\"name\": \"plugin.apk\", \"browser_download_url\": \"https://example.com/plugin.apk\"}]"
        return "{" + fields.joinToString(", ") + "}"
    }

    @Test
    fun aGitHubReleaseIsReadFieldByField() {
        val release = requireNotNull(ReleaseInfoCodec.fromGitHub(github()))
        assertEquals("v1.1.0", release.tagName)
        assertEquals("Readium EPUB Reader 1.1.0", release.name)
        assertEquals("https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases/tag/v1.1.0", release.htmlUrl)
        assertEquals("2026-10-01T08:00:00Z", release.publishedAt)
        assertFalse(release.preRelease)
        assertEquals("Notes", release.notes)
    }

    @Test
    fun draftsMissingTagsAndForeignPagesAreNotReleases() {
        assertNull(ReleaseInfoCodec.fromGitHub(github(draft = true)))
        assertNull(ReleaseInfoCodec.fromGitHub(github(tag = null)))
        assertNull(ReleaseInfoCodec.fromGitHub(github(tag = "  ")))
        assertNull(ReleaseInfoCodec.fromGitHub(github(url = null)))
        assertNull(ReleaseInfoCodec.fromGitHub(github(url = "http://github.com/SuperMonster003/x/releases")))
        assertNull(ReleaseInfoCodec.fromGitHub(github(url = "https://example.com/releases")))
        assertNull(ReleaseInfoCodec.fromGitHub(github(url = "https://github.com/")))
        assertNull(ReleaseInfoCodec.fromGitHub("not json"))
        assertNull(ReleaseInfoCodec.fromGitHub("[]"))
        assertNull(ReleaseInfoCodec.fromGitHub(""))
    }

    @Test
    fun notesAreCappedAndOptional() {
        val long = "x".repeat(ReleaseInfoCodec.MAX_NOTES_LENGTH + 500)
        assertEquals(ReleaseInfoCodec.MAX_NOTES_LENGTH, ReleaseInfoCodec.fromGitHub(github(body = long))!!.notes!!.length)
        assertNull(ReleaseInfoCodec.fromGitHub(github(body = null))!!.notes)
        assertNull(ReleaseInfoCodec.fromGitHub(github(body = ""))!!.notes)
    }

    @Test
    fun theCacheRoundTripsAndRejectsGarbage() {
        val release = requireNotNull(ReleaseInfoCodec.fromGitHub(github()))
        assertEquals(release, ReleaseInfoCodec.decode(ReleaseInfoCodec.encode(release)))
        val bare = ReleaseInfo("v2.0.0", null, "https://github.com/SuperMonster003/x/releases/tag/v2.0.0", null, true, null)
        assertEquals(bare, ReleaseInfoCodec.decode(ReleaseInfoCodec.encode(bare)))
        assertNull(ReleaseInfoCodec.decode(null))
        assertNull(ReleaseInfoCodec.decode(""))
        assertNull(ReleaseInfoCodec.decode("{\"tag_name\": 1}"))
        assertTrue(ReleaseInfoCodec.isReleasePage("https://github.com/SuperMonster003/x/releases/tag/v1"))
        assertFalse(ReleaseInfoCodec.isReleasePage("https://github.com/ evil"))
    }
}
