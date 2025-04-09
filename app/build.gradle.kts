import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Google services plugin for Firebase
}

android {
    namespace = "com.example.cognibyte"
    compileSdk = 35 // Use latest API level

    defaultConfig {
        applicationId = "com.example.cognibyte"
        minSdk = 24
        targetSdk = 35 // Match with compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API key for OpenAI
        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${findLocalProperty("OPENAI_API_KEY")}\""
        )
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/io.netty.versions.properties"
            )
        }
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
}

dependencies {
    // Core Android libraries
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Google Calendar API
    implementation("com.google.api-client:google-api-client-android:1.34.1")
    implementation("com.google.http-client:google-http-client-gson:1.45.2")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20250115-2.0.0")

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.1")
    implementation("com.google.firebase:firebase-config-ktx")

    // gRPC dependencies to fix Firestore issues
    implementation("io.grpc:grpc-okhttp:1.42.2")
    implementation("io.grpc:grpc-protobuf-lite:1.42.2")
    implementation("io.grpc:grpc-stub:1.42.2")
    implementation("io.grpc:grpc-api:1.42.2")
    implementation("io.grpc:grpc-core:1.42.2")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    // Retrofit for networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Testing libraries
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Guava for utility functions
    implementation("com.google.guava:guava:33.3.1-android")

    // MPAndroidChart for graphs and charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    //Jdoodle api
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")

    //CodeEditor
   // implementation("com.github.Rosemoe:CodeEditor:0.23.0")


    // Ensure dependency compatibility
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.grpc") {
                useVersion("1.42.2") // Ensures compatibility with Firestore
            }
            if (requested.group == "com.android.support" && !requested.name.startsWith("multidex")) {
                useVersion("28.0.0") // Keeps compatibility with AndroidX
            }
        }
    }
}

// Apply Google services plugin
apply(plugin = "com.google.gms.google-services")

// Reads local.properties for API keys or other configurations
fun findLocalProperty(key: String): String {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { properties.load(it) }
    }
    return properties.getProperty(key, "")
}
