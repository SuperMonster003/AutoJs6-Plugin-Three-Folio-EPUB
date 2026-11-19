3-Folio EPUB is the EPUB e-book reading plugin for AutoJs6 and also works as a standalone reader. It opens EPUB 2 and EPUB 3 books with a table of contents, full-text search, bookmarks, highlights and notes, and read-aloud; scripts can also read book content or open the reader.

### Getting started

1. Make sure this plugin is enabled in AutoJs6 "Plugin center".
2. In the AutoJs6 file manager, tap an EPUB file and the book opens in the reader; you can also choose "Read EPUB" from the menu of that file.
3. Tap the left or right side of the page or press a volume key to turn pages, and tap the center to show or hide the toolbar; the same book reopens where you left off.
4. To use it without AutoJs6, open this plugin from its launcher icon and tap "Open EPUB" to pick a book.

### Main features

- **Contents and reading position**: Jump to any chapter from "Table of contents"; each book remembers where you stopped, even after it is moved or renamed, and "Start from the beginning" clears that position.
- **Reading preferences**: In "Reading preferences", adjust font size, font, spacing, alignment and page turning, and choose "Light", "Sepia", "Dark" or "Follow host"; "Import font" adds TTF or OTF fonts.
- **Vertical text and fixed layouts**: Chinese and Japanese vertical books are shown vertically, and "Text direction" can force horizontal or vertical text; fixed-layout books offer "Page spread" and pinch-to-zoom.
- **Search and bookmarks**: "Search" lists every match in the book with its context, and tapping one jumps there; the toolbar icon bookmarks the current page, and "Bookmarks" lists and deletes them.
- **Highlights and notes**: Select text and choose "Highlight" or "Add note", with five colors and an underline style; "Highlights and notes" jumps to and edits them, and "Export" saves them as a Markdown file.
- **Read aloud**: "Read aloud" reads from the current page with the system text-to-speech engine, highlighting the current sentence and turning pages; adjust speed and voice, set the "Sleep timer", and turn on "Continue in the background" to keep reading after the reader closes.
- **Links and images**: Links inside the book support going back, and footnotes appear in a popup; by default, external links show the full address first and go to the browser only after you confirm; tap an image to view it full screen.
- **Recent books and other apps**: The launcher icon opens your recent books; EPUB files shared from other apps open directly, and "Add to recent books" keeps them in the list.
- **Appearance and settings**: Language, night mode and "Theme color" follow AutoJs6 by default; "Settings" also sets page-turning and read-aloud defaults, and offers "Check for updates" and "Release history".

### Using it in scripts

The AutoJs6 `epub` module is provided by this plugin. It reads the metadata, table of contents and text of a book without opening the reader:

```js
let book = epub.open('./book.epub');
console.log(book.metadata.title, book.metadata.authors.join(', '));
book.toc.forEach(entry => console.log(entry.title));
console.log(book.text(0, { maxChars: 500 }));
book.close();
```

`epub.read()` opens the reader of this plugin and reports the reading progress to the script:

```js
let session = epub.read('./book.epub');
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('error', e => console.warn(e.toString()));
session.on('close', e => console.log('closed:', e.reason));
```

- Paths can be absolute or relative to the script working directory; `content://` and other URIs are not accepted.
- Errors are thrown as `epub.EpubError`, whose `code` gives the reason, such as `PLUGIN_DISABLED` when the plugin is disabled; check with `epub.isAvailable()` first.
- Most methods have an Async form that returns a Promise, such as `epub.openAsync()`.
- AutoJs6 starts the reader, so the system limits on starting screens from the background apply; run scripts that call `epub.read()` while AutoJs6 is visible.

### Requirements

- Android 7.0 or later, with the system WebView.
- Opening books from the file manager and the `epub` module require AutoJs6 6.8.0 or later; using it on its own from the launcher icon does not need AutoJs6.
- Read-aloud needs a text-to-speech engine with voice data for the book language.
- On Android 13 and later, the read-aloud notification controls need the notification permission; read-aloud still works if you deny it.

### FAQ

**Why "Read EPUB" is missing in the file manager**

Make sure AutoJs6 meets the version requirement and this plugin is enabled in "Plugin center"; once both are true, EPUB files offer "Read EPUB".

**Supported e-book formats**

EPUB 2 and EPUB 3 are supported, including fixed-layout books. PDF, MOBI, AZW, CBZ and audiobooks are not supported; convert them to EPUB first with a tool such as Calibre. DRM-protected books show a protection notice and cannot be read.

**When read-aloud makes no sound**

Make sure the system text-to-speech engine has voice data for the book language, and turn up the media volume. When the device has no usable engine, a notice appears after about 20 seconds.

**When this plugin uses the network**

This plugin never uploads books or reading data; it goes online only when a book references remote resources or when you tap "Check for updates". Books can contain scripts, so open only books you trust.

See the [AutoJs6 documentation](https://docs.autojs6.com/#/epub) for the full script API, and the [project homepage](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB) for more details.
