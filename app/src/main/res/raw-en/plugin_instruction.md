Use Readium EPUB Reader from the AutoJs6 file manager:

1. Install and enable the `Readium EPUB Reader` plugin.
2. Tap an `.epub` file, or open its overflow menu and choose `Readium EPUB Reader`.
3. The book opens in a reader powered by the Readium Kotlin Toolkit.

Tap the left or right third of the page or press the volume keys to turn pages; tap the middle to hide or show the toolbar. The reading position is saved per book and restored on the next open; choose `Start from the beginning` in the overflow menu to clear it.

The plugin receives temporary read access to the selected file and its parent directory through content URIs. It never receives a raw filesystem path, never copies the book to storage, and reads the EPUB container directly through the granted file descriptor.

Current stage: the reader shows the book with Readium's default settings, provides a table of contents, remembers the reading position of every book, offers scroll mode, tap zones, volume keys and immersive mode, and has a preferences panel for text size, font, spacing, alignment, columns and themes, and imports your own TTF or OTF fonts and handles vertical CJK and right-to-left books. Bookmarks, search, read-aloud, fixed layout, the standalone launcher entry and the `epub` scripting API are tracked in ROADMAP.md and arrive in later builds.

Books may contain scripts and remote resources; the plugin keeps Readium's default behavior and does not block them, including plain `http://` resources. Only open books you trust.

Explorer Action v2 supports both the primary button and the overflow menu for a single file. AutoJs6 build 5269 or later is required.
