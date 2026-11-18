# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.hanecuzdan.app.**$$serializer { *; }
-keepclassmembers class com.hanecuzdan.app.** { *** Companion; }
-keepclasseswithmembers class com.hanecuzdan.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Room & Firestore
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn okio.**
-dontwarn javax.annotation.**
