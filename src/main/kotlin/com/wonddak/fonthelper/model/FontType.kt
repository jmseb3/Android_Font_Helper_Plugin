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
        val st = StringBuilder(baseName.lowercase())
        st.append("_")
        st.append(FontUtil.getWeightTextByIndex(weight).lowercase())
        if (this is Italic) {
            st.append("_italic")
        }
        if (useExtension) {
            st.append(".")
            st.append(path.split(".").last())
        }
        return st.toString()
    }
}