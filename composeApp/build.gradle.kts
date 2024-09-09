import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutLibraries)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.markdown)
            implementation(libs.jsoup)
            implementation(libs.filekit.core)
            implementation(libs.filekit.compose)
            implementation(libs.kermit)
            implementation(libs.aboutLibraries.core)
            implementation(libs.aboutLibraries.compose)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        desktopTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.junit)
        }
    }
}

compose.desktop {
    application {
        mainClass = "net.ruffpug.markdownstatichtmlkun.MarkdownStaticHtmlKun"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "net.ruffpug.markdownstatichtmlkun"
            packageVersion = "1.0.0"
        }
    }
}
