package io.github.supermonster003.autojs6.plugin.readium.epub.reader.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionPolicyTest {

    @Test
    fun tagsParseWithOrWithoutThePrefixPatchAndBuildMetadata() {
        assertEquals(AppVersion(1, 0, 0), AppVersionPolicy.parse("v1.0.0"))
        assertEquals(AppVersion(1, 0, 0), AppVersionPolicy.parse(" V1.0.0 "))
        assertEquals(AppVersion(1, 2, 0), AppVersionPolicy.parse("1.2"))
        assertEquals(AppVersion(1, 0, 0, listOf("beta", "1")), AppVersionPolicy.parse("1.0.0-beta.1"))
        assertEquals(AppVersion(1, 0, 0), AppVersionPolicy.parse("1.0.0+build.45"))
        assertEquals(AppVersion(2, 3, 4, listOf("rc", "2")), AppVersionPolicy.parse("v2.3.4-rc.2+7"))
        assertEquals("1.0.0-beta.1", AppVersionPolicy.parse("v1.0.0-beta.1").toString())
        assertTrue(AppVersionPolicy.parse("1.0.0-beta.1")!!.isPreRelease)
        assertFalse(AppVersionPolicy.parse("1.0.0")!!.isPreRelease)
    }

    @Test
    fun whatIsNotAVersionParsesToNull() {
        assertNull(AppVersionPolicy.parse(null))
        assertNull(AppVersionPolicy.parse(""))
        assertNull(AppVersionPolicy.parse("latest"))
        assertNull(AppVersionPolicy.parse("1"))
        assertNull(AppVersionPolicy.parse("1.0.0.0"))
        assertNull(AppVersionPolicy.parse("1.0.0-"))
        assertNull(AppVersionPolicy.parse("1.0.0-beta..1"))
        assertNull(AppVersionPolicy.parse("v1.0.0 (build 42)"))
        assertNull(AppVersionPolicy.parse("99999999999.0.0"))
    }

    @Test
    fun orderingFollowsSemver() {
        val ordered = listOf(
            "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2",
            "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0", "1.0.1", "1.1.0", "2.0.0",
        ).map { requireNotNull(AppVersionPolicy.parse(it)) }
        for (index in 1 until ordered.size) {
            assertTrue("${ordered[index - 1]} < ${ordered[index]}", ordered[index - 1] < ordered[index])
            assertTrue("${ordered[index]} > ${ordered[index - 1]}", ordered[index] > ordered[index - 1])
        }
        assertEquals(0, AppVersionPolicy.parse("v1.0.0")!!.compareTo(AppVersionPolicy.parse("1.0.0+build.9")!!))
    }

    @Test
    fun newerMeansBothParseAndTheRemoteRanksHigher() {
        assertTrue(AppVersionPolicy.isNewer("v1.0.1", "1.0.0"))
        assertTrue(AppVersionPolicy.isNewer("v2.0.0-beta.1", "1.9.9"))
        assertFalse(AppVersionPolicy.isNewer("1.0.0", "1.0.0"))
        assertFalse(AppVersionPolicy.isNewer("1.0.0-rc.1", "1.0.0"))
        assertFalse(AppVersionPolicy.isNewer("0.9.0", "1.0.0"))
        assertFalse(AppVersionPolicy.isNewer("latest", "1.0.0"))
        assertFalse(AppVersionPolicy.isNewer("1.1.0", null))
        assertFalse(AppVersionPolicy.isNewer(null, "1.0.0"))
    }

    @Test
    fun anIgnoredVersionMatchesAsAVersionNotAsAString() {
        assertTrue(AppVersionPolicy.isIgnored("v1.1.0", "1.1.0"))
        assertTrue(AppVersionPolicy.isIgnored("1.1.0+build.3", "v1.1.0"))
        assertFalse(AppVersionPolicy.isIgnored("1.1.0", "1.0.0"))
        assertFalse(AppVersionPolicy.isIgnored("1.1.0-beta.1", "1.1.0"))
        assertFalse(AppVersionPolicy.isIgnored("1.1.0", null))
        assertFalse(AppVersionPolicy.isIgnored(null, "1.1.0"))
    }
}
