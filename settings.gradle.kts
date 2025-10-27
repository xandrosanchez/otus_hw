rootProject.name = "otus_hw"
include("hw01-gradle")
include("hw02-generics")
include("hw03-annotation")
include("hw04-gc")

pluginManagement {
    val dependencyManagement: String by settings
    val springframeworkBoot: String by settings
    val johnrengelmanShadow: String by settings
    val jib: String by settings
    val protobufVer: String by settings
    val spotless: String by settings
    val sonarlint: String by settings

    plugins {
        id("io.spring.dependency-management") version dependencyManagement
        id("org.springframework.boot") version springframeworkBoot
        id("com.github.johnrengelman.shadow") version johnrengelmanShadow
        id("com.google.cloud.tools.jib") version jib
        id("com.google.protobuf") version protobufVer
        id("com.diffplug.spotless") version spotless
        id("name.remal.sonarlint") version sonarlint
    }
}
include("hw05-aop")
include("hw07-patterns")
