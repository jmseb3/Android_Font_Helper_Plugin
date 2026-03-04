package com.wonddak.fonthelper.widget

internal fun String.isSupportedFontFile(): Boolean {
    val normalized = lowercase()
    return normalized.endsWith(".ttf") || normalized.endsWith(".otf")
}

internal fun String.normalizeDroppedPath(): String {
    return replaceFirst(Regex("^file:", RegexOption.IGNORE_CASE), "")
}
