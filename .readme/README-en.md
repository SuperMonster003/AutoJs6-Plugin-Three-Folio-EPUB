<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="readium-epub-reader-ic-launcher" border="0" width="128" />
  </p>

  <p>Reads EPUB e-books with navigation, search, read-aloud and scripting access</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The current README.md supports the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ar.md)

******

### Introduction

******

One-tap reading: open an `.epub` file straight from the AutoJs6 file manager, either with the primary `Readium EPUB Reader` button or from the overflow menu. The reader is built on the [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0, the same open-source engine used by many commercial readers.

The plugin reads the book directly through the temporary file descriptor granted by the host. It never receives a filesystem path, never copies the book anywhere, and never extracts it to storage.

> 1.0.0 is the first release. The reader opens EPUB 2 and EPUB 3 books with a table of contents, remembers the reading position of every book, offers scroll mode, tap zones, volume keys and immersive mode, a preferences panel (text size, font, spacing, alignment, columns and themes that can follow the host's night mode), imported TTF / OTF fonts, vertical CJK and right-to-left books, fixed-layout books as single pages or spreads, full-text search, bookmarks, in-book links, notes and images, and read-aloud with the system text-to-speech engine. The app icon opens a launcher with the recent books and the system document picker, other apps hand over an EPUB through `ACTION_VIEW`, and the settings page covers the reader defaults, the data kept on the device and a manual update check. The `epub` script API, the host reader session and three sample scripts ship with AutoJs6 6.8.0 (build 5282). Highlights, notes and export are planned for 1.1.0 (ROADMAP.md, P9).

******

### Screenshots

******

Taken on a phone from the sample books generated in `docs/fixtures` (no third-party book is shown); the interface follows the AutoJs6 language, English here:

<table>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/reader.png?raw=true" alt="reader" width="180" /><br/>Reading</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/table-of-contents.png?raw=true" alt="table-of-contents" width="180" /><br/>Table of contents</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/preferences.png?raw=true" alt="preferences" width="180" /><br/>Reading preferences</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/search.png?raw=true" alt="search" width="180" /><br/>Full-text search</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/bookmarks.png?raw=true" alt="bookmarks" width="180" /><br/>Bookmarks</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/read-aloud.png?raw=true" alt="read-aloud" width="180" /><br/>Read aloud</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/dark-theme.png?raw=true" alt="dark-theme" width="180" /><br/>Dark theme</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/sepia-theme.png?raw=true" alt="sepia-theme" width="180" /><br/>Sepia theme</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/vertical-ja.png?raw=true" alt="vertical-ja" width="180" /><br/>Vertical Japanese</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/fixed-layout.png?raw=true" alt="fixed-layout" width="180" /><br/>Fixed layout</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/launcher.png?raw=true" alt="launcher" width="180" /><br/>Recent books</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/settings.png?raw=true" alt="settings" width="180" /><br/>Settings</td>
  </tr>
</table>

******

### Features

******

- Readium engine: EPUB 2 (NCX) and EPUB 3 (NAV) books render through the Readium navigator with Readium CSS, including internal links, footnotes and images.
- No copies: the EPUB container is read in place through a read-only descriptor with positional reads, so even large books open without a cache file.
- Table of contents: jump to any chapter from the toolbar; nested entries keep their depth.
- Reading position memory: the last position of every book is stored under a fingerprint of its content in the plugin's private storage, so the same book resumes even after it is moved or renamed; `Start from the beginning` clears it.
- Reader chrome: title and chapter in the toolbar, a progress bar with position and percentage, immersive mode on a center tap, tap zones and volume keys for page turns, and scroll or paginated mode.
- Reading preferences: a bottom panel sets text size, font family, line height, page margins, paragraph spacing, alignment, hyphenation, publisher styles, column count and paged or scrolled layout; changes apply immediately and are remembered for every book. Light, sepia and dark themes, or follow the host's night mode; the toolbar and system bars take the theme's colours.
- Font import: pick TTF or OTF files with the system document picker; they are validated, stored privately in the plugin (up to 10 fonts, 20 MB each), listed in the preferences panel next to the built-in fonts, served to every book and removable from the same panel.
- Vertical CJK and right-to-left books: the reading progression follows the publication, so tap zones mirror for right-to-left books; Japanese and Chinese books with a right-to-left page progression render vertically, and a `Text direction` preference forces horizontal or vertical text. The interface follows the AutoJs6 language for its own layout direction, independently of the book.
- Fixed-layout books: pages are counted as `Page x of N`, the panel offers a `Page spread` choice (automatic shows two pages side by side in landscape) and hides the text preferences that do not apply; pinch zoom and panning are Readium's own.
- Full-text search: a `Search` entry in the toolbar finds every occurrence in the book, 50 at a time (up to 500), grouped by chapter with the surrounding text; tapping a result jumps to it, highlights it on the page and offers previous / next above the progress bar.
- Bookmarks: the toolbar icon marks the current page (it fills when the page is bookmarked) and the `Bookmarks` entry lists every bookmark with its chapter, an excerpt and the time, newest first, to jump, delete or clear them all; they are stored per book (up to 500) next to the reading position.
- Gestures, keys and links: tap zones (off, left / right or top / bottom), volume keys, hardware keyboard keys and a text-selection menu with copy, share, web search and text-processing apps; in-book links keep a back stack, notes open in a dialog, external links open after confirmation or directly, and a tapped image opens full screen.
- Read aloud: `Read aloud` in the overflow menu speaks the book from the current page with the system text-to-speech engine, highlights the sentence being spoken and turns the pages along; a bar under the page and a media notification offer play / pause, previous / next sentence and stop, headset buttons work, speed, pitch, language and voice are adjustable, reading continues with the screen off and stops when the reader closes unless `Continue in the background` is on, and the read-aloud settings add a sleep timer (15 / 30 / 60 minutes or the end of the chapter) and a keep-screen-on switch.
- External links: tapping an `http` or `https` link shows the full address and opens the system browser only after confirmation.
- Standalone launcher: the app icon opens a grid of recent books with cover, title, author, progress and last read time, plus an `Open EPUB` button that picks a book with the system document picker; the reader is the same one the file manager opens.
- Opens from other apps: a file manager, browser or mail app can hand over a `content://` EPUB through `ACTION_VIEW`; `Add to recent books` in the overflow menu keeps it in the launcher when the sender allows lasting access.
- Settings page with theme, page turning, read-aloud defaults, links and data management, plus the release history and a manual update check that only asks GitHub when you tap it
- Scripting service: an `org.autojs.plugin.EPUB` Binder service lets the AutoJs6 host read a book without opening the reader (metadata, table of contents, reading order, chapter text as plain text or light Markdown, resources, full-text search and position counts), with bounded requests, at most 8 books open at a time and access limited to the host; AutoJs6 6.8.0 exposes it to scripts as the `epub` module (see Scripting below).
- Host reader session: the AutoJs6 host can open the reader on a book through the `org.autojs.plugin.EPUB` service and follow it (position, bookmark and close events), jump to a locator, href or progression, turn pages or chapters and set the reading preferences; the reader starts only through the host's own explicit launch with a one-time session token, and closing the session leaves the reader open for the user unless the host asks to finish it.
- Host integration: menus and dialogs follow the AutoJs6 language and dark mode; the Explorer Action envelope is validated strictly before any content is opened.
- Multilingual: interface, instructions, README, and changelog are available in 10 languages.

******

### Installation

******

1. From the plugin center: open `Plugins` in AutoJs6, pick `Readium EPUB Reader` from the official list and tap install; the plugin center downloads the signed APK, installs it and lets you enable the plugin.
2. From GitHub: download the APK from the [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) page (the file name carries a CRC32 and `SHA256SUMS` lists the checksum), install it, then enable the plugin in the plugin center.
3. Requirements: AutoJs6 internal build 5269 or later for the file manager entry, AutoJs6 6.8.0 (build 5282) or later for the `epub` script API, Android 7.0 or later, and a system WebView.

******

### How to Use

******

1. Download the latest plugin APK from the [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) page and install it on your device.
2. Open the AutoJs6 plugin center and enable the `Readium EPUB Reader` plugin.
3. In the AutoJs6 file manager, tap an `.epub` file, or open its overflow menu (more actions) and choose `Readium EPUB Reader`.
4. Use the table of contents button in the toolbar to jump between chapters and the preferences button to adjust the text and the theme; tap the left or right third of the page or press the volume keys to turn pages, and tap the middle to hide or show the toolbar; press Back to close the reader, the position is remembered.
5. Without the file manager, tap the app icon: the launcher lists your recent books, and `Open EPUB` picks a book with the system document picker; books opened this way stay in the list with their cover and progress.
6. From another app (a file manager, a browser's downloads, a mail attachment), choose this reader for an `.epub` file; the book opens the same way, and `Add to recent books` in the overflow menu keeps it in the launcher's list when the sending app allows lasting access.
7. Open `Settings` from the launcher menu or the reader's overflow menu to set the theme, page turning, read-aloud defaults and links, clear the data the plugin keeps, read the release history or check for updates (the check contacts GitHub only when you tap it).
8. From a script: `epub.open(path)` reads a book (metadata, contents, text, search) and `epub.read(path)` opens this reader and reports its position; see the Scripting section below and the `E-books` samples in AutoJs6.

> If the plugin does not appear in the plugin center, update AutoJs6 to a recent version first (internal build 5269 or later). Explorer Action v2 supports both the primary button and the overflow menu for a single file, using temporary read grants for the document and its parent directory.

******

### Scripting

******

AutoJs6 6.8.0 adds the global `epub` module (alias `$epub`), which this plugin serves: read a book without opening the reader, or open the reader and follow it from a script. AutoJs6 ships three sample scripts under `Samples > E-books`, and the reference is in the [AutoJs6 documentation](https://docs.autojs6.com/#/epub):

Metadata, table of contents and chapter text:

```javascript
let book = epub.open('./books/lighthouse.epub');
console.log(book.metadata.title, '-', (book.metadata.authors || []).join(', '));
book.toc.forEach(entry => console.log(entry.title, entry.href, (entry.children || []).length, 'children'));
console.log(book.readingOrder.length, 'resources,', book.positions, 'positions');
let first = book.readingOrder[0];
console.log(book.text(first.href, { format: 'markdown' }));
book.close();
```

Cover, search and the convenience functions:

```javascript
let path = './books/lighthouse.epub';
let book = epub.open(path);
try {
    console.log('cover saved to', book.cover(files.cwd(), { overwrite: true }));
} catch (e) {
    if (!(e instanceof epub.EpubError) || e.code !== 'RESOURCE_NOT_FOUND') throw e;
    console.log('this book has no cover');
}
book.search('lighthouse', { limit: 20 }).forEach(hit => console.log(hit.title || hit.href, ':', hit.text));
files.write('./lighthouse.txt', book.textAll({ maxChars: 2 * 1024 * 1024 }));
book.close();
console.log(epub.metadata(path).language); // the convenience functions open and close the book themselves
epub.tocAsync(path).then(toc => console.log(toc.length, 'entries'));
```

Open the reader and follow the position:

```javascript
let session = epub.read('./books/lighthouse.epub', { progression: 0.25, preferences: { theme: 'sepia' } });
session.on('open', e => console.log('opened', e.title, 'at', e.href, '|', e.positions, 'positions'));
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('bookmark', e => console.log('bookmark', e.action, e.locator.href, '| total', session.bookmarks().length));
session.on('close', e => console.log('closed:', e.reason)); // user, host, replaced, timeout, error or overflow
setTimeout(() => session.isOpen && session.nextChapter(), 30 * 1000);
setTimeout(() => session.isOpen && session.close(), 60 * 1000);
```

Paths are relative to the script's working directory or absolute (`content://` URIs are not accepted). Every call throws an `EpubError` with a `code` (`PLUGIN_UNAVAILABLE`, `NOT_EPUB`, `ENCRYPTED`, `PARSE_FAILED`, `TIMEOUT`, ...) when the plugin or the book cannot be used, `epub.isAvailable()` tells whether the plugin is installed and enabled, and every method has an `*Async` twin that returns a Promise.

******

### Supported Formats

******

The plugin recognizes the following filename extension, plus extensionless files the host explicitly marks as `application/epub+zip`:

```text
epub
```

Only EPUB is supported: reflowable and fixed-layout books in EPUB 2 or EPUB 3. Comic archives (CBZ), audiobooks, PDF and LCP-protected books are out of scope; a book marked as LCP-encrypted is reported as unreadable instead of rendering garbage.

******

### Compatibility

******

What the plugin needs, what it was verified on and what stays out of scope:

- AutoJs6: internal build 5269 or later for the file manager entry (Explorer Action v2); the `epub` script API, the host reader session and the sample scripts need AutoJs6 6.8.0 (build 5282), the last host build audited for this release.
- Android 7.0 (API 24) up to Android 16 (API 37, the target); the pages render in the device's WebView, so an up-to-date Android System WebView or Chrome is expected. The plugin has no native libraries and runs unchanged on 16 KB page devices.
- Verified on AVD API 24 / 33 / 36 / 37, Sony Xperia XZ1 Compact (Android 9), Redmi 12C (Android 13, MIUI) and Xiaomi Pad 6 (Android 15, service side); the device x scenario matrix, its deviations and the WebView versions are in `docs/dev/compatibility-matrix.md`.
- Books: EPUB 2 and EPUB 3, reflowable and fixed layout, vertical CJK and right-to-left. DRM-protected books (LCP, Adobe ADEPT) are reported as protected and never rendered; PDF, MOBI, AZW, CBZ and audiobooks are out of scope.
- Read-aloud needs a text-to-speech engine with voice data for the book's language (Google Speech Services, the vendor's engine or any other installed engine); a device without a usable engine reports it after about 20 seconds instead of staying silent.
- Size and performance: the release APK is about 3.3 MB; a 200 MB book opens in 1 to 3 seconds on a 2017 phone, position and fingerprint computation never delay the first page, and books with thousands of chapters take noticeably longer to open (`docs/dev/performance-baseline.md`).

******

### FAQ

******

#### How is my reading position remembered?

The last position of each book is saved in the plugin's private storage under a fingerprint of the file content, never under its path, so reopening the same book resumes where you left off. Choose `Start from the beginning` in the overflow menu to clear it.

#### Can I change the font, text size or theme?

Yes. Open the preferences panel from the toolbar to set the text size, the font family (publisher default, serif, sans-serif, monospace or the accessibility fonts bundled with Readium), line height, margins, spacing, alignment, columns and the theme (light, sepia, dark or follow the host). Tap `Import font` in the panel to add your own TTF or OTF files; they are stored privately in the plugin and can be removed with `Manage fonts`.

#### Does this plugin upload my books anywhere?

No. The plugin has no server of its own. Network access is only used when a book itself references remote resources, and for the manual update check on the settings page, which asks the GitHub Releases API over HTTPS only when you tap it and never downloads anything.

#### Why are PDF, MOBI or AZW files not supported?

The reader is built on the Readium toolkit, which renders EPUB only. PDF needs a different renderer, and MOBI / AZW are Amazon formats without an open rendering engine; convert them to EPUB with a tool such as Calibre first. Comic archives (CBZ) and audiobooks are out of scope as well.

#### Read aloud makes no sound

The plugin speaks through the text-to-speech engine chosen in the system settings (`Accessibility > Text-to-speech output`). Check that an engine with voice data for the book's language is installed, that the media volume is up and that no other app holds the audio focus (a call or music pauses read-aloud). A book in a language the engine cannot speak uses the engine's default voice, and a device without a usable engine shows a message after about 20 seconds.

#### An imported font does not show in the book

Publisher styles may pin their own fonts: switch `Publisher styles` off in the preferences panel and select the imported font again. Only TTF and OTF files are accepted (font collections, `.ttc`, are refused with their own message), a font applies to the body text, and headings the publisher styled with a specific family keep it.

#### How are vertical Japanese or Chinese books handled?

A book whose spine declares a right-to-left page progression and a Japanese or Chinese language renders vertically and turns pages from right to left; the `Text direction` preference forces horizontal or vertical text for any book. The interface keeps the AutoJs6 language direction, so an English interface stays left to right while the book reads right to left.

******

### Permissions and Security

******

The plugin keeps Readium's default behavior for book content: scripts and remote resources inside a book are not removed or blocked, including plain `http://` resources. Only open books you trust.

- Least privilege: the plugin only receives the temporary content URI read permission granted by the host, never sees filesystem paths, and never writes the book to storage.
- Strict envelope: the Explorer Action request must carry exactly one EPUB target, its parent directory, a matching protocol version, a supported host build and both read grants; anything else is rejected before the file is opened.
- Separate door for other apps: `ACTION_VIEW` is served by its own exported activity that accepts only `content://` documents with a read grant (never `file://`, never a directory), while the Explorer Action activity stays behind the AutoJs6 plugin permission; the sending app's grant is kept only when you choose `Add to recent books`.
- Guarded scripting service: the `org.autojs.plugin.EPUB` service is exported behind the plugin permission, serves only the AutoJs6 host package with a matching signature, checks every request against fixed ceilings (href length, text window, search pages, options size, 8 open books, 64 MB per resource) and never starts the reader from the background: a reader session only hands the host a one-time token, the host starts the reader Activity itself, an unclaimed session closes after 60 s and a wrong token opens nothing.
- Update check on demand only: the settings page asks the GitHub Releases API over HTTPS when you tap `Check for updates` (at most once a day, no redirects, a small answer cap), shows what it found and opens the release page in your browser; the plugin never downloads or installs anything by itself.
- Bounded parsing: a malformed container (not a ZIP, missing `container.xml`, missing package document, path traversal in the manifest) fails with an error message instead of a crash.
- External links are shown in full and opened in the system browser only after confirmation; schemes other than `http` and `https` are refused.
- Reading data stays local: positions are keyed by a content fingerprint and no file path or name is written to storage.
- Read-aloud runs in a non-exported media playback service that lives only while a voice reads and stops when you stop it, the book ends or the reader closes (or, with `Continue in the background` on, when you stop it from the notification); the text goes to the text-to-speech engine chosen in the system settings, and the plugin holds no wake lock.

The manifest requests the network permission, the AutoJs6 plugin permission and, for read-aloud, the foreground service permissions (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) plus `POST_NOTIFICATIONS` on Android 13+, which is asked for once when read-aloud starts and can be refused (reading then continues without the notification controls). AndroidX also contributes a package-scoped signature permission that protects non-exported dynamic receivers; it grants no access to device data. No storage, media, camera, location, accessibility or overlay permission is requested.

******

### Plugin Interface

******

The following information is for developers; the host discovers and executes the plugin with these identities:

```text
application id: io.github.supermonster003.autojs6.plugin.readium.epub.reader
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: readium-epub-reader
engine: explorer-action
variant: default
protocol version: 2
minimum host build: 5269
audited host build: 5282
audited host protocol: 22
```

Explorer Action v2 supports both the primary button and the overflow menu for a single file, using temporary read grants for the document and its parent directory. AutoJs6 build 5269 or later is required.

- [View the Explorer Action compatibility matrix](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/explorer-action-compatibility.md)

******

### Roadmap

******

ROADMAP.md tracks every milestone as a checkable list with acceptance criteria and evidence: P0 to P8 (the reader, preferences and fonts, search and bookmarks, read-aloud, the standalone entry, the host contract, the `epub` script API, robustness and the 1.0.0 release gate) are checked; P9 (highlights, notes and export, 1.1.0) is planned. Unchecked items describe plans rather than shipped capabilities. Feedback via Issues is welcome.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.0.0

_2026/09/19_

- `Hint` Development build: the roadmap phases P0 (skeleton, Readium spike, fixtures) are in progress; the first public release ships with roadmap phase P8
- `Feature` A `Readium EPUB Reader` primary button and overflow action for `.epub` files in the AutoJs6 file manager (plugin ID `readium-epub-reader`, Explorer Action v2); files the host reports as `application/zip` with the `.epub` extension are accepted too
- `Feature` Reader baseline: EPUB 2 and EPUB 3 books render through the Readium navigator, with a table of contents and confirmed external links
- `Feature` Reading position memory: the last locator of every book is saved under its content fingerprint (a quick key while opening, the full-file SHA-256 afterwards) and restored on the next open; `Start from the beginning` clears it
- `Feature` Reader chrome: book title and current chapter in the toolbar, a progress bar with synthetic position and percentage, immersive mode on a center tap, tap zones and volume keys for page turns, and a scroll mode toggle
- `Feature` Reading preferences panel: text size, font family, line height, page margins, paragraph spacing, alignment, hyphenation, publisher styles, column count and paged or scrolled layout apply immediately and are remembered across books; light, sepia and dark themes plus `Follow host`, with the toolbar and system bars recoloured to match
- `Feature` Font import: TTF and OTF files picked with the system document picker are validated (SFNT signature, `name` table, 20 MB per file, 10 fonts), stored privately under `files/fonts/<sha256>` and served to the Readium navigator as `@font-face` declarations; imported fonts appear in the preferences panel next to the built-in ones and can be deleted there
- `Feature` Vertical CJK and right-to-left books: the reading progression follows the publication (tap zones mirror for right-to-left books), Japanese / Chinese books with a right-to-left page progression render vertically through Readium CSS, a `Text direction` preference forces horizontal or vertical text, and the interface layout direction stays independent of the book
- `Feature` Fixed-layout books: `Page x of N` in the progress bar, a `Page spread` preference (auto = two pages in landscape, single page, two pages) with the text preferences hidden, and Readium's pinch zoom
- `Feature` Full-text search: a `Search` toolbar entry opens a results panel that loads 50 hits at a time (up to 500) grouped by chapter with context; tapping a hit jumps there and highlights it on the page, with previous / next in a bar above the progress bar
- `Feature` Bookmarks: a toolbar icon adds or removes a bookmark for the current page (with chapter and a text excerpt), and a `Bookmarks` panel lists them newest first with jump, delete and clear all; stored per book (up to 500) next to the reading position
- `Feature` Reading controls: tap zones can be switched off or set to left / right or top / bottom, hardware keyboards turn pages with the arrow, page and space keys, and selected text offers copy, share, web search and the system's text-processing apps
- `Feature` Links: in-book links open in the reader and the back key returns to where you were, footnotes and endnotes open in a dialog, and external links open after confirmation or, if you choose so, directly in the browser; links with other schemes are refused
- `Feature` Images: tapping an image opens it full screen with its caption
- `Feature` Read aloud: the overflow menu speaks the book from the current page with the system text-to-speech engine, highlights the sentence being spoken and turns pages along; a bar under the page and a media notification offer play / pause, previous / next sentence and stop, headset buttons work, speed, pitch, language and voice are adjustable, and reading continues with the screen off and stops when the reader closes
- `Feature` Read-aloud sleep timer (15 / 30 / 60 minutes or the end of the chapter), a keep-screen-on switch and `Continue in the background` (off by default): with it on, the voice goes on after the reader closes until the book ends or the timer fires, the notification pauses or stops it and reopens the book at the spoken sentence, and reopening the same book picks the voice up where it speaks; the reading position is saved when a background voice stops
- `Feature` Books are read in place through the granted file descriptor with positional reads; nothing is copied or extracted to storage
- `Feature` Interface, instructions, README, and changelog in 10 languages
- `Feature` Standalone launcher: the app icon opens a grid of recent books (cover, title, author, progress and last read time, up to 100) and an `Open EPUB` button that picks a book with the system document picker; picked books keep a persisted read grant so they reopen from the grid, a book whose file went away is marked unavailable, and a long press removes a book and releases its grant
- `Feature` Opening from other apps: file managers, browsers and mail apps can hand a `content://` EPUB to the reader through `ACTION_VIEW`; the book opens like any other but is not listed in the launcher unless `Add to recent books` in the overflow menu succeeds in keeping the sender's access (it refuses when it cannot); `file://` paths, requests without a read grant and directories are rejected
- `Feature` Settings page and release history: the launcher menu and the reader overflow open a settings page for the theme, tap zones, volume keys, read-aloud speed, pitch and default sleep timer, external links and the data the plugin keeps (reading positions, recent books, imported fonts, preferences, each cleared after a confirmation), with an about section, the bundled release history and a manual update check that asks GitHub only when tapped and opens the release page in the browser (no download, `Ignore this version` remembered)
- `Feature` EPUB capability service for the AutoJs6 host (roadmap P5.2): the `org.autojs.plugin.EPUB` Binder service opens a book from the host's read-only descriptor and answers metadata, table of contents, reading order, chapter text (plain text or light Markdown, paged), resources through a pipe, full-text search and position counts; at most 8 books are open at a time, an idle book closes after 5 minutes, every request is bounds-checked and only the AutoJs6 host may call the service
- `Feature` Host reader session over the EPUB contract (roadmap P5.3): `openReader` opens the book, mints a one-time session token and leaves the launch to the host, which starts the reader Activity explicitly with that token; the session then reports `open`, `progress` (at most every 500 ms), `bookmark`, `error` and `close` events with one generation and a strictly increasing sequence, takes `goTo` (locator, href or progression), `navigate` (page or chapter), `setPreferences` (the contract's preference subset; unknown keys are reported, not applied), `getBookmarks` and `getState`, replaces an earlier session, closes after 60 s when no reader claims it, and a host `close` leaves the reader open unless it asks to finish
- `Fix` SDK XML v4 parsing warnings with AGP 9.1 and APK native alignment checks incorrectly triggered by JVM unit-test assembly tasks, using shared build plugins 1.8.3
- `Fix` A failed progress write (the book directory removed underneath the reader, storage not writable) no longer crashes the reader; that record is lost and reading continues
- `Fix` The reader no longer dies together with the host when AutoJs6 is stopped or updated while its settings provider is being read; that read just fails and the host's language / night mode are not applied
- `Fix` A host session whose launch intent reaches a reader already on top of its task (single-top delivery, for example after a script left the reader open) opens in a fresh reader instead of waiting unclaimed until the 60 s timeout; the previous reader ends like a replaced one (roadmap P6.2)
- `Fix` A book whose NCX / OPF XML is truncated or otherwise malformed now fails closed: the service answers `PARSE_FAILED` and the reader shows its open-failed panel, instead of the `INTERNAL` code or a crash from the `AssertionError` that Readium's XML parser throws (roadmap P7.1)
- `Fix` One `search` page is bounded to 50 s on the plugin side and answers `TIMEOUT` when the query only matches late in a huge book (50 000 resources), so the Binder thread no longer stays busy past the host's own 60 s call timeout (roadmap P7.1)
- `Fix` Every page WebView Readium creates in the reader now carries a boundary on top of Readium's own settings: no file-system or content-provider access and both file-URL cross-origin switches off, while JavaScript stays on for Readium (roadmap D6); the review of the WebView, container and component boundaries is recorded in `docs/dev/security-boundaries.md` (roadmap P7.2)
- `Fix` If the reader process dies from an uncaught exception, the current reading position is written to disk first, synchronously, before the system's own crash handling runs; the plugin itself writes no logs and plants no Timber tree, so no book title, path or text ever reaches logcat (roadmap P7.7)
- `Fix` Picking a TrueType or OpenType collection (`.ttc` / `.otc`) as a reading font now reports that font collections are not supported, instead of calling the file not a font; found while running the device x scenario compatibility matrix recorded in `docs/dev/compatibility-matrix.md` (roadmap P7.3)
- `Fix` Accessibility: the four sliders of the reading preferences panel (text size, page margins, line height, paragraph spacing) now carry labels a screen reader can speak, and the reader toolbar grows with large system font sizes instead of clipping the chapter subtitle; an instrumentation audit over labels, 48 dp touch targets, 1.3x font scale, night mode, forced RTL, keyboard paging and landscape backs this (roadmap P7.6)
- `Fix` Read-aloud no longer waits forever for a speech engine that never finishes initializing (the API 24 emulator's Google TTS without voice data does exactly that): after 20 seconds the reader reports that no engine is usable and returns to idle, and a session that still arrives later is closed (roadmap P7.3)
- `Improvement` Release APK size: the DiViNa player Readium ships in its navigator assets (427 KB, never used by an EPUB reader) is left out of the merged assets and the blanket keep rule for the plugin package is gone, so R8 shrinks the plugin's own classes too; the release APK goes from 3,922,786 B after P5 to 3,328,220 B (roadmap P7.5, details in `docs/dev/release-size.md`)
- `Dependency` Add Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
- `Dependency` Add `androidx.media3:media3-session` 1.11.0 (already pulled in by `readium-navigator-media-tts`; declared directly for the read-aloud foreground service)
- `Dependency` Add `org.jsoup:jsoup` 1.23.2 (already pulled in by `readium-shared`; declared directly for the chapter text extraction of the EPUB service)

##### For more release history

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Release build:

```powershell
.\gradlew.bat :app:assembleRelease
```

Build parameters come from `version.properties`. The current minimum SDK is 24 and the target SDK is 37.

******

### Localization and Docs Generation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` localizes plugin metadata and the reader UI, while `plugin_instruction.md` provides host-visible usage instructions. For README and changelog, always edit the JSON sources under `.readme/` and `.changelog/`, then run `py .python/generate_markdown.py` to regenerate; generated files are never edited by hand. Run `py .python/generate_markdown.py --check` to verify that sources and artifacts are in sync.

******

### License and Third-Party Notices

******

The plugin is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE). The Readium Kotlin Toolkit (BSD 3-Clause), AndroidX Media3 and Jsoup, the AutoJs6 contract libraries and the other components shipped in the APK are listed with their versions, checksums and licenses in [Third-Party Notices](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/THIRD_PARTY_NOTICES.md).

******

### Links

******

- AutoJs6 documentation: https://docs.autojs6.com
- `epub` script API reference: https://docs.autojs6.com/#/epub
- EPUB 3.3 specification: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/THIRD_PARTY_NOTICES.md
- 16 KB page alignment and build verification: https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md
