plugins {
    id("java")
}

group = "org.example"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:33.4.0-jre")
}

allprojects {
    group = "org.example"

    repositories {
        mavenCentral()
    }

    val guava: String by project
}

subprojects {
    plugins.apply(JavaPlugin::class.java)
}

tasks.test {
    useJUnitPlatform()
}

