-keepattributes *Annotation*, InnerClasses

-keep,includedescriptorclasses class com.codekage.showup.v2.**$$serializer { *; }
-keepclassmembers class com.codekage.showup.v2.** {
    *** Companion;
}
-keepclasseswithmembers class com.codekage.showup.v2.** {
    kotlinx.serialization.KSerializer serializer(...);
}
