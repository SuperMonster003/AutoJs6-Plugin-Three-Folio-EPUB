# Host protocol AAR staging

This directory contains the exact, hash-locked AutoJs6 host API distribution consumed by the
plugin. Gradle never resolves host artifacts from sibling repositories or from `mavenLocal()`.

Before any Gradle configuration, stage the audited **release** artifacts named exactly:

- `common-plugin-api.aar` (host module `plugin-api/common-plugin-api`: `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`)
- `explorer-action-api.aar` (host module `plugin-api/explorer-action-api`, frozen at the Explorer Action v1 descriptor that protocol v2 reuses unchanged; see `docs/explorer-action-compatibility.md`)
- `epub-api.aar` (host module `plugin-api/epub-api`, the EPUB Binder contract of roadmap P5.1, contract version 1; release build of host commit `261417e90`)

Record the lowercase SHA-256 of every staged artifact in `../locks/host-api-aars.lock`.
`app/build.gradle.kts` rejects missing files, debug artifacts, placeholder hashes, extra lock
entries, and digest mismatches during configuration. The Explorer Action AAR is additionally
verified (uppercase digest) by `:app:verifyExplorerActionApiCompatibility` before `preBuild`.

Provenance is recorded per artifact, not per host commit: `explorer-action-api.aar` is the frozen v1
descriptor and `common-plugin-api.aar` the shared vendored artifact (both audited against host
source `1db2d9b87`, whose `plugin-api/common-plugin-api` and `plugin-api/explorer-action-api` sources
are unchanged at the commit that built `epub-api.aar`); `epub-api.aar` is the release build of the host
commit named above. Rebuilding the two older modules today yields different digests (newer AAR
metadata and two host-side settings classes the plugin does not use), so they are not restaged.
When any artifact is replaced, update the lock file, `THIRD_PARTY_NOTICES.md` and this list in the
same commit.

Do not commit locally assembled debug AARs or rename debug outputs to bypass this policy.
