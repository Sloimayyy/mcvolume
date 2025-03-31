Depends on the "smath" and "querznbt" libraries.
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