# Explorer Action compatibility matrix

This document records the Explorer Action contract intentionally implemented by Readium EPUB
Reader. It is an audit boundary, not a promise that the plugin adopts every capability exposed by
the host.

## Audited contract

| Item | Value |
| --- | --- |
| Declared plugin protocol | v2 |
| Minimum host version code | 5269 |
| Maximum audited host version code | 5282 |
| Maximum audited host protocol | v22 |
| Vendored API | `libs/explorer-action-api.aar` |
| Vendored API SHA-256 | `40836C05B1D5FB532B21B916E57211C472225CF17E4CEE0E9F5FC7AA48E21B45` |
| Host source audit revision | `1db2d9b87` (AutoJs6 6.8.0, build 5282) |

The machine-readable values live in
`gradle/explorer-action-compatibility.properties`. Android builds verify the AAR digest before
compilation and expose the protocol and host boundaries through generated `BuildConfig` fields.

## Host compatibility

| Host version code | Host protocol checkpoint | Readium EPUB Reader behavior | Audit status |
| --- | --- | --- | --- |
| `< 5268` | Explorer Action unavailable | Rejected | Unsupported |
| `5268` | v1, overflow only | Rejected; primary placement needs v2 | Unsupported |
| `5269` | v2-v3, primary placement and output transactions | v2 legacy envelope | Audited |
| `5276` | v4-v21, multi-target, directory, host-session, archive, and transaction extensions | v2 legacy envelope; new capabilities are not declared | Audited |
| `5277` | v22, bounded related-file descriptors | v2 legacy envelope; related files are not declared | Audited |
| `5282` | v22, current host source (`1db2d9b87`) | v2 primary and overflow actions | Audited |
| `> 5282` | Later protocol | Accepted only when the host still honors a v2 catalog and envelope | Forward-compatible, not yet audited |

The v22 host declares `MIN_SUPPORTED_VERSION = 1`. Its catalog policy supplies the historical
single-target and Activity defaults for pre-v4 descriptors, and its launcher branches on the
descriptor's protocol version. A v2 action therefore still receives exactly two `ClipData` items:
the selected file and its parent-directory URI. It does not receive the v4 `TARGETS` list or a
host-session Binder.

## Why the plugin declares v2

An EPUB is a self-contained ZIP container: every resource the reader needs lives inside the
selected file, so the plugin never reads siblings, never writes next to the book, and never needs
host-owned archive browsing, output transactions, target replacement, Trash, or related-file
sidecars. The parent-directory URI of the v2 envelope is validated (the book must be its
descendant) but never accessed.

Protocol v2 adds primary placement without changing the v1 Binder descriptor or two-URI envelope.
The frozen v1 AAR remains valid for these descriptors; the additive primary placement value is
explicitly declared by the plugin. Both `readium-epub-reader.primary` (primary) and
`readium-epub-reader` (overflow) launch the same Activity.

## Protocol capability checkpoints

| Protocol | First audited host build | Capability relevant to the audit |
| --- | ---: | --- |
| v1 | 5268 | Single-file read-only overflow Activity |
| v2 | 5269 | Primary placement |
| v3 | 5269 | Host-owned sibling output transaction |
| v4 | 5276 | Multiple targets, directories, target Bundles, host session |
| v5-v6 | 5276 | Host-rendered archive browsing and entry opening |
| v7-v11 | 5276 | Pending-output verification, replacement, output trees, extraction, password recovery |
| v12-v14 | 5276 | Sibling/media sessions, filename charset, compound suffix matching |
| v15-v18 | 5276 | Output batches, source Trash handoff, fallback probing, replacement undo |
| v19-v20 | 5276 | Archive mutation, directory creation, frozen archive inputs |
| v21 | 5276 | Durable source recovery batches |
| v22 | 5277 | Exact related-file suffix descriptors |

## Runtime rejection rules

The execution Activity fails closed unless all of the following remain true:

- the action ID is `readium-epub-reader.primary` or `readium-epub-reader` and the protocol is exactly v2;
- `HOST_VERSION_CODE` is present and at least 5269;
- the source surface is the main file manager;
- read and prefix grants are both present;
- the data URI, first ClipData item, and display metadata describe one EPUB file (`.epub` extension or `application/epub+zip`);
- ClipData has exactly two items and the second item is the validated parent URI;
- the target remains a descendant of that parent URI.

An envelope marked v22, an additional target, a directory-style request, a missing or older host
version, or an incomplete grant is rejected rather than guessed.

## Upgrade procedure

Any intentional Explorer Action API or protocol upgrade must update all of these in one change:

1. Re-audit the vendored AAR. Replace it and update its SHA-256 (in both
   `gradle/explorer-action-compatibility.properties` and `locks/host-api-aars.lock`) only if the
   chosen protocol needs a new ABI; v2 reuses the frozen descriptor unchanged.
2. Select an explicit declared protocol and minimum host version; never derive it implicitly from
   `ExplorerActionProtocol.VERSION`.
3. Implement the matching Intent, ClipData, target Bundle, and host-session lifecycle.
4. Update this matrix, ROADMAP, all localized README sources, and plugin instructions.
5. Run the AAR gate, JVM suite, documentation check, Lint, and instrumentation on API 24 and
   API 35, followed by a cross-check against the current host catalog policy and launcher.

## Verification commands

```powershell
.\gradlew.bat :app:verifyExplorerActionApiCompatibility
.\gradlew.bat :app:testDebugUnitTest
py .python/generate_markdown.py --check
```

The host source audit checks `ExplorerActionCatalogPolicy` and `ExplorerActionLauncher`: v2 admits
primary placement and retains the two-item ClipData envelope. The plugin contract and Activity
tests cover both action IDs. The independent host appearance provider uses its own v1 contract and
a protected `org.autojs.plugin.INFO` service; it does not alter the Explorer Action envelope.
