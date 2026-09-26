-keep class com.bjwag.mensaminus.model.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }

# Retrofit rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
