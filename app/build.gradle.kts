plugins {
    alias(libs.plugins.agp.app)
}

android {
    namespace = "io.github.xiaomimarket.spoof"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "io.github.xiaomimarket.spoof"
        minSdk = 26
        targetSdk = 37
        versionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 4
        versionName = "1.0.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            // 仅用于本机测试；正式发布请换成自己的签名配置。
            signingConfig = signingConfigs["debug"]
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
}
