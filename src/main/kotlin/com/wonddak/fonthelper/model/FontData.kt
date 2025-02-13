package com.wonddak.fonthelper.model

data class FontData(
    val fileName: String = "",
    val selectedModule: ModuleData? = null,
    var packageName: String = "",
    var useKotlinPath: Boolean = false,
    var normalFontPath: List<FontType.Normal?> = List(9) { null },
    var italicFontPath: List<FontType.Italic?> = List(9) { null },
) {
    private val safeFileName: String
        get() = fileName.trim()

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

    fun previewClassPath(): String {
        return getSaveClassPath() + savingClassFileName
    }


    val saveFontPath: String
        get() = (selectedModule?.getFontFilePath() ?: "")

    val totalFontPath: List<FontType>
        get() = normalFontPath.filterNotNull() + italicFontPath.filterNotNull()


    fun updateNormalFont(
        index: Int,
        path: String
    ): FontData {
        val temp = normalFontPath.toMutableList()
        temp[index] = FontType.Normal(path, index)
        return this.copy(normalFontPath = temp)
    }

    fun updateItalicFont(
        index: Int,
        path: String
    ): FontData {
        val temp = italicFontPath.toMutableList()
        temp[index] = FontType.Italic(path, index)
        return this.copy(italicFontPath = temp)
    }
}