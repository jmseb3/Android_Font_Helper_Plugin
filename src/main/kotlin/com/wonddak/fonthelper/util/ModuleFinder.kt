package com.wonddak.fonthelper.util

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.wonddak.fonthelper.model.ModuleData
import java.io.File

object ModuleFinder {

    /**
     * find Top Module
     */
    fun findModule(project: Project): List<ModuleData> {
        val projectPath = project.basePath!!
        val moduleManager = ModuleManager.getInstance(project)

        val moduleList = mutableListOf<ModuleData>()

        moduleManager.modules.forEach { module ->
            ModuleRootManager.getInstance(module).contentRoots.forEach { root ->
                val moduleDir = File(root.path)
                //check build.gradle or build.gradle.kts exist
                val gradleFile = File(moduleDir, "build.gradle")
                val gradleKtsFile = File(moduleDir, "build.gradle.kts")

                if (gradleFile.exists() || gradleKtsFile.exists()) {
                    // if build.gradle or build.gradle.kts exist
                    // then In this case, it is determined to Top Module
                    val commonMainPath = File(moduleDir, "src/commonMain")

                    if (commonMainPath.exists()) {
                        // if src/commonMain exist
                        // then In this case, it is determined to CMP Project
                        val moduleData = ModuleData(
                            name = module.name,
                            path = commonMainPath.path,
                            isCMP = true,
                        )
                        moduleList.add(moduleData)
                    }

                    val mainPath = File(moduleDir, "src/main")
                    if (mainPath.exists()) {
                        // if src/main exist
                        // then In this case, it is determined to Android Only Project
                        val moduleData = ModuleData(
                            name = module.name,
                            path = mainPath.path,
                            isCMP = false,
                        )
                        moduleList.add(moduleData)
                    }
                }
            }
        }
        return moduleList
    }

}