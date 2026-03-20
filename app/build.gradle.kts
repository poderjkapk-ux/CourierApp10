plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.restify.courierapp"

    // ИСПРАВЛЕНИЕ: Обновляем версию для компиляции до 36,
    // так как этого требуют новые библиотеки androidx.activity и androidx.core
    compileSdk = 36

    defaultConfig {
        applicationId = "com.restify.courierapp"
        minSdk = 26

        // ИСПРАВЛЕНИЕ: Рекомендуется также обновить targetSdk до 36
        targetSdk = 35

        versionCode = 23
        versionName = "1.5.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")

    // Стандартные библиотеки (используют версии из libs.versions.toml)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Навигация
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // GPS
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Разрешения
    implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")

    // Карты OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.18")
}