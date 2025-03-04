/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

kotlin.sourceSets {
    jvmMain {
        dependencies {
            api(libs.pebble)
        }
    }
    jvmTest {
        dependencies {
            api(project(":ktor-server:ktor-server-plugins:ktor-server-conditional-headers"))
            api(project(":ktor-server:ktor-server-plugins:ktor-server-compression"))
        }
    }
}
