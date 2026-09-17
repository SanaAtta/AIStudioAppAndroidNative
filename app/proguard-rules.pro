# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
# AI Content Report models & repository
-keep class com.aiartgenerator.imagegenerator.videogenerator.model.report.** { *; }
-keepnames class com.aiartgenerator.imagegenerator.videogenerator.model.report.** { *; }

# Google Play Services Ads
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }

# Google User Messaging Platform (UMP)
-keep class com.google.android.ump.** { *; }
-keep interface com.google.android.ump.** { *; }

# Project Ads package & Remote Config models
-keep class com.aiartgenerator.imagegenerator.videogenerator.ads.** { *; }
-keepnames class com.aiartgenerator.imagegenerator.videogenerator.ads.** { *; }