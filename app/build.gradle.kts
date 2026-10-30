import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

// API de BackVynils publicada en Replit, la que usan release y el CI
val publishedApiBaseUrl = "https://back-vynils-api--vargasjuanp.replit.app/api/"

// En debug se puede usar otra API, por ejemplo la de Docker: -PapiBaseUrl=... o apiBaseUrl en local.properties
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}
val debugApiBaseUrl = (providers.gradleProperty("apiBaseUrl").orNull ?: localProperties.getProperty("apiBaseUrl"))
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    // Retrofit exige que la URL base termine en «/»
    ?.let { if (it.endsWith("/")) it else "$it/" }
    ?: publishedApiBaseUrl

android {
    namespace = "com.equipo2.vinilos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.equipo2.vinilos"
        minSdk = 21
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$publishedApiBaseUrl\"")
    }

    signingConfigs {
        // Llave compartida para que los APK de cualquier computador o workflow se actualicen entre sí
        getByName("debug") {
            storeFile = file("debug.keystore")
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions {
        animationsDisabled = true
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    lint {
        // Versiones fijadas en las últimas compatibles con Android 5.0
        disable += setOf("GradleDependency", "NewerVersionAvailable")
        // Reglas de accesibilidad que detienen la compilación
        error += setOf(
            "ContentDescription", "HardcodedText", "LabelFor", "SpUsage", "SmallSp",
            "ClickableViewAccessibility", "KeyboardInaccessibleWidget",
        )
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

// Cada prueba unitaria aparece en el log con su resultado, y al final los totales
tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun beforeTest(test: TestDescriptor) {}
        override fun afterTest(test: TestDescriptor, result: TestResult) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                println("Total: ${result.testCount}  Pasaron: ${result.successfulTestCount}  Fallaron: ${result.failedTestCount}  Omitidas: ${result.skippedTestCount}")
            }
        }
    })
}

tasks.withType<com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask>().configureEach {
    additionalTestOutputDir.set(layout.buildDirectory.dir("outputs/connected_android_test_additional_output"))
}
