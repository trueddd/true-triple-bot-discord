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

fun DependencyHandlerScope.ktor(name: String) = implementation("io.ktor", name, "1.4.1")
fun DependencyHandlerScope.exposed(name: String) = implementation("org.jetbrains.exposed", name, "0.27.1")

dependencies {
    val kotlinVersion = "1.4.0"
    val logbackVersion = "1.2.3"
    val postgresDriverVersion = "42.2.2"

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    ktor("ktor-server-core")
    ktor("ktor-server-netty")
    ktor("ktor-websockets")
    ktor("ktor-gson")
    ktor("ktor-client-core")
    ktor("ktor-client-cio")
    ktor("ktor-client-websockets")
    ktor("ktor-client-okhttp")
    ktor("ktor-client-json")
    ktor("ktor-client-gson")
    ktor("ktor-client-logging-jvm")

    implementation("dev.kord:kord-core:0.8.0-M5")

    implementation("org.jsoup:jsoup:1.13.1")

    implementation("org.postgresql:postgresql:$postgresDriverVersion")
    exposed("exposed-core")
    exposed("exposed-dao")
    exposed("exposed-jdbc")
    exposed("exposed-java-time")
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
    kotlinOptions.useIR = true
}
