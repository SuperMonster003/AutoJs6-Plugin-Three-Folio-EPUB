package io.github.supermonster003.autojs6.plugin.readium.epub.reader.prefs

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesCodecTest {

    private fun allFields(): JSONObject = JSONObject()
        .put("backgroundColor", -1)
        .put("columnCount", "2")
        .put("fontFamily", "OpenDyslexic")
        .put("fontSize", 1.3)
        .put("fontWeight", 1.5)
        .put("hyphens", true)
        .put("imageFilter", "darken")
        .put("language", "zh-Hans")
        .put("letterSpacing", 0.2)
        .put("ligatures", false)
        .put("lineHeight", 1.4)
        .put("pageMargins", 2.0)
        .put("paragraphIndent", 1.0)
        .put("paragraphSpacing", 0.5)
        .put("publisherStyles", false)
        .put("readingProgression", "rtl")
        .put("scroll", true)
        .put("spread", "always")
        .put("textAlign", "justify")
        .put("textColor", -14540254)
        .put("textNormalization", true)
        .put("typeScale", 1.2)
        .put("verticalText", false)
        .put("wordSpacing", 0.3)

    @Test
    fun everySupportedFieldRoundTrips() {
        val stored = StoredPreferences(ThemeMode.SEPIA, allFields())

        val decoded = PreferencesCodec.decode(PreferencesCodec.encode(stored))

        assertEquals(ThemeMode.SEPIA, decoded?.themeMode)
        assertEquals(allFields().toString(), decoded?.readium?.toString())
        assertTrue(stored.sameAs(decoded))
        assertEquals(24, decoded?.readium?.length())
    }

    @Test
    fun unknownAndDerivedKeysAreDropped() {
        val clean = PreferencesCodec.sanitize(
            JSONObject().put("fontSize", 1.2).put("theme", "dark").put("bogus", 1).put("readingSpeed", 2.0),
        )

        assertEquals(setOf("fontSize"), clean.keys().asSequence().toSet())
    }

    @Test
    fun outOfRangeNumbersAreClampedIntoTheSupportedWindow() {
        val clean = PreferencesCodec.sanitize(
            JSONObject().put("fontSize", 9).put("fontWeight", 3).put("lineHeight", 0.5).put("pageMargins", -1),
        )

        assertEquals(3.0, clean.getDouble("fontSize"), 0.0)
        assertEquals(2.5, clean.getDouble("fontWeight"), 0.0)
        assertEquals(1.0, clean.getDouble("lineHeight"), 0.0)
        assertEquals(0.0, clean.getDouble("pageMargins"), 0.0)
        assertEquals(0.5, PreferencesCodec.sanitize(JSONObject().put("fontSize", 0.01)).getDouble("fontSize"), 0.0)
    }

    @Test
    fun valuesOfTheWrongTypeOrShapeAreDropped() {
        val clean = PreferencesCodec.sanitize(
            JSONObject()
                .put("fontSize", "big")
                .put("scroll", "yes")
                .put("columnCount", 1)
                .put("spread", "auto")
                .put("language", "not a tag!")
                .put("fontFamily", "   ")
                .put("backgroundColor", 1.5)
                .put("textAlign", "middle"),
        )

        assertEquals(0, clean.length())
    }

    @Test
    fun fontFamilyAndLanguageAreTrimmedAndBounded() {
        val clean = PreferencesCodec.sanitize(
            JSONObject().put("fontFamily", "  IA Writer Duospace ").put("language", " en-US "),
        )

        assertEquals("IA Writer Duospace", clean.getString("fontFamily"))
        assertEquals("en-US", clean.getString("language"))
        assertFalse(PreferencesCodec.sanitize(JSONObject().put("fontFamily", "x".repeat(121))).has("fontFamily"))
    }

    @Test
    fun corruptOrForeignFilesDecodeToNull() {
        assertNull(PreferencesCodec.decode(""))
        assertNull(PreferencesCodec.decode("not json"))
        assertNull(PreferencesCodec.decode("[]"))
        assertNull(PreferencesCodec.decode("""{"themeMode":"dark"}"""))
        assertNull(PreferencesCodec.decode("""{"format":2,"themeMode":"dark"}"""))
    }

    @Test
    fun aDamagedPayloadFallsBackFieldByField() {
        val decoded = PreferencesCodec.decode("""{"format":1,"themeMode":"neon","preferences":[1,2]}""")

        assertEquals(ThemeMode.DEFAULT, decoded?.themeMode)
        assertEquals(0, decoded?.readium?.length())
        assertTrue(decoded?.isDefault == true)
    }

    @Test
    fun theEnvelopeCarriesTheFormatAndTheMode() {
        val json = JSONObject(PreferencesCodec.encode(StoredPreferences(ThemeMode.DARK, JSONObject().put("scroll", true))))

        assertEquals(PreferencesCodec.FORMAT, json.getInt("format"))
        assertEquals("dark", json.getString("themeMode"))
        assertEquals(setOf("format", "themeMode", "preferences"), json.keys().asSequence().toSet())
        assertTrue(json.getJSONObject("preferences").getBoolean("scroll"))
    }

    @Test
    fun defaultsAreRecognized() {
        assertTrue(StoredPreferences.defaults().isDefault)
        assertFalse(StoredPreferences(ThemeMode.LIGHT, JSONObject()).isDefault)
        assertFalse(StoredPreferences(ThemeMode.HOST, JSONObject().put("fontSize", 1.1)).isDefault)
        assertFalse(StoredPreferences.defaults().sameAs(null))
        assertTrue(StoredPreferences.defaults().sameAs(StoredPreferences(ThemeMode.HOST, JSONObject())))
    }
}
