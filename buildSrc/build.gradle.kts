plugins {
    `kotlin-dsl`
    maven
}

repositories {
    jcenter()
}

dependencies {
    // https://mvnrepository.com/artifact/org.eclipse.jgit/org.eclipse.jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.9.0.202009080501-r")
    // https://square.github.io/kotlinpoet/#download
    implementation("com.squareup:kotlinpoet:1.6.0")

}