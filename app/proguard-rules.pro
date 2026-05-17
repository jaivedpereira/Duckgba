# Keep emulator core classes (used for save state serialization)
-keep class eu.rekawek.coffeegb.core.** { *; }
-keep class com.duckgba.core.** { *; }
-keepclassmembers class eu.rekawek.coffeegb.core.** {
    *;
}
