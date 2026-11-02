plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.remotepulse"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.remotepulse"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Rename the generated debug APK.
androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("Remote_Pulse_app.apk")
        }
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.github.bumptech.glide:glide:5.0.5")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}