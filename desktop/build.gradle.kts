plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}
kotlin { jvmToolchain(17) }
application { mainClass.set("com.andrew.music.MainKt") }
dependencies { implementation(project(":core")) }
