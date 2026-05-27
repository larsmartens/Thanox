import org.gradle.api.artifacts.VersionCatalogsExtension

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val libxposedApi = libs.findLibrary("libxposed-api").get()

val libxposedApiAar by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val extractLibxposedApi by tasks.registering(Copy::class) {
    from({ zipTree(libxposedApiAar.singleFile) }) {
        include("classes.jar")
    }
    into(layout.buildDirectory.dir("generated/libxposed-api"))
    rename("classes.jar", "libxposed-api.jar")
}

dependencies {
    add(libxposedApiAar.name, libxposedApi)
    add("compileOnly", files(layout.buildDirectory.file("generated/libxposed-api/libxposed-api.jar")))
}

tasks.matching { it.name == "compileKotlin" || it.name == "compileJava" }.configureEach {
    dependsOn(extractLibxposedApi)
}