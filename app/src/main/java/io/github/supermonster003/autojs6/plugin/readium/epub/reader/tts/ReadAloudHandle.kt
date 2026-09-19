package io.github.supermonster003.autojs6.plugin.readium.epub.reader.tts

import io.github.supermonster003.autojs6.plugin.readium.epub.reader.book.PfdResource
import org.readium.r2.shared.publication.Publication

/** A book the reader left behind for the voice (roadmap D26): the publication and the descriptor it reads from. */
internal class OrphanBook(val publication: Publication, val resource: PfdResource?) {
    fun close() {
        runCatching { publication.close() }
        runCatching { resource?.close() }
    }
}

/**
 * A read-aloud session handed over between the reader and the foreground service (roadmap D26)
 * together with the book it speaks from. The book is closed with the session unless a reader
 * takes it back with [takeOrphan] to show it again.
 */
internal class ReadAloudHandle(val session: TtsSession, orphan: OrphanBook?) {

    var orphan: OrphanBook? = orphan
        private set

    fun takeOrphan(): OrphanBook? = orphan.also { orphan = null }

    fun close() {
        session.close()
        orphan?.close()
        orphan = null
    }
}
