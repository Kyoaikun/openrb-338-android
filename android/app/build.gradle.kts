plugins { id("com.android.application") }
android {
 namespace = "org.openrb.mobile"
 compileSdk = 35
 defaultConfig { applicationId = "org.openrb.mobile"; minSdk = 26; targetSdk = 28; versionCode = 1; versionName = "0.1"; ndk { abiFilters += "arm64-v8a" } }
 sourceSets.getByName("main").jniLibs.srcDirs("src/main/jniLibs")
}
