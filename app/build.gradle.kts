import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Senhas de assinatura ficam em keystore.properties (FORA do git).
// Sem esse arquivo o app ainda compila (gera um APK release não
// assinado), mas só o APK assinado instala no celular dos pais.
val arquivoAssinatura = rootProject.file("keystore.properties")
val propsAssinatura = Properties().apply {
    if (arquivoAssinatura.exists()) load(FileInputStream(arquivoAssinatura))
}

android {
    namespace = "com.familia.controledegastos"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.familia.controledegastos"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (arquivoAssinatura.exists()) {
                storeFile = rootProject.file(propsAssinatura.getProperty("storeFile"))
                storePassword = propsAssinatura.getProperty("storePassword")
                keyAlias = propsAssinatura.getProperty("keyAlias")
                keyPassword = propsAssinatura.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: encolhe e ofusca o código. proguard-rules.pro protege o
            // que é usado por reflexão (modelos do Firestore, o worker).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (arquivoAssinatura.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}