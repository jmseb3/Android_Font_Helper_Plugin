package com.wonddak.fonthelper.util

import com.wonddak.fonthelper.model.ModuleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PackageNameResolver {

    suspend fun resolve(module: ModuleData): String? {
        return withContext(Dispatchers.IO) {
            val sourceRoot = File(module.path)
            if (!sourceRoot.isDirectory) return@withContext null

            resolveFromManifest(sourceRoot)
                ?: resolveFromGradleNamespace(sourceRoot)
                ?: resolveFromSourcePackages(sourceRoot)
        }
    }

    private fun resolveFromManifest(sourceRoot: File): String? {
        val manifest = File(sourceRoot, "AndroidManifest.xml")
        if (!manifest.isFile) return null
        val text = manifest.readText()
        val pkg = Regex("""\bpackage\s*=\s*["']([^"']+)["']""")
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
        return pkg.ifBlank { null }
    }

    private fun resolveFromGradleNamespace(sourceRoot: File): String? {
        val moduleRoot = sourceRoot.parentFile?.parentFile ?: return null
        val gradleKts = File(moduleRoot, "build.gradle.kts")
        val gradleGroovy = File(moduleRoot, "build.gradle")
        val candidate = when {
            gradleKts.isFile -> gradleKts.readText()
            gradleGroovy.isFile -> gradleGroovy.readText()
            else -> return null
        }

        val kts = Regex("""\bnamespace\s*=\s*["']([^"']+)["']""")
            .find(candidate)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
        if (kts.isNotBlank()) return kts

        val groovy = Regex("""\bnamespace\s+["']([^"']+)["']""")
            .find(candidate)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
        return groovy.ifBlank { null }
    }

    private fun resolveFromSourcePackages(sourceRoot: File): String? {
        val roots = listOf(File(sourceRoot, "kotlin"), File(sourceRoot, "java"))
            .filter { it.isDirectory }
        if (roots.isEmpty()) return null

        val counts = linkedMapOf<String, Int>()
        var inspected = 0

        roots.forEach { root ->
            root.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    if (inspected++ > 2000) return@forEach
                    val pkg = readPackageLine(file) ?: return@forEach
                    counts[pkg] = (counts[pkg] ?: 0) + 1
                }
        }

        return counts.maxByOrNull { it.value }?.key
    }

    private fun readPackageLine(file: File): String? {
        file.useLines { lines ->
            lines.take(40).forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("package ")) {
                    return trimmed.removePrefix("package ").trim().ifBlank { null }
                }
            }
        }
        return null
    }
}
