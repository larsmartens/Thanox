apply(from = rootProject.file("gradle/libxposed-api-jar.gradle.kts"))

plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(project(":android_framework:base"))

}
