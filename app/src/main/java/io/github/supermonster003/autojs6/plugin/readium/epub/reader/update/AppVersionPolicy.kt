package io.github.supermonster003.autojs6.plugin.readium.epub.reader.update

/**
 * A semantic version (roadmap P4.3): `major.minor[.patch][-pre.release][+build]`, with or without
 * a leading `v`. Ordering follows semver: numeric parts, then a pre-release ranks below the plain
 * release, pre-release identifiers compare numerically when both are numbers and by ASCII
 * otherwise, and build metadata never counts.
 */
internal data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String> = emptyList(),
) : Comparable<AppVersion> {

    val isPreRelease: Boolean get() = preRelease.isNotEmpty()

    override fun compareTo(other: AppVersion): Int {
        compareValues(major, other.major).takeIf { it != 0 }?.let { return it }
        compareValues(minor, other.minor).takeIf { it != 0 }?.let { return it }
        compareValues(patch, other.patch).takeIf { it != 0 }?.let { return it }
        if (preRelease.isEmpty() || other.preRelease.isEmpty()) {
            // A release outranks its own pre-releases; two releases are equal.
            return compareValues(preRelease.isEmpty(), other.preRelease.isEmpty())
        }
        for (index in 0 until minOf(preRelease.size, other.preRelease.size)) {
            val mine = preRelease[index]
            val theirs = other.preRelease[index]
            val mineNumber = mine.toIntOrNull()
            val theirsNumber = theirs.toIntOrNull()
            val step = when {
                mineNumber != null && theirsNumber != null -> mineNumber.compareTo(theirsNumber)
                mineNumber != null -> -1 // numeric identifiers rank below alphanumeric ones
                theirsNumber != null -> 1
                else -> mine.compareTo(theirs)
            }
            if (step != 0) return step
        }
        return compareValues(preRelease.size, other.preRelease.size)
    }

    override fun toString(): String =
        "$major.$minor.$patch" + if (preRelease.isEmpty()) "" else "-" + preRelease.joinToString(".")
}

/**
 * Decides whether a GitHub release tag names something newer than the installed build and whether
 * the user asked to skip it (roadmap D28: manual checks only, ignore per version).
 */
internal object AppVersionPolicy {

    private val PATTERN = Regex("""^[vV]?(\d+)\.(\d+)(?:\.(\d+))?(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")

    /** The parsed version, or null for anything that is not a version (`latest`, empty, `1.0.0.0`, words). */
    fun parse(text: String?): AppVersion? {
        val match = PATTERN.matchEntire(text?.trim().orEmpty()) ?: return null
        val (major, minor, patch, pre) = match.destructured
        val identifiers = pre.takeIf { it.isNotEmpty() }?.split('.').orEmpty()
        if (identifiers.any { it.isEmpty() }) return null
        return AppVersion(major.toIntOrNull() ?: return null, minor.toIntOrNull() ?: return null, patch.toIntOrNull() ?: 0, identifiers)
    }

    /** True when both sides parse and the remote tag ranks above the installed version name. */
    fun isNewer(remoteTag: String?, installedVersionName: String?): Boolean {
        val remote = parse(remoteTag) ?: return false
        val installed = parse(installedVersionName) ?: return false
        return remote > installed
    }

    /** True when the user chose to skip exactly this version (compared as versions, so `v1.1.0` and `1.1.0` match). */
    fun isIgnored(remoteTag: String?, ignoredTag: String?): Boolean {
        val remote = parse(remoteTag) ?: return false
        val ignored = parse(ignoredTag) ?: return false
        return remote.compareTo(ignored) == 0
    }
}
