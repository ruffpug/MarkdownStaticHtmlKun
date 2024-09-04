plugins {
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktlintGradle)
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        android.set(false)
        coloredOutput.set(true)
        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(false)
        verbose.set(true)

        //  自動生成されたファイル (例: Res.kt) を対象外とする。
        filter {
            exclude { entry ->
                entry.file.toString().contains("generated")
            }
        }

        //  Checkstyle形式でレポートする。
        reporters { reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE) }
    }
}
