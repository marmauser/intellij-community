// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.projectModel

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.project.isHMPPEnabled
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import java.io.File
import kotlin.properties.Delegates

/**
 * Entity representing Module of project
 * can be obtained from Openapi [Module], or [ResolveModule]
 * can be converted to [ResolveModule]
 * Will be joined with [ResolveModule] to unified entity
 */
class ModuleEntity(val name: String, val projectEntity: ProjectEntity) {

    lateinit var actualVersion: String
    lateinit var orderEntries: Array<OrderEntry>
    lateinit var sourceFolderByPath: Map<String, SourceFolder>
    var componentPlatforms: Set<SimplePlatform>? = null
    var hMPPEnabled by Delegates.notNull<Boolean>()
    var root: File? = null
    var testRoot: File? = null
    // TODO: add remaining entity properties

    /**
     * Convert to [ResolveModule]
     * Eventually these models will be joined.
     */
    fun toResolveModule(): ResolveModule {
        return ResolveModule(
            name = this.name,
            root = this.root!!,
            platform = TargetPlatform(componentPlatforms!!),
            dependencies = orderEntries.toListOfResolvedDependencies(),
            // TODO: provide remaining arguments
        )
    }

    companion object {
        /**
         * Create [ModuleEntity] from Openapi [Module]
         */
        fun fromOpenapiModule(module: Module, projectEntity: ProjectEntity): ModuleEntity {
            return ModuleEntity(module.name, projectEntity).apply {
                val rootModel = module.rootManager
                actualVersion = module.languageVersionSettings.languageVersion.versionString
                orderEntries = rootModel.orderEntries
                componentPlatforms = module.platform?.componentPlatforms
                hMPPEnabled = module.isHMPPEnabled
                sourceFolderByPath = rootModel.contentEntries.asSequence()
                    .flatMap { it.sourceFolders.asSequence() }
                    .mapNotNull {
                        val path = it.file?.path ?: return@mapNotNull null
                        FileUtil.getRelativePath(projectEntity.projectPath, path, '/')!! to it
                    }
                    .toMap()
                root = File(module.moduleFilePath)

                // TODO: add remaining entity properties
            }
        }

        /**
         * Convert from [ResolveModule]
         * Eventually these models will be joined.
         */
        fun fromResolveModule(module: ResolveModule, projectEntity: ProjectEntity): ModuleEntity {
            return ModuleEntity(module.name, projectEntity).apply {
                this.componentPlatforms = module.platform.componentPlatforms
                this.root = module.root
                // TODO convert remaining properties
            }
        }
    }
}

fun Array<OrderEntry>.toListOfResolvedDependencies(): List<ResolveDependency> {
    // TODO("NOT IMPLEMENTED")
    return emptyList()
}


//fun module(name: String, isOptional: Boolean = false, assertionFunc: ModuleEntity.() -> Unit): ModuleEntity {
//    return ModuleEntity(name, isOptional).apply { this.assertionFunc() }
//}



