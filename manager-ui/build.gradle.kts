// Shared manager layer, consumed by both Vector's manager app and LSPatch's manager (via the
// composite-build substitution vector:manager-ui). Holds the reusable, backend-agnostic Compose --
// the panel header, search field, ambience, theme seed, the store HTML renderer -- and the shared
// network stack: the one OkHttp client, the DoH resolver and its status section, so both apps
// resolve names the same way.
plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

android {
    namespace = "org.matrix.vector.ui"
    // Compose 1.13 beta requires API 37.1 stubs; minSdk and targetSdk are unchanged.
    compileSdkMinor = 1
    buildFeatures { compose = true }
    defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // NavKey and NavBackStack appear in this module's public signatures -- TopLevelDestination
    // carries a route, PanelBar takes one -- so a consumer cannot use the container without them.
    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.webkit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
