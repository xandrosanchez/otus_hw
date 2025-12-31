plugins {
    java
    application
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("io.grpc:grpc-netty")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("ch.qos.logback:logback-classic")
    implementation("com.google.protobuf:protobuf-java")

    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

}

// Конфигурация только для grpc плагина (protoc используем системный)
configurations {
    create("grpc")
}

dependencies {
    // Указываем правильный артефакт с классификатором для Linux
    "grpc"("io.grpc:protoc-gen-grpc-java:1.72.0:windows-x86_64@exe")
}

tasks.register<Exec>("generateProto") {
    val grpcPluginPath = File("$buildDir/tmp/grpc-plugin.exe")

    doFirst {
        // Копируем grpc плагин из зависимости
        if (configurations["grpc"].isEmpty()) {
            throw GradleException("GRPC plugin configuration is empty. Check if the artifact exists.")
        }
        val grpcArtifact = configurations["grpc"].singleFile
        println("Using GRPC plugin: ${grpcArtifact.absolutePath}")
        grpcArtifact.copyTo(grpcPluginPath, overwrite = true)
        grpcPluginPath.setExecutable(true)

        // Создаем выходные директории
        file("build/generated/source/proto/main/java").mkdirs()
        file("build/generated/source/proto/main/grpc").mkdirs()
    }

    commandLine(
        "protoc",
        "-I=src/main/proto",
        "--java_out=build/generated/source/proto/main/java",
        "--plugin=protoc-gen-grpc-java=${grpcPluginPath.absolutePath}",
        "--grpc-java_out=build/generated/source/proto/main/grpc",
        "src/main/proto/numbers.proto"
    )
}

sourceSets {
    main {
        java {
            srcDirs(
                "build/generated/source/proto/main/java",
                "build/generated/source/proto/main/grpc"
            )
        }
    }
}

tasks.compileJava {
    dependsOn("generateProto")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("ru.otus.numbers.Main")
}

// Задача для запуска сервера
tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Run the gRPC server"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.otus.numbers.server.NumbersServer"
}

tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Run the gRPC client"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "ru.otus.numbers.client.NumbersClient"
}