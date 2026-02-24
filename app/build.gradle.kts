plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.github.giuoaejgiusejnb.mhrsb_qurious_crafting_ocr"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.giuoaejgiusejnb.mhrsb_qurious_crafting_ocr"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        compose = true
    }
}

// 署名付きAPKウィザードやビルド成果物の名前を「アプリ名-バージョン名」に変更します。
// AGP 8.x/9.x 以降では、不具合やクラッシュの原因となる outputFileName や 
// applicationVariants の直接操作を避け、Gradle 標準の base エクステンションを使用するのが最も確実です。
base {
    // 成果物のベース名を変更します。これにより出力は「MHR-SB-傀異錬成OCR-1.0-release.apk」のようになります。
    archivesName.set("MHR-SB-傀異錬成OCR-${android.defaultConfig.versionName}")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // Compose Preview のレンダリングエラー（ClassNotFoundException: ComposeViewAdapter）を
    // 回避するため、ui-tooling を implementation で追加しています。
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
