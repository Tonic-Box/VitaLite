# Injector and Mixins

VitaLite applies bytecode modifications to:

- The gamepack.
- RuneLite classes.

It supports two modes:

- Development mode: run injectors and generate `patches.zip`.
- Runtime mode: apply pre generated diffs from `patches.zip` without running full transformation.

Source: `src/main/java/com/tonic/vitalite/Main.java:main`

## Development Mode: `-runInjector`

When `-runInjector` is set, VitaLite:

1. Enables patch capture.
2. Runs the gamepack injector.
3. Runs the RuneLite injector.
4. Writes an updated `patches.zip` under `src/main/resources`.

Source: `src/main/java/com/tonic/vitalite/Main.java:main`
Source: `src/main/java/com/tonic/injector/Injector.java:patch`
Source: `src/main/java/com/tonic/injector/RLInjector.java:patch`

### Output location

Patch generation writes into `src/main/resources` using a hardcoded relative path.
If you run the client from a different working directory, patch writing can fail or write to an unexpected location.

Source: `src/main/java/com/tonic/vitalite/Main.java:main` (writes to `src/main/resources`)

### Gamepack mixins

Gamepack mixins live under the `com.tonic.mixins` package and are applied by `Injector.patch()`.
Source: `src/main/java/com/tonic/injector/Injector.java` (`MIXINS = "com.tonic.mixins"`)

### RuneLite mixins

RuneLite mixins live under the `com.tonic.rlmixins` package and are applied by `RLInjector.patch()`.
Source: `src/main/java/com/tonic/injector/RLInjector.java` (`MIXINS = "com.tonic.rlmixins"`)

## Runtime Mode: `patches.zip`

When `-runInjector` is not set, VitaLite loads `patches.zip` from resources and applies diffs directly to class byte arrays.
This avoids running the full ASM pipeline on every start.

Source: `src/main/java/com/tonic/vitalite/Main.java:main`
Source: `src/main/java/com/tonic/patch/PatchApplier.java:applyPatches`

### Patches bundle location

The packaged `patches.zip` is stored as a resource under `src/main/resources/com/tonic/patches.zip`.
Source: `src/main/java/com/tonic/patch/PatchApplier.java:applyPatches`

Note: `PatchApplier` throws an error message that says `--runInjector` should be used to generate patches, but the actual boolean flag defined by `VitaLiteOptions` is `-runInjector`.
Source: `src/main/java/com/tonic/patch/PatchApplier.java:applyPatches`
Source: `base-api/src/main/java/com/tonic/VitaLiteOptions.java`

### Git history note

If you change mixins but do not regenerate and commit `patches.zip`, runtime builds can ship stale patches.
This has happened before and was corrected by updating `patches.zip`.

- Commit: `3dc1425a0141767b7a095bfde66ebb39a914ad9d` (updates `src/main/resources/com/tonic/patches.zip`)
  Source: `git show -n 1 --name-status 3dc1425a`
