rootProject.name = "otus_hw"
include("hw01-gradle")

pluginManagement {
    val johnrengelmanShadow: String by settings
    plugins {
        id("com.github.johnrengelman.shadow") version johnrengelmanShadow
    }
}