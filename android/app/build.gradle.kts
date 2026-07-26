plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties

val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore.properties")
if (keystorePropsFile.exists()) {
    keystoreProps.load(keystorePropsFile.inputStream())
}

val apiBaseUrl = (
    System.getenv("MEDBILL_API_BASE_URL")
        ?: (project.findProperty("MEDBILL_API_BASE_URL") as String?)
        ?: "http://10.0.2.2:5199/"
).trim().let { base ->
    if (base.endsWith('/')) base else "$base/"
}

val storePath = System.getenv("ANDROID_KEYSTORE_PATH") ?: keystoreProps.getProperty("storeFile")
val storePasswordValue = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
val keyAliasValue = System.getenv("ANDROID_KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
val keyPasswordValue = System.getenv("ANDROID_KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
val hasCustomReleaseSigning = !storePath.isNullOrBlank() && !storePasswordValue.isNullOrBlank() && !keyAliasValue.isNullOrBlank() && !keyPasswordValue.isNullOrBlank()

android {
    namespace = "com.medbill.tracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.medbill.tracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "ENTRA_CLIENT_ID", "\"d5edc6d7-a111-4f8d-81bd-ea97cfe600c4\"")
        buildConfigField("String", "ENTRA_TENANT", "\"7fcb095b-344f-4c1e-8e12-bc781eb03e72\"")
        buildConfigField("String", "ENTRA_API_SCOPE", "\"api://6255300e-e5ca-4bd7-8520-a5d236304c88/user_impersonation\"")
    }

    signingConfigs {
        create("release") {
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
            }
            storePassword = storePasswordValue
            keyAlias = keyAliasValue
            keyPassword = keyPasswordValue
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = if (hasCustomReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.2")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.microsoft.identity.client:msal:5.6.0") {
        exclude(group = "com.microsoft.device.display", module = "display-mask")
    }

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
