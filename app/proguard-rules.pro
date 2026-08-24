# dear diary — keep Room entities and Compose reflection-free release builds.
-keep class com.karoslabs.deardiary.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn org.jetbrains.annotations.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Vosk + JNA
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn java.awt.**
