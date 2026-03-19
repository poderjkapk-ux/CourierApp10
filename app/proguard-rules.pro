# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ОБЯЗАТЕЛЬНО: Сохраняем системные подписи и аннотации, чтобы Retrofit не крашился
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Защищаем твои классы и модели от переименования полей
-keep class com.restify.courierapp.** { *; }
-keepclassmembers class com.restify.courierapp.** { *; }

# Правила для библиотек сети и парсинга
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class sun.misc.Unsafe { *; }
# --- ЗАЩИТА RETROFIT И COROUTINES ОТ СЖАТИЯ В РЕЛИЗЕ ---
-keepattributes Signature, InnerClasses, EnclosingMethod, Exceptions, *Annotation*

-keep class kotlin.coroutines.Continuation { *; }
-keep class kotlin.coroutines.** { *; }

-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# Гарантируем, что Gson сможет прочитать все наши модели ответов
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken