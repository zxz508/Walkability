// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false

    // Make sure that you have the Google services Gradle plugin 4.4.1+ dependency
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false



    // Add the dependency for the Crashlytics Gradle plugin


}