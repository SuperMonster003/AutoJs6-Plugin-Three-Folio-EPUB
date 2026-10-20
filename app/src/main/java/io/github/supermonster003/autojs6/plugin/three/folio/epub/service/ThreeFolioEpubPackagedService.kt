package io.github.supermonster003.autojs6.plugin.three.folio.epub.service

/** The normal-permission entry keeps returned books and reader controls scoped to their caller uid. */
class ThreeFolioEpubPackagedService : ThreeFolioEpubPluginService() {
    override val packagedEntry: Boolean = true
}
