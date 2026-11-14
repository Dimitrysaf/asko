import java.net.URI

plugins {
    // AGP 9 compiles Kotlin itself, so no separate kotlin-android plugin.
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "io.github.dimitrysaf.asko"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileSdkMinor = libs.versions.android.compileSdkMinor.get().toInt()

    defaultConfig {
        applicationId = "io.github.dimitrysaf.asko"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // The release workflow passes these from the tag (v1.2.3: 1.2.3, code 10203).
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 10103
        versionName = (findProperty("versionName") as String?) ?: "1.1.3"
    }

    // The release key, from the environment (the release workflow's secrets). Without it the
    // release APK comes out unsigned.
    signingConfigs {
        System.getenv("ASKO_KEYSTORE")?.takeIf { it.isNotBlank() }?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = System.getenv("ASKO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ASKO_KEY_ALIAS")
                keyPassword = System.getenv("ASKO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // The everyday app on the phone, so not debuggable: Android then compiles it ahead of
            // time and applies the libraries' performance profiles (startup ~1.4 s -> ~0.4 s, no
            // dropped frames while idle). Set it to true for a debugging session.
            isDebuggable = false
            // Test builds carry only 64-bit native code (ML Kit's is ~20 MB per CPU type): the
            // phone (arm64) and the emulator (x86_64). Release keeps every CPU type.
            ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        }
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // MediaPipe reads its models straight from the APK: they must stay uncompressed.
    androidResources {
        noCompress += "task"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

room {
    // Exported schemas are committed so database migrations can be checked later.
    schemaDirectory("$projectDir/schemas")
}

// The camera rep counter's MediaPipe models (Google, Apache 2.0): the heavy body model and the
// hand model. Too big for git, so downloaded once into the build and packed into the app's assets.
abstract class DownloadPoseModels : DefaultTask() {
    @get:Input
    abstract val models: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun download() {
        val dir = outputDir.get().asFile.apply { mkdirs() }
        models.get().forEach { (name, url) ->
            val file = File(dir, name)
            if (file.length() > 0) return@forEach
            val part = File(dir, "$name.part")
            URI(url).toURL().openStream().use { input -> part.outputStream().use { input.copyTo(it) } }
            part.renameTo(file)
        }
    }
}

val poseModels = tasks.register<DownloadPoseModels>("downloadPoseModels") {
    val base = "https://storage.googleapis.com/mediapipe-models"
    models.set(
        mapOf(
            "pose_landmarker_heavy.task" to "$base/pose_landmarker/pose_landmarker_heavy/float16/latest/pose_landmarker_heavy.task",
            "hand_landmarker.task" to "$base/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task",
        ),
    )
    outputDir.set(layout.buildDirectory.dir("generated/poseModels"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(poseModels, DownloadPoseModels::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.navigation.suite)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.reorderable)
    implementation(libs.androidx.health.connect)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    testImplementation(libs.junit)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.coil.network.ktor3)
    implementation(libs.ktor.client.okhttp)
}
