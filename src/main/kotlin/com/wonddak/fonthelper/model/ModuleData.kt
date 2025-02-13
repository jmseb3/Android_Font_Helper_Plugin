package com.wonddak.fonthelper.model

data class ModuleData(
    var name: String = "",
    var path: String = "",
    var isCMP: Boolean = false,
) {
    fun getFontFilePath(): String {
        val st = StringBuilder(path)
        st.append("/")
        if (isCMP) {
            st.append("composeResources")
        } else {
            st.append("res")
        }
        st.append("/")
        st.append("font")
        st.append("/")
        return st.toString()
    }

    fun getClassPath(
        useKotlinPath: Boolean
    ): String {
        val st = StringBuilder(path)
        st.append("/")
        if (isCMP || useKotlinPath) {
            st.append("kotlin")
        } else {
            st.append("java")
        }
        st.append("/")
        return st.toString()
    }
}