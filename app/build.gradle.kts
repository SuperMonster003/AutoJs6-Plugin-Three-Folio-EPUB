import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import java.security.MessageDigest
import java.util.Properties

plugins {
    id("io.github.supermonster003.autojs6-native-alignment")
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.readium.epub.reader"

val buildTypeDebug = "debug"
val buildTypeRelease = "release"

// ---------------------------------------------------------------------------------------------
// Host API AARs: staged in libs/ and pinned by locks/host-api-aars.lock (roadmap D2 / AGENTS 5.2).
// ---------------------------------------------------------------------------------------------

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun Properties.requiredValue(key: String): String =
    getProperty(key)?.trim()?.takeIf(String::isNotEmpty)
        ?: error("Missing required lock value: $key")

fun File.loadUniqueLock(): Properties {
    val lock = Properties()
    useLines(Charsets.UTF_8) { lines ->
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#') || trimmed.startsWith('!')) {
                return@forEachIndexed
            }
            val separator = trimmed.indexOf('=')
            require(separator > 0) { "Malformed lock line ${index + 1} in $name" }
            val key = trimmed.substring(0, separator).trim()
            val value = trimmed.substring(separator + 1).trim()
            require(!lock.containsKey(key)) { "Duplicate lock key in $name: $key" }
            lock.setProperty(key, value)
        }
    }
    return lock
}

val sha256Pattern = Regex("[0-9a-f]{64}")
val hostApiLockFile = rootProject.file("locks/host-api-aars.lock")
require(hostApiLockFile.isFile) {
    "Missing host API lock: ${hostApiLockFile.relativeTo(rootProject.projectDir)}"
}
val hostApiLock = hostApiLockFile.loadUniqueLock()
val hostApiIds = listOf("common-plugin-api", "explorer-action-api", "epub-api")
val expectedHostApiLockKeys = setOf("format") + hostApiIds.flatMap { id -> listOf("$id.file", "$id.sha256") }
require(hostApiLock.stringPropertyNames() == expectedHostApiLockKeys) {
    "Host API AAR lock must contain exactly these keys: ${expectedHostApiLockKeys.sorted()}"
}
require(hostApiLock.requiredValue("format") == "1") {
    "Unsupported host API AAR lock format"
}

fun lockedHostApiAar(id: String): File {
    val fileName = hostApiLock.requiredValue("$id.file")
    val expectedSha256 = hostApiLock.requiredValue("$id.sha256").lowercase()
    require(fileName == File(fileName).name && fileName.endsWith(".aar")) {
        "Invalid $id.file in ${hostApiLockFile.name}"
    }
    require(!fileName.endsWith("-debug.aar")) {
        "Debug AARs are forbidden: $fileName"
    }
    require(sha256Pattern.matches(expectedSha256)) {
        "Invalid $id.sha256 in ${hostApiLockFile.name}"
    }
    val aar = rootProject.file("libs/$fileName")
    require(aar.isFile) { "Missing host API AAR: libs/$fileName" }
    val actualSha256 = aar.sha256()
    require(actualSha256 == expectedSha256) {
        "Host API AAR digest mismatch for libs/$fileName: expected $expectedSha256, actual $actualSha256"
    }
    return aar
}

val commonPluginApiAar = lockedHostApiAar("common-plugin-api")
val explorerActionApiAar = lockedHostApiAar("explorer-action-api")
val epubApiAar = lockedHostApiAar("epub-api")

// ---------------------------------------------------------------------------------------------
// Explorer Action audit record (roadmap D9), shared by the catalog, policy, tests and docs.
// ---------------------------------------------------------------------------------------------

val explorerActionCompatibilityFile =
    rootProject.file("gradle/explorer-action-compatibility.properties")
val explorerActionCompatibility = Properties().apply {
    explorerActionCompatibilityFile.inputStream().use(::load)
}

fun explorerActionCompatibilityProperty(name: String): String =
    explorerActionCompatibility.getProperty(name)
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: error("Missing Explorer Action compatibility property: $name")

