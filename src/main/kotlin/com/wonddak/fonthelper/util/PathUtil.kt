package com.wonddak.fonthelper.util

import com.intellij.openapi.vfs.VfsUtil
import com.wonddak.fonthelper.FontHelperDialog
import com.wonddak.fonthelper.FontHelperDialog.Companion.isCMPProject

/**
 * Helper Object For Path
 */
object PathUtil {
    private const val FONT_PATH = "src/main/res/font"
    private const val FONT_PATH_CMP = "src/commonMain/composeResources/font"
    private const val CLASS_PATH = "src/main/java"
    private const val CLASS_PATH_CMP = "src/commonMain/kotlin"



    // Class Name and Variable Name
    var fileName: String = ""

    // basePath
    var base = ""

    // module Name (ex: app)
    var module = ""
        private set

    //set Module And setPackageName
    fun setModule(module:String) {
        this.module = module
        this.packageName = GradleUtil.getPackageName(FontHelperDialog.spinnerList[module]!!)
        println(getClassPath())
        println(getFontPath())
    }

    // packageName
    var packageName = ""


    fun clearAll() {
        fileName = ""
        base = ""
        module = ""
        packageName = ""
    }
    fun getFontPath(): String {
        if (isCMPProject) {
            return "$base/$module/$FONT_PATH_CMP"
        }
        return "$base/$module/$FONT_PATH"
    }

    fun getClassPath(): String {
        val st = StringBuilder()
        st.append(base)
        st.append("/")
        st.append(module)
        st.append("/")
        if(isCMPProject) {
            st.append(CLASS_PATH_CMP)
        } else {
            st.append(CLASS_PATH)
        }
        if (packageName.isNotEmpty()) {
            st.append("/")
            st.append(packageName.replace(".", "/"))
        }
        return st.toString()
    }

    fun makeSavingFormatFileName() :String {
        if (fileName.isEmpty()) {
            return  ""
        }

        val result = if (fileName.length == 1) {
            fileName.uppercase()
        } else {
            "${fileName[0].uppercase()}${fileName.slice(IntRange(1, fileName.length - 1))}"
        }

        return "${result}.kt"
    }

    fun getSimplePath(): String {
        val st = StringBuilder()
        st.append(".")
        st.append("/")
        st.append(module)
        st.append("/")
        if(isCMPProject) {
            st.append(CLASS_PATH_CMP)
        } else {
            st.append(CLASS_PATH)
        }
        if (packageName.isNotEmpty()) {
            st.append("/")
            st.append(packageName.replace(".", "/"))
        }
        st.append("/")
        st.append(makeSavingFormatFileName())
        return st.toString()
    }

    fun refresh() {
        VfsUtil.createDirectoryIfMissing(getFontPath())?.refresh(false, true)
        VfsUtil.createDirectoryIfMissing(getClassPath())?.refresh(false, true)
    }
}
