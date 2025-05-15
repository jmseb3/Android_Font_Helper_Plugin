package com.wonddak.fonthelper.model

/**
 * ModuleData
 * @param[name] module name
 * @param[path] module path
 * @param[isCMP] check Compose MultiPlatform Project
 */
data class ModuleData(
    var name: String = "",
    var path: String = "",
    var isCMP: Boolean = false,
) {
    val importModuleName : String
        get() = name.lowercase().replace("-","_")
    /**
     * make font save path
     */
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

    /**
     * make class File save Path
     * @param[useKotlinPath] use kotlin path instead java,
     *
     * if is [isCMP] true then ignore [useKotlinPath]
     */
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