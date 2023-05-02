package com.wonddak.fonthelper.util

import com.intellij.openapi.vfs.VfsUtil

object PathUtil {
    private const val FONT_PATH = "src/main/res/font"
    private const val CLASS_PATH = "src/main/java"


    // Class Name and Variable Name
    var fileName: String = ""

    // basePath
    var base = ""

    // module Name (ex: app)
    var module = ""

    // packageName
    var packageName = ""


    fun clearAll() {
        fileName = ""
        base = ""
        module = ""
        packageName = ""
    }
    fun getFontPath(): String {
        return "$base/$module/$FONT_PATH"
    }

    fun getClassPath(): String {
        val st = StringBuilder()
        st.append(base)
        st.append("/")
        st.append(module)
        st.append("/")
        st.append(CLASS_PATH)
        if (packageName.isNotEmpty()) {
            st.append("/")
            st.append(packageName.replace(".", "/"))
        }
        return st.toString()
    }

    fun refresh() {
        VfsUtil.createDirectoryIfMissing(getFontPath())?.refresh(false, true)
        VfsUtil.createDirectoryIfMissing(getClassPath())?.refresh(false, true)
    }
}