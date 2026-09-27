plugins { id("com.android.application") }
android {
    namespace = "com.jepongdevxyz.phonetotv"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.jepongdevxyz.phonetotv"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity:1.10.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.zxing:core:3.5.3")
}
