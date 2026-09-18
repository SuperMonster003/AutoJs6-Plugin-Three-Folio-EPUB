# Host protocol AAR staging

This directory contains the exact, hash-locked AutoJs6 host API distribution consumed by the
plugin. Gradle never resolves host artifacts from sibling repositories or from `mavenLocal()`.

Before any Gradle configuration, stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`)
- `explorer-action-api.aar` (host module `plugin-api/explorer-action-api`, frozen at the Explorer Action v1 descriptor that protocol v2 reuses unchanged; see `docs/explorer-action-compatibility.md`)
- `epub-api.aar` (host module `plugin-api/epub-api`, the EPUB Binder contract of roadmap P5.1; staged when that phase lands)

Record the lowercase SHA-256 of every staged artifact in `../locks/host-api-aars.lock`.
`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock
entries, and digest mismatches during configuration. The Explorer Action AAR is additionally
verified (uppercase digest) by `:app:verifyExplorerActionApiCompatibility` before `preBuild`.

When `epub-api.aar` arrives, all three artifacts must come from the same host commit; restage and
extend the lock file in the same commit.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.
