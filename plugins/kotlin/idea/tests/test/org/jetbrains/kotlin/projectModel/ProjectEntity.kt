// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.projectModel

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID

/**
 * Entity representing Project
 * can be obtained from Openapi [Project], or by DSL entry fun [project]
 * can be converted to [ProjectResolveModel]
 * Will be joined with [ProjectResolveModel] to unified entity
 */
class ProjectEntity() {
    lateinit var project: Project
    lateinit var projectPath: String
    lateinit var moduleManager: ModuleManager
    var modules: MutableList<ModuleEntity> = mutableListOf()

    // DSL for definition in code
    fun module(name: String, initFunc: ModuleEntity.() -> Unit) {
        modules.add(ModuleEntity(name, this).apply(initFunc))
    }

    /**
     * Convert to [ProjectResolveModel]
     * Eventually these models will be joined.
     */
    fun toProjectResolveModel(): ProjectResolveModel {
        return ProjectResolveModel(this.modules.map { it.toResolveModule() })
    }

    companion object {
        // Adapter from Openapi
        fun importFromOpenapiProject(project: Project, projectPath: String): ProjectEntity {
            val moduleManager = ModuleManager.getInstance(project)
            val projectDataNode = ExternalSystemApiUtil.findProjectData(project, GRADLE_SYSTEM_ID, projectPath)

            return ProjectEntity().apply {
                this.project = project
                this.projectPath = projectPath
                this.moduleManager = moduleManager
                this.modules = moduleManager.modules.map { ModuleEntity.fromOpenapiModule(it, this) } as MutableList<ModuleEntity>
            }
        }

        // DSL for definition in code
        fun project(initFunc: ProjectEntity.() -> Unit): ProjectEntity {
            return ProjectEntity().apply {
                initFunc()
            }
        }
    }
}