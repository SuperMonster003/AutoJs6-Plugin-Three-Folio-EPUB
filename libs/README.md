# Host protocol AAR staging

This directory contains the exact, hash-locked AutoJs6 host API distribution consumed by the
plugin. Gradle never resolves host artifacts from sibling repositories or from `mavenLocal()`.

Before any Gradle configuration, stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`)
- `explorer-action-api.aar` (host module `plugin-api/explorer-action-api`, frozen at the Explorer Action v1 descriptor that protocol v2 reuses unchanged; see `docs/explorer-action-compatibility.md`)
- `epub-api.aar` (host module `plugin-api/epub-api`, the EPUB Binder contract of roadmap P5.1, contract version 2 with baseline 1 since roadmap P9.4; release artifact with the Three Folio EPUB identity; exact source snapshot in `epub-api-sources/`)

Record the lowercase SHA-256 of every staged artifact in `../locks/host-api-aars.lock`.
`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock
entries, and digest mismatches during configuration. The Explorer Action AAR is additionally
verified (uppercase digest) by `:app:verifyExplorerActionApiCompatibility` before `preBuild`.

Provenance is recorded per artifact. The frozen common and Explorer Action contracts retain their
existing audited binaries. The current `epub-api.aar` uses the unchanged EPUB Binder protocol with
`EpubIds` updated for Three Folio EPUB. Its exact contract sources and MPL-2.0 license are retained
in `epub-api-sources/` for independent review; those files are not an additional compiled module.
The AAR SHA-256 is `a86ede7ead489c2812ab55130b6157d4d81624c120729779d05d1a996d4bf2c1`.
When any artifact is replaced, update the lock file, `THIRD_PARTY_NOTICES.md` and this list in the
same commit.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.
