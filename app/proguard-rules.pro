# PDFBox-Android uses reflection over its resource loader and COS model.
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# ML Kit ships consumer rules, but keep the recognizer entry points explicit.
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# kotlinx.serialization: keep generated serializers for our own models.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.lhy.deepseekmobile.**$$serializer { *; }
-keepclassmembers class com.lhy.deepseekmobile.** {
    *** Companion;
}
-keepclasseswithmembers class com.lhy.deepseekmobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}
