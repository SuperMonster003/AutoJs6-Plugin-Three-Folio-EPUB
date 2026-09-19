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
* `Feature` Books are read in place through the granted file descriptor with positional reads; nothing is copied or extracted to storage
* `Feature` Interface, instructions, README, and changelog in 10 languages
* `Dependency` Add Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
