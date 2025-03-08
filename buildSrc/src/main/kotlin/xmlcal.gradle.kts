plugins {
    id("java-library")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://maven.saxonica.com/maven") }
}

val extension = project.extensions.create<XmlCalExtension>("xmlcal")
