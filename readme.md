Depends on the "smath" and "querznbt" libraries.
McVolume is under heavy development so if those libraries aren't directly bundled inside the JAR:
Add `jitpack.io` to your repositories in your `build.gradle` (or `build.gradle.kts`).
```
repositories {
    maven("https://jitpack.io")
}
```
Add the two dependencies:
```
dependencies {
    implementation("com.github.sloimayyy:smath:<smath version>")
    implementation("com.github.Querz:NBT:<querz nbt version>")
}
```

**Currently under heavy development, methods will be renamed or their implementations may change from versions to versions.
There is no documentation yet.**
