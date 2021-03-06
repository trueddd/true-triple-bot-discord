plugins {
    application
    kotlin("jvm") version "1.4.0"
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

group = "com.github.trueddd"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

repositories {
    jcenter()
    mavenLocal()
    maven { url = uri("http://dl.bintray.com/kotlin/kotlin-eap") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("http://kotlin.bintray.com/ktor") }
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
    maven(url = "https://dl.bintray.com/kordlib/Kord")
}

dependencies {
    val ktorVersion = "1.4.1"
    val kotlinVersion = "1.4.0"
    val logbackVersion = "1.2.3"
    val postgresDriverVersion = "42.2.2"
    val exposedVersion = "0.27.1"

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")

    implementation("dev.kord:kord-core:0.7.0-RC")

    implementation("org.jsoup:jsoup:1.13.1")

    implementation("org.postgresql:postgresql:$postgresDriverVersion")
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)
}

sourceSets {
    kotlin.sourceSets["main"].kotlin.srcDirs("src")
    kotlin.sourceSets["test"].kotlin.srcDirs("test")
    kotlin.sourceSets["main"].kotlin.srcDirs("resources")
}

tasks.register("stage") {
    dependsOn(tasks.named("clean"), tasks.named("build"))
    mustRunAfter(tasks.named("clean"))
}

tasks.register<Jar>("fatJar") {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName,
                "Class-Path" to configurations.compile
            )
        )
    }
    archiveBaseName.set("${project.name}-all")
    from(Callable { configurations["runtimeClasspath"].map { if (it.isDirectory) it else zipTree(it) } })
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=io.ktor.util.KtorExperimentalAPI"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=io.ktor.util.InternalAPI"
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalStdlibApi"
}
