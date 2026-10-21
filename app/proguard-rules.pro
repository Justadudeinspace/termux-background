# JavaScript bridge methods are looked up by their declared names at runtime.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
