# Keep metadata used by Room, Media3 and Kotlin reflection.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-keep class * extends androidx.media3.session.MediaSessionService { *; }
-dontwarn androidx.media3.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.larkplaye.player.core.db.** { *; }

# App models are serialized by the backup format and read from MediaStore.
-keep class com.larkplaye.player.core.model.** { *; }

# Home-screen widget entry points are instantiated by the system.
-keep class com.larkplaye.player.widget.PlayerWidgetProvider { *; }
-keep class com.larkplaye.player.widget.WidgetUpdateService { *; }