val explorerActionProtocolVersion =
    explorerActionCompatibilityProperty("declaredProtocolVersion").toInt()
val explorerActionMinimumHostVersionCode =
    explorerActionCompatibilityProperty("minimumHostVersionCode").toLong()
val explorerActionMaximumAuditedHostVersionCode =
    explorerActionCompatibilityProperty("maximumAuditedHostVersionCode").toLong()
val explorerActionMaximumAuditedHostProtocolVersion =
    explorerActionCompatibilityProperty("maximumAuditedHostProtocolVersion").toInt()
val explorerActionApiSha256 =
    explorerActionCompatibilityProperty("explorerActionApiSha256").uppercase()

require(explorerActionApiSha256.lowercase() == hostApiLock.requiredValue("explorer-action-api.sha256").lowercase()) {
    "gradle/explorer-action-compatibility.properties and locks/host-api-aars.lock disagree about explorer-action-api.aar"
}

android {
    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    defaultConfig {
        applicationId = globalApplicationId
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("int", "EXPLORER_ACTION_PROTOCOL_VERSION", explorerActionProtocolVersion.toString())
        buildConfigField("String", "READIUM_VERSION", "\"${libs.versions.readium.get()}\"")
        buildConfigField(
            "long",
            "EXPLORER_ACTION_MINIMUM_HOST_VERSION_CODE",
            "${explorerActionMinimumHostVersionCode}L",
        )
        buildConfigField(
            "long",
            "EXPLORER_ACTION_MAXIMUM_AUDITED_HOST_VERSION_CODE",
            "${explorerActionMaximumAuditedHostVersionCode}L",
        )
        buildConfigField(
            "int",
            "EXPLORER_ACTION_MAXIMUM_AUDITED_HOST_PROTOCOL_VERSION",
            explorerActionMaximumAuditedHostProtocolVersion.toString(),
        )

        // Identity values read by the official plugin index (roadmap D1 / D10); the Kotlin
        // constants in ReadiumEpubReaderPlugin must stay identical.
        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_engine", "explorer-action")
        resValue("string", "plugin_id", "readium-epub-reader")
        resValue("string", "plugin_requires_host_version", explorerActionMinimumHostVersionCode.toString())
        resValue("string", "plugin_variant", "default")
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))
    }

    lint {
        abortOnError = true
    }

    androidResources {
        // Roadmap P7.5: Readium ships its DiViNa player (427 KB) in the navigator assets; the plugin renders
        // EPUB only, so the merged assets leave that directory out. The other patterns are AGP's own
        // defaults, repeated because a non-empty list replaces them.
        ignoreAssetsPatterns.addAll(
            listOf("!.svn", "!.git", "!.ds_store", "!*.scc", ".*", "<dir>_*", "!CVS", "!thumbs.db", "!picasa.ini", "!*~", "<dir>divina"),
        )
        // Roadmap P7.5: the libraries translate their strings into ~85 locales; only the ten the plugin itself
        // ships (locales_config.xml, plus the zh-rCN spelling the libraries use for Simplified Chinese) stay in
        // the resource table.
        localeFilters.addAll(listOf("ar", "en", "es", "fr", "ja", "ko", "ru", "zh", "zh-rCN", "zh-rHK", "zh-rTW"))
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        resValues = true
        viewBinding = true
    }

    compileOptions {
        // java.time (kotlinx-datetime through Readium) on API 24 and 25 devices.
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets.named("main") {
        kotlin.directories += "src/main/java"
    }

    // Instrumentation tests read the generated fixtures (roadmap P0.3) straight from docs/fixtures.
    sourceSets.named("androidTest") {
        assets.directories += rootProject.file("docs/fixtures").path
    }

    packaging {
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
    }

    bundle {
        language.enableSplit = false
        density.enableSplit = false
        abi.enableSplit = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    "${rootProject.name}-v$version.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)

    implementation(files(commonPluginApiAar))
    implementation(files(explorerActionApiAar))
    implementation(files(epubApiAar))

    // Readium Kotlin Toolkit full stack (roadmap D2): parsing, rendering, TTS.
    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)
    implementation(libs.readium.navigator)
    implementation(libs.readium.navigator.media.tts)
    // A runtime dependency of readium-navigator-media-tts; the read-aloud foreground service subclasses
    // media3's MediaSessionService, so the plugin declares it directly (roadmap P3).
    implementation(libs.media3.session)
    // Jsoup is already on the runtime classpath through readium-shared; declared directly for the XHTML
    // block walk behind the chapter text of the EPUB service (roadmap P5.2 / D30).
    implementation(libs.jsoup)

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    implementation(libs.webkit)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)

    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.runner)
}

