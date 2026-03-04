package com.wonddak.fonthelper.util

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.wonddak.fonthelper.model.ModuleData
import java.io.File
import java.io.IOException
import java.util.Locale

object ModuleFinder {

    /**
     * Find importable Gradle modules.
     *
     * Selection rules:
     * 1) Module root must have build.gradle(.kts)
     * 2) Prefer KMP(commonMain) over Android(main) when both exist
     * 3) Remove duplicates by normalized absolute path
     * 4) Return a stable sorted list for predictable UI ordering
     */
    fun findModule(project: Project): List<ModuleData> {
        val moduleManager = ModuleManager.getInstance(project)
        val byPath = LinkedHashMap<String, ModuleData>()

        moduleManager.modules
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
            .forEach { module ->
                ModuleRootManager.getInstance(module).contentRoots.forEach { root ->
                    val moduleDir = root.path.toCanonicalFileOrNull()
                        ?: return@forEach
                    if (!moduleDir.isDirectory) {
                        return@forEach
                    }
                    if (!moduleDir.hasGradleBuildFile()) {
                        return@forEach
                    }

                    val selected = resolveModuleData(module.name, moduleDir)
                        ?: return@forEach
                    byPath.putIfAbsent(selected.path, selected)
                }
            }

        return byPath.values
            .sortedWith(
                compareBy<ModuleData> { it.name.lowercase(Locale.getDefault()) }
                    .thenBy { it.path.lowercase(Locale.getDefault()) }
            )
    }

    private fun resolveModuleData(moduleName: String, moduleDir: File): ModuleData? {
        val commonMainPath = File(moduleDir, "src/commonMain")
        val mainPath = File(moduleDir, "src/main")

        return when {
            commonMainPath.isDirectory -> ModuleData(
                name = moduleName,
                path = commonMainPath.path.toCanonicalPathOrSame(),
                isCMP = true,
            )
            mainPath.isDirectory -> ModuleData(
                name = moduleName,
                path = mainPath.path.toCanonicalPathOrSame(),
                isCMP = false,
            )
            else -> null
        }
    }

    private fun File.hasGradleBuildFile(): Boolean {
        return File(this, "build.gradle").isFile || File(this, "build.gradle.kts").isFile
    }

    private fun String.toCanonicalPathOrSame(): String {
        return try {
            File(this).canonicalPath
        } catch (_: IOException) {
            this
        }
    }

    private fun String.toCanonicalFileOrNull(): File? {
        return try {
            File(this).canonicalFile
        } catch (_: IOException) {
            null
        }
    }
}
