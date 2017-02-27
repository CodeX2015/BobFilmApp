# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\SDK/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keepattributes EnclosingMethod
-keep class okio.** { ; }
-dontwarn okio.*
-keep class com.sun.** { ; }
-dontwarn com.sun.*
-keep class org.slf4j.** { ; }
-dontwarn org.slf4j.*

#-dontwarn org.apache.commons.**
#-dontwarn com.google.**
#-dontwarn com.j256.ormlite**
#-dontwarn org.apache.http**
#
#-keepattributes SourceFile,LineNumberTable
#-keep class com.j256.**
#-keepclassmembers class com.j256.** { *; }
#-keep enum com.j256.**
#-keepclassmembers enum com.j256.** { *; }
#-keep interface com.j256.**
#-keepclassmembers interface com.j256.** { *; }
#
#-keepattributes Signature
## GSON Library
## For using GSON @Expose annotation
#-keepattributes *Annotation*
#
## Gson specific classes
#-keep class sun.misc.Unsafe { *; }
##-keep class com.google.gson.stream.** { *; }
#
## Application classes that will be serialized/deserialized over Gson
#-keep class com.google.gson.examples.android.model.** { *; }
#
## Google Map
#-keep class com.google.android.gms.maps.** { *; }
#-keep interface com.google.android.gms.maps.** { *; }