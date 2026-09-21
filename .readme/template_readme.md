<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="{{ repo_url }}/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="{{ icon_alt }}" border="0" width="128" />
  </p>

  <p>{{ text_plugin_synopsis }}</p>

  <p>
    <a href="{{ repo_url }}/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/{{ repo_slug }}?label=Release"/></a>
    <a href="{{ repo_url }}/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/{{ repo_slug }}?color=A24232&label=Issues"/></a>
    <a href="{{ repo_url }}/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/{{ repo_slug }}?color=534BAE&label=License"/></a>
  </p>
</div>

******

### {{ h3_languages_with_ascii }}

******

{{ p_languages_all_supported_for_readme }}:

{{ placeholder_ul_languages_all_supported }}

******

### {{ h3_introduction }}

******

{{ p_introduction_what }}

{{ p_introduction_how }}

> {{ p_status }}

******

### {{ h3_screenshots }}

******

{{ p_screenshots }}:

<table>
  <tr>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/reader.png?raw=true" alt="reader" width="180" /><br/>{{ caption_reader }}</td>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/table-of-contents.png?raw=true" alt="table-of-contents" width="180" /><br/>{{ caption_table_of_contents }}</td>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/preferences.png?raw=true" alt="preferences" width="180" /><br/>{{ caption_preferences }}</td>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/search.png?raw=true" alt="search" width="180" /><br/>{{ caption_search }}</td>
  </tr>
  <tr>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/bookmarks.png?raw=true" alt="bookmarks" width="180" /><br/>{{ caption_bookmarks }}</td>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/read-aloud.png?raw=true" alt="read-aloud" width="180" /><br/>{{ caption_read_aloud }}</td>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/dark-theme.png?raw=true" alt="dark-theme" width="180" /><br/>{{ caption_dark_theme }}</td>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/sepia-theme.png?raw=true" alt="sepia-theme" width="180" /><br/>{{ caption_sepia_theme }}</td>
  </tr>
  <tr>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/vertical-ja.png?raw=true" alt="vertical-ja" width="180" /><br/>{{ caption_vertical }}</td>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/fixed-layout.png?raw=true" alt="fixed-layout" width="180" /><br/>{{ caption_fixed_layout }}</td>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/launcher.png?raw=true" alt="launcher" width="180" /><br/>{{ caption_launcher }}</td>
    <td align="center"><img src="{{ repo_url }}/blob/master/docs/images/screenshots/settings.png?raw=true" alt="settings" width="180" /><br/>{{ caption_settings }}</td>
  </tr>
</table>

******

### {{ h3_features }}

******

{{ placeholder_features }}

******

### {{ h3_installation }}

******

{{ placeholder_installation_steps }}

******

### {{ h3_usage }}

******

{{ placeholder_usage_steps }}

> {{ p_usage_note }}

******

### {{ h3_scripting }}

******

{{ p_scripting_intro }}:

{{ text_script_metadata }}:

```javascript
let book = epub.open('./books/lighthouse.epub');
console.log(book.metadata.title, '-', (book.metadata.authors || []).join(', '));
book.toc.forEach(entry => console.log(entry.title, entry.href, (entry.children || []).length, 'children'));
console.log(book.readingOrder.length, 'resources,', book.positions, 'positions');
let first = book.readingOrder[0];
console.log(book.text(first.href, { format: 'markdown' }));
book.close();
```

{{ text_script_export }}:

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

{{ text_script_reader }}:

```javascript
let session = epub.read('./books/lighthouse.epub', { progression: 0.25, preferences: { theme: 'sepia' } });
session.on('open', e => console.log('opened', e.title, 'at', e.href, '|', e.positions, 'positions'));
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('bookmark', e => console.log('bookmark', e.action, e.locator.href, '| total', session.bookmarks().length));
session.on('close', e => console.log('closed:', e.reason)); // user, host, replaced, timeout, error or overflow
setTimeout(() => session.isOpen && session.nextChapter(), 30 * 1000);
setTimeout(() => session.isOpen && session.close(), 60 * 1000);
```

{{ p_scripting_note }}

******

### {{ h3_supported_formats }}

******

{{ p_supported_formats }}:

```text
{{ supported_formats }}
```

{{ p_format_scope }}

******

### {{ h3_compatibility }}

******

{{ p_compatibility_intro }}:

{{ placeholder_compatibility_points }}

******

### {{ h3_faq }}

******

{{ placeholder_faq }}

******

### {{ h3_security }}

******

{{ p_security_intro }}

{{ placeholder_security_points }}

{{ p_security_permission }}

******

### {{ h3_plugin_interface }}

******

{{ p_plugin_interface }}:

```text
application id: {{ application_id }}
service action: {{ plugin_action }}
execute action: {{ plugin_execute_action }}
plugin id: {{ plugin_id }}
engine: {{ plugin_engine }}
variant: {{ plugin_variant }}
protocol version: {{ protocol_version }}
minimum host build: {{ required_host_build }}
audited host build: {{ audited_host_build }}
audited host protocol: {{ audited_host_protocol }}
```

{{ p_plugin_scope }}

- [{{ text_link_protocol_compatibility }}]({{ repo_url }}/blob/master/{{ protocol_compatibility_path }})

******

### {{ h3_roadmap }}

******

{{ p_roadmap }}

- [{{ text_link_roadmap }}]({{ repo_url }}/blob/master/ROADMAP.md)

******

### {{ h3_release_history }}

******

{{ placeholder_latest_release_history }}

##### {{ h5_for_more_release_history }}

* {{ placeholder_read_more_in_changelog_md }}

******

### {{ h3_build }}

******

```powershell
.\gradlew.bat :app:assembleDebug
```

{{ text_release_build }}:

```powershell
.\gradlew.bat :app:assembleRelease
```

{{ p_build_params }}.

******

### {{ h3_resource_layout }}

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

{{ p_resource_layout }}.

******

### {{ h3_license }}

******

{{ p_license }}

******

### {{ h3_links }}

******

- {{ text_link_autojs6_docs }}: {{ docs_autojs6_url }}
- {{ text_link_epub_api_docs }}: {{ docs_epub_api_url }}
- {{ text_link_format_reference }}: {{ format_reference_url }}
- {{ text_link_readium }}: {{ readium_url }}
- {{ text_link_third_party_notices }}: {{ third_party_notices_url }}
- {{ text_link_16kb }}: {{ repo_url }}/blob/master/docs/16kb.md
