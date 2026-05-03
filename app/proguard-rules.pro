-keepattributes *Annotation*, InnerClasses

-keep,includedescriptorclasses class com.codekage.showup.**$$serializer { *; }
-keepclassmembers class com.codekage.showup.** {
    *** Companion;
}
-keepclasseswithmembers class com.codekage.showup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
