# ProGuard rules for FugasApp
# (Puedes personalizar esto según tus necesidades)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.google.firebase.**
-keep class com.tuempresa.fugas.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
