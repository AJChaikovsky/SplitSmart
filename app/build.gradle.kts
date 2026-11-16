import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    jacoco
}

android {
    namespace = "com.splitsmart.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.splitsmart.app"
        minSdk = 26
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation("androidx.fragment:fragment:1.5.4")
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity)
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Lets Espresso wait for background work started by UiUtils (see UiUtils.IDLING).
    implementation(libs.espresso.idling.resource)

    testImplementation(libs.junit)

    // Black-box (instrumented) tests in app/src/androidTest
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.intents)
}

// Local JUnit coverage for the business layer. No emulator or Firebase account is needed.
jacoco {
    toolVersion = "0.8.12"
}

tasks.register<JacocoReport>("businessCoverageReport") {
    dependsOn("testDebugUnitTest")
    executionData(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(fileTree(layout.buildDirectory.dir(
        "intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
        include("com/splitsmart/app/business/**")
    })
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(true)
    }
}
