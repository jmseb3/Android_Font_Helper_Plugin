package com.wonddak.fonthelper.util

import java.io.File
import java.util.regex.Pattern


/**
 * Helper Object For Gradle parse
 */
object GradleUtil {
    private const val GRADLE = "/build.gradle"
    private const val GRADLE_KTS = "/build.gradle.kts"

    private const val ID_PATTERN_GRADLE = "applicationId\\s+[\"']?([a-zA-Z_][a-zA-Z0-9_.]*[a-zA-Z0-9])[\"']?\n"
    private const val ID_PATTERN_GRADLE_KTS = "applicationId\\s*=\\s*\"?([a-zA-Z_][a-zA-Z0-9_.]*[a-zA-Z0-9])\"?\n"
    private const val NAMESPACE_PATTERN_GRADLE = "namespace\\s+[\"']?([a-zA-Z_][a-zA-Z0-9_.]*[a-zA-Z0-9])[\"']?\n"
    private const val NAMESPACE_PATTERN_GRADLE_KTS = "namespace\\s*=\\s*\"?([a-zA-Z_][a-zA-Z0-9_.]*[a-zA-Z0-9])\"?\n"

    fun getPackageName(rootPath: String): String {
        val gradle = rootPath + GRADLE
        val gradleKts = rootPath + GRADLE_KTS
        val gradleFile = File(gradle)
        val gradleKtsFile = File(gradleKts)

        var result :String? = null
        if (gradleFile.exists()) {
            println(".gradle exist()")
            result =  getPackageNameFormGradle(gradleFile, NAMESPACE_PATTERN_GRADLE, ID_PATTERN_GRADLE)
        } else if (gradleKtsFile.exists()) {
            println(".gradle.kts exist()")
            result =  getPackageNameFormGradle(gradleKtsFile, NAMESPACE_PATTERN_GRADLE_KTS, ID_PATTERN_GRADLE_KTS)
        }

        if (result == null) {
            throw RuntimeException("can't get NameSpace or ApplicationId")
        }
        return result
    }


    private fun getPackageNameFormGradle(file: File, name: String, id: String): String? {
        val namePattern = Pattern.compile(name)
        val idPattern = Pattern.compile(id)
        val buildGradleText = file.readText()

        val nameMatcher = namePattern.matcher(buildGradleText)
        if (nameMatcher.find()) {
            return nameMatcher.group(1)
        }

        val idMatcher = idPattern.matcher(buildGradleText)
        if (idMatcher.find()) {
            return idMatcher.group(1)
        }

        return null
    }


}