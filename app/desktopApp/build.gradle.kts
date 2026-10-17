import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Classifier nativo di webrtc-java per la macchina corrente (es. "windows-x86_64")
val webrtcNativeClassifier: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val osName = when {
        "win" in os -> "windows"
        "mac" in os -> "macos"
        else -> "linux"
    }
    val archName = if ("aarch64" in arch || "arm64" in arch) "aarch64" else "x86_64"
    "$osName-$archName"
}

dependencies {
    implementation(project(":app:sharedUI"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    // Classi Java di webrtc-java
    implementation(libs.webrtc.java)
    // Libreria nativa (DLL/so/dylib) per l'OS corrente: stessa versione del catalog + classifier
    implementation("dev.onvoid.webrtc:webrtc-java:${libs.versions.webrtc.java.get()}:$webrtcNativeClassifier")
}

compose.desktop {
    application {
        mainClass = "com.pintapontus.chatting.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.pintapontus.chatting"
            packageVersion = "1.0.0"
        }
    }
}