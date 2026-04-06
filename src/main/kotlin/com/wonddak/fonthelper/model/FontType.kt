package com.wonddak.fonthelper.model

import com.wonddak.fonthelper.util.FontUtil

sealed class FontType(
    open val path: String,
    open val weight: Int
) {

    data class Normal(
        override val path: String,
        override val weight: Int
    ) : FontType(path, weight)

    data class Italic(
        override val path: String,
        override val weight: Int
    ) : FontType(path, weight)

    fun makeFontFileName(
        baseName: String,
        useExtension : Boolean = true
    ): String {
        val st = StringBuilder(baseName.toSnakeCase())
        st.append("_")
        st.append(FontUtil.getWeightTextByIndex(weight).toSnakeCase())
        if (this is Italic) {
            st.append("_italic")
        }
        if (useExtension) {
            st.append(".")
            st.append(path.split(".").last())
        }
        return st.toString()
    }

    private fun String.toSnakeCase(): String {
        return trim()
            .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1_$2")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .lowercase()
    }
}
