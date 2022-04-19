// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.projectModel.ProjectEntity
import org.jetbrains.kotlin.test.matcher.checkProjectEntity
import org.junit.Test

/**
 * An attempt to create independent suite - not inherited from the hierarchy
 */
class HMImportIsolatedTest {

    val utilBase = object : MultiplePluginVersionGradleImportingTestCase() { }

    @Test
    fun testImportHMPPFlag() {
        utilBase.gradleVersion = "6.9"
        utilBase.kotlinPluginParameter = "1.5.20"
        utilBase.setName("testImportHMPPFlag")
        utilBase.setUp()
        utilBase.configureByFiles()
        utilBase.importProject()

        val messageCollector = MessageCollector()

        checkProjectEntity(
            ProjectEntity.importFromOpenapiProject(utilBase.myProject, utilBase.projectPath),
            messageCollector,
            exhaustiveModuleList = false,
            exhaustiveSourceSourceRootList = false,
            exhaustiveDependencyList = false,
            exhaustiveTestsList = false,
        ) {
            allModules {
                isHMPP(true)
                assertNoDependencyInBuildClasses()
            }
            module("my-app.commonMain")
            module("my-app.jvmAndJsMain")
        }
    }
}