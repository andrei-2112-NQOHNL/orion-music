plugins { id("org.jetbrains.kotlin.jvm") }
kotlin { jvmToolchain(17) }
dependencies {
    api("com.github.TeamNewPipe:NewPipeExtractor:v0.27.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.TeamNewPipe:nanojson:e9d656ddb49a412a5a0a5d5ef20ca7ef09549996")
}
