// Импортируем необходимые классы для работы с плагинами
import org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

// Блок определения используемых плагинов
plugins {
    // Плагин поддержки IntelliJ IDEA для корректной работы IDE
    idea

    // Плагин для управления зависимостями
    id("io.spring.dependency-management")

    // Spring Boot плагин (apply false означает, что он будет применен только к подпроектам,
    // которые явно укажут его использование)
    id("org.springframework.boot") apply false
}

// Конфигурация IntelliJ IDEA
idea {
    // Настройка проекта
    project {
        // Установка уровня языка Java для проекта
        languageLevel = IdeaLanguageLevel(21)
    }

    // Настройка модуля
    module {
        // Загружать документацию (javadoc)
        isDownloadJavadoc = true

        // Загружать исходные файлы
        isDownloadSources = true
    }
}

// Конфигурация всех проектов (родительского и дочерних)
allprojects {
    // Группа проекта (организация)
    group = "org.example"

    // Репозитории для загрузки зависимостей
    repositories {
        // Центральный Maven репозиторий
        mavenCentral()

        // Локальный Maven репозиторий
        mavenLocal()
    }

    // Определение переменных версий через project.properties
    val protobufBom: String by project
    val guava: String by project

    // Применяем плагин управления зависимостями
    apply(plugin = "io.spring.dependency-management")

    // Управление зависимостями
    dependencyManagement {
        dependencies {
            // Импорт BOM-файлов для управления версиями
            imports {
                // Spring Boot BOM
                mavenBom(BOM_COORDINATES)

                // Protocol Buffers BOM
                mavenBom("com.google.protobuf:protobuf-bom:$protobufBom")
            }

            // Прямые зависимости
            dependency("com.google.guava:guava:$guava")
        }
    }

    // Стратегия разрешения конфликтов версий
    configurations.all {
        resolutionStrategy {
            // Вызывать ошибку при конфликтах версий
            failOnVersionConflict()
        }
    }
}

// Конфигурация подпроектов
subprojects {
    // Применяем Java-плагин ко всем подпроектам
    plugins.apply(JavaPlugin::class.java)

    // Настраиваем расширение Java-плагина
    extensions.configure<JavaPluginExtension> {
        // Устанавливаем версию совместимости Java
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // Настраиваем компиляцию Java
    tasks.withType<JavaCompile> {
        // Кодировка исходного кода
        options.encoding = "UTF-8"

        // Дополнительные параметры компилятора
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-serial,-processing"))
    }

    // Настраиваем тестирование
    tasks.withType<Test> {
        // Используем JUnit Platform для запуска тестов
        useJUnitPlatform()

        // Показываем исключения при выполнении тестов
        testLogging.showExceptions = true
    }
}