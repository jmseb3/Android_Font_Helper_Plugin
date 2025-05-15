package com.wonddak.fonthelper.model

/**
 * FontSaveData
 * @param[fileName] saveFileName if roboto then Roboto.kt
 * @param[selectedModule] selectModule in project
 * @param[packageName] packageName in project
 * @param[useKotlinPath] use "kotlin" path instead "java" this is work only android project
 * @param[normalFontPath] normal font List
 * @param[italicFontPath] italic font List
 */
data class FontData(
    val fileName: String = "",
    val selectedModule: ModuleData? = null,
    var packageName: String = "",
    var useKotlinPath: Boolean = false,
    var normalFontPath: List<FontType.Normal?> = List(9) { null },
    var italicFontPath: List<FontType.Italic?> = List(9) { null },
) {
    /**
     * safe file name trim
     */
    private val safeFileName: String
        get() = fileName.trim()

    /**
     * saving class file formant
     */
    val savingClassFileName: String
        get() = if (safeFileName.isEmpty()) {
            ""
        } else {
            val result = if (safeFileName.length == 1) {
                safeFileName.uppercase()
            } else {
                "${safeFileName[0].uppercase()}${safeFileName.slice(IntRange(1, safeFileName.length - 1))}"
            }
            "${result}.kt"
        }

    /**
     * make save class File Path with packageName
     */
    fun getSaveClassPath(): String {
        val st = StringBuilder()
        if (selectedModule != null) {
            st.append(selectedModule.getClassPath(useKotlinPath))
        }
        if (packageName.isNotEmpty()) {
            st.append(packageName.replace(".", "/"))
            st.append("/")
        }
        return st.toString()
    }

    /**
     * make save class File Path Preview
     */
    fun previewClassPath(): String {
        return getSaveClassPath() + savingClassFileName
    }

    /**
     * save Font Path
     */
    val saveFontPath: String
        get() = (selectedModule?.getFontFilePath() ?: "")

    /**
     * get Total FontType List not null
     */
    val totalFontPath: List<FontType>
        get() = normalFontPath.filterNotNull() + italicFontPath.filterNotNull()


    /**
     * update Normal Font List
     */
    fun updateNormalFont(
        index: Int,
        path: String
    ): FontData {
        val temp = normalFontPath.toMutableList()
        temp[index] = FontType.Normal(path, index)
        return this.copy(normalFontPath = temp)
    }

    /**
     * update Italic Font List
     */
    fun updateItalicFont(
        index: Int,
        path: String
    ): FontData {
        val temp = italicFontPath.toMutableList()
        temp[index] = FontType.Italic(path, index)
        return this.copy(italicFontPath = temp)
    }

    fun clearAllFont() : FontData {
        return this.copy(
            normalFontPath = List(9) { null },
            italicFontPath = List(9) { null }
        )
    }

    fun enabledOk() :Boolean {
        if (fileName.isEmpty()) {
            return false
        }

        if (selectedModule == null) {
            return false
        }

        if (totalFontPath.isEmpty()) {
            return false
        }
        return true
    }
}