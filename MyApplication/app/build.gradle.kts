plugins {
    alias(libs.plugins.android.application)  // 使用 Version Catalog
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}


android {
    namespace = "com.example.myapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }



}


dependencies {



    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))

    implementation("com.amap.api:search:7.3.0")

    // https://mvnrepository.com/artifact/ch.hsr/geohash
    implementation("ch.hsr:geohash:1.3.0")



    // When using the BoM, you don't specify versions in Firebase library dependencies

    // Add the dependency for the Firebase SDK for Google Analytics
    implementation("com.google.firebase:firebase-analytics")

    // TODO: Add the dependencies for any other Firebase products you want to use
    // See https://firebase.google.com/docs/android/setup#available-libraries
    // For example, add the dependencies for Firebase Authentication and Cloud Firestore
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation ("androidx.work:work-runtime:2.7.1")
    implementation ("androidx.room:room-runtime:2.4.3")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation ("com.google.android.gms:play-services-auth:20.5.0")
    implementation ("com.google.android.gms:play-services-fitness:20.0.0")
    implementation ("androidx.cardview:cardview:1.0.0")

    implementation ("commons-io:commons-io:2.11.0")
    implementation ("com.google.android.material:material:1.6.0")


    // 3D地图SDK基础功能
    // https://mvnrepository.com/artifact/com.amap.api/navi-3dmap
    // https://mvnrepository.com/artifact/com.amap.api/navi-3dmap

    implementation ("com.amap.api:3dmap:9.8.2")
    implementation("com.amap.api:map2d:5.2.0")
  //  implementation("com.amap.api:search:9.8.0")
// 定位SDK核心功能
    //implementation ("com.amap.api:location:6.4.9")



    implementation (libs.androidx.room.runtime)
   // implementation(files("libs\\navi-3dmap-9.3.0_3dmap9.3.0.jar"))
    annotationProcessor ("androidx.room:room-compiler:$2.52") // 对于 Java 项目

    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))

    // Add the dependencies for the Crashlytics and Analytics libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    implementation(libs.firebase.crashlytics.buildtools)
    annotationProcessor ("androidx.room:room-compiler:2.4.3")
    implementation ("androidx.core:core-ktx:1.10.1")
    implementation ("androidx.core:core:1.15.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