tasks {
    val verifyExplorerActionApiCompatibility = register("verifyExplorerActionApiCompatibility") {
        description = "Verifies the audited Explorer Action v1 AAR digest"
        group = "verification"
        val apiAar = rootProject.file("libs/explorer-action-api.aar")
        inputs.file(apiAar)
        inputs.file(explorerActionCompatibilityFile)

        doLast {
            val actual = apiAar.sha256().uppercase()
            check(actual == explorerActionApiSha256) {
                "Explorer Action API AAR digest changed: expected $explorerActionApiSha256, actual $actual. " +
                    "Audit the protocol and update the compatibility matrix before accepting a new AAR."
            }
            println("Explorer Action API compatibility OK: protocol v$explorerActionProtocolVersion, SHA-256 $actual")
        }
    }

    named("preBuild").configure {
        dependsOn(verifyExplorerActionApiCompatibility)
    }

    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    register<Copy>("appendDigestToReleasedFiles") {
        description = "Appends CRC32 digest to released APK files"
        dependsOn("assembleRelease")

        val ext = utils.FILE_EXTENSION_APK
        val src = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease")
        val dst = file("${buildTypeRelease}s")

        from(src)
        into(dst)
        include("*.$ext")
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.FAIL

        eachFile {
            val digest = utils.digestCRC32(file)
            relativePath = RelativePath(true, "${name.removeSuffix(".$ext")}-$digest.$ext")
        }

        doLast { println("Destination: $dst") }
    }
}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}

// Reject accidental native dependencies on every ABI (roadmap D7).
nativeAlignment { expectNoNativeLibraries.set(true) }

// Fail before collection when credentials, keystore or the actual APK set are incomplete.
val verifySignedReleaseArtifacts = tasks.register("verifySignedReleaseArtifacts") {
    group = "verification"
    dependsOn("assembleRelease")
    doLast {
        val signing = android.buildTypes.getByName("release").signingConfig
        check(signing != null && signing.storeFile?.isFile == true &&
            !signing.storePassword.isNullOrBlank() && !signing.keyAlias.isNullOrBlank() &&
            !signing.keyPassword.isNullOrBlank()) { "Release signing configuration is missing or incomplete" }
        val directory = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val apks = directory.listFiles { file -> file.isFile && file.extension == "apk" }.orEmpty()
        check(apks.map { it.name }.toSet() == setOf("${rootProject.name}-v${versions.appVersionName}.apk")) {
            "Unexpected release APK set: ${apks.map { it.name }.sorted()}"
        }
        val buildTools = androidComponents.sdkComponents.sdkDirectory.get().asFile
            .resolve("build-tools/${android.buildToolsVersion}")
        val signerJar = buildTools.resolve("lib/apksigner.jar")
        check(signerJar.isFile) { "Android SDK apksigner is unavailable" }
        val result = providers.exec {
            commandLine("java", "-jar", signerJar.absolutePath, "verify", apks.single().absolutePath)
            isIgnoreExitValue = true
        }.result.get()
        check(result.exitValue == 0) { "Release APK signature verification failed" }
    }
}
tasks.named("appendDigestToReleasedFiles") { dependsOn(verifySignedReleaseArtifacts) }
tasks.matching { it.name == "prepareReleaseArtifacts" }.configureEach {
    dependsOn(verifySignedReleaseArtifacts)
}
