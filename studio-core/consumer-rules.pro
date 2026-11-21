# Recipes are kotlinx-serialization classes; keep their serializers.
-keep,includedescriptorclasses class com.xenoorigin.xestudio.core.**$$serializer { *; }
-keepclassmembers class com.xenoorigin.xestudio.core.** { *** Companion; }
-keepclasseswithmembers class com.xenoorigin.xestudio.core.** { kotlinx.serialization.KSerializer serializer(...); }
