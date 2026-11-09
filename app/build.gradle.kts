plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.andrew.music"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.andrew.music"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "0.4"
    }
    buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources { excludes += setOf("META-INF/DEPENDENCIES", "META-INF/INDEX.LIST", "META-INF/*.kotlin_module", "/META-INF/{AL2.0,LGPL2.1}") } }
}
dependencies {
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
