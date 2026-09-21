Use Readium EPUB Reader from the AutoJs6 file manager:

1. Install and enable the `Readium EPUB Reader` plugin.
2. Tap an `.epub` file, or open its overflow menu and choose `Readium EPUB Reader`.
3. The book opens in a reader powered by the Readium Kotlin Toolkit.

Tap the left or right third of the page or press the volume keys to turn pages; tap the middle to hide or show the toolbar. The reading position is saved per book and restored on the next open; choose `Start from the beginning` in the overflow menu to clear it.

The plugin receives temporary read access to the selected file and its parent directory through content URIs. It never receives a raw filesystem path, never copies the book to storage, and reads the EPUB container directly through the granted file descriptor.

1.1.0 is the current release; 1.0.0 was the first. The reader opens EPUB 2 and EPUB 3 books with a table of contents, remembers the reading position of every book, offers scroll mode, tap zones, volume keys and immersive mode, a preferences panel (text size, font, spacing, alignment, columns and themes that can follow the host's night mode), imported TTF / OTF fonts, vertical CJK and right-to-left books, fixed-layout books as single pages or spreads, full-text search, bookmarks, in-book links, notes and images, and read-aloud with the system text-to-speech engine. The app icon opens a launcher with the recent books and the system document picker, other apps hand over an EPUB through `ACTION_VIEW`, and the settings page covers the reader defaults, the data kept on the device and a manual update check. The `epub` script API, the host reader session and three sample scripts ship with AutoJs6 6.8.0 (build 5282). 1.1.0 adds highlights and notes (ROADMAP.md, P9): selected text can be highlighted or underlined in four colors and carry a note, the highlights are drawn on the page and listed in a panel (jump, edit, delete), and the highlights and notes of a book can be exported as Markdown through the system share sheet or saved as a file; hosts that carry EPUB contract version 2 (an AutoJs6 build newer than 5282) read them with `book.annotations()` and receive `highlight` events on the reader session, while AutoJs6 6.8.0 (build 5282) keeps working with contract version 1. The `org.autojs.plugin.EPUB` service behind it answers metadata, contents, text, resources and search to the AutoJs6 host and opens the host-driven reader session (`epub.open(path)`, `epub.read(path)`, samples under `Samples > E-books`).

Books may contain scripts and remote resources; the plugin keeps Readium's default behavior and does not block them, including plain `http://` resources. Only open books you trust.

Explorer Action v2 supports both the primary button and the overflow menu for a single file. AutoJs6 build 5269 or later is required.
