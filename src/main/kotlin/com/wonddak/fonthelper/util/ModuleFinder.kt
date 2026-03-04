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
        val moduleManager = ModuleManager.getInstance(project)

        val moduleList = LinkedHashMap<String, ModuleData>()

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
                    val mainPath = File(moduleDir, "src/main")

                    if (commonMainPath.exists()) {
                        // Prefer CMP source set when both commonMain and main exist.
                        val key = "${module.name}:${commonMainPath.path}:cmp"
                        moduleList.putIfAbsent(key, ModuleData(
                            name = module.name,
                            path = commonMainPath.path,
                            isCMP = true,
                        ))
                    } else if (mainPath.exists()) {
                        val key = "${module.name}:${mainPath.path}:main"
                        moduleList.putIfAbsent(key, ModuleData(
                            name = module.name,
                            path = mainPath.path,
                            isCMP = false,
                        ))
                    }
                }
            }
        }
        return moduleList.values.toList()
    }

}
