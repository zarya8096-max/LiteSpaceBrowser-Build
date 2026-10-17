-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.litespace.browser.**$$serializer { *; }
-keepclassmembers class com.litespace.browser.** {
    *** Companion;
}
-keepclasseswithmembers class com.litespace.browser.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.litespace.browser.core.webview.LiteSpaceNotificationBridge { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
