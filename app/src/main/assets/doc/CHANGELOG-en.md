******

### Release History

******

# v1.0.0

###### 2026/09/19

* `Hint` Development build: the roadmap phases P0 (skeleton, Readium spike, fixtures) are in progress; the first public release ships with roadmap phase P8
* `Feature` A `Readium EPUB Reader` primary button and overflow action for `.epub` files in the AutoJs6 file manager (plugin ID `readium-epub-reader`, Explorer Action v2); files the host reports as `application/zip` with the `.epub` extension are accepted too
* `Feature` Reader baseline: EPUB 2 and EPUB 3 books render through the Readium navigator, with a table of contents and confirmed external links
* `Feature` Reading position memory: the last locator of every book is saved under its content fingerprint (a quick key while opening, the full-file SHA-256 afterwards) and restored on the next open; `Start from the beginning` clears it
* `Feature` Reader chrome: book title and current chapter in the toolbar, a progress bar with synthetic position and percentage, immersive mode on a center tap, tap zones and volume keys for page turns, and a scroll mode toggle
* `Feature` Reading preferences panel: text size, font family, line height, page margins, paragraph spacing, alignment, hyphenation, publisher styles, column count and paged or scrolled layout apply immediately and are remembered across books; light, sepia and dark themes plus `Follow host`, with the toolbar and system bars recoloured to match
* `Feature` Font import: TTF and OTF files picked with the system document picker are validated (SFNT signature, `name` table, 20 MB per file, 10 fonts), stored privately under `files/fonts/<sha256>` and served to the Readium navigator as `@font-face` declarations; imported fonts appear in the preferences panel next to the built-in ones and can be deleted there
* `Feature` Vertical CJK and right-to-left books: the reading progression follows the publication (tap zones mirror for right-to-left books), Japanese / Chinese books with a right-to-left page progression render vertically through Readium CSS, a `Text direction` preference forces horizontal or vertical text, and the interface layout direction stays independent of the book
* `Feature` Fixed-layout books: `Page x of N` in the progress bar, a `Page spread` preference (auto = two pages in landscape, single page, two pages) with the text preferences hidden, and Readium's pinch zoom
* `Feature` Full-text search: a `Search` toolbar entry opens a results panel that loads 50 hits at a time (up to 500) grouped by chapter with context; tapping a hit jumps there and highlights it on the page, with previous / next in a bar above the progress bar
* `Feature` Bookmarks: a toolbar icon adds or removes a bookmark for the current page (with chapter and a text excerpt), and a `Bookmarks` panel lists them newest first with jump, delete and clear all; stored per book (up to 500) next to the reading position
* `Feature` Reading controls: tap zones can be switched off or set to left / right or top / bottom, hardware keyboards turn pages with the arrow, page and space keys, and selected text offers copy, share, web search and the system's text-processing apps
* `Feature` Links: in-book links open in the reader and the back key returns to where you were, footnotes and endnotes open in a dialog, and external links open after confirmation or, if you choose so, directly in the browser; links with other schemes are refused
* `Feature` Images: tapping an image opens it full screen with its caption
* `Feature` Read aloud: the overflow menu speaks the book from the current page with the system text-to-speech engine, highlights the sentence being spoken and turns pages along; a bar under the page and a media notification offer play / pause, previous / next sentence and stop, headset buttons work, speed, pitch, language and voice are adjustable, and reading continues with the screen off and stops when the reader closes
* `Feature` Read-aloud sleep timer (15 / 30 / 60 minutes or the end of the chapter), a keep-screen-on switch and `Continue in the background` (off by default): with it on, the voice goes on after the reader closes until the book ends or the timer fires, the notification pauses or stops it and reopens the book at the spoken sentence, and reopening the same book picks the voice up where it speaks; the reading position is saved when a background voice stops
* `Feature` Books are read in place through the granted file descriptor with positional reads; nothing is copied or extracted to storage
* `Feature` Interface, instructions, README, and changelog in 10 languages
* `Feature` Standalone launcher: the app icon opens a grid of recent books (cover, title, author, progress and last read time, up to 100) and an `Open EPUB` button that picks a book with the system document picker; picked books keep a persisted read grant so they reopen from the grid, a book whose file went away is marked unavailable, and a long press removes a book and releases its grant
* `Feature` Opening from other apps: file managers, browsers and mail apps can hand a `content://` EPUB to the reader through `ACTION_VIEW`; the book opens like any other but is not listed in the launcher unless `Add to recent books` in the overflow menu succeeds in keeping the sender's access (it refuses when it cannot); `file://` paths, requests without a read grant and directories are rejected
* `Fix` SDK XML v4 parsing warnings with AGP 9.1 and APK native alignment checks incorrectly triggered by JVM unit-test assembly tasks, using shared build plugins 1.8.3
* `Fix` A failed progress write (the book directory removed underneath the reader, storage not writable) no longer crashes the reader; that record is lost and reading continues
* `Fix` The reader no longer dies together with the host when AutoJs6 is stopped or updated while its settings provider is being read; that read just fails and the host's language / night mode are not applied
* `Dependency` Add Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
* `Dependency` Add `androidx.media3:media3-session` 1.11.0 (already pulled in by `readium-navigator-media-tts`; declared directly for the read-aloud foreground service)
