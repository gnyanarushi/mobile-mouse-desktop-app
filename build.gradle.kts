plugins {
    id("java")
    id("application")
}

group = "com.mousecontrol"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.json:json:20230227")
}

application {
  mainClass.set("com.mousecontrol.Main")
}

tasks.test {
    useJUnitPlatform()
}

