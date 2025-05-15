package com.wonddak.fonthelper.setting

data class FontMatchSettingsState(
    var thinKeywords: List<String> = listOf("-thin"),
    var extraLightKeywords: List<String> = listOf("-extralight"),
    var lightKeywords: List<String> = listOf("-light"),
    var regularKeywords: List<String> = listOf("-regular"),
    var mediumKeywords: List<String> = listOf("-medium"),
    var semiBoldKeywords: List<String> = listOf("-semibold"),
    var boldKeywords: List<String> = listOf("-bold"),
    var extraBoldKeywords: List<String> = listOf("-extraBold"),
    var blackKeywords: List<String> = listOf("-black"),

    var thinItalicKeywords: List<String> = listOf("-thinitalic"),
    var extraLightItalicKeywords: List<String> = listOf("-extralightitalic"),
    var lightItalicKeywords: List<String> = listOf("-lightitalic"),
    var regularItalicKeywords: List<String> = listOf("-italic"),
    var mediumItalicKeywords: List<String> = listOf("-mediumitalic"),
    var semiBoldItalicKeywords: List<String> = listOf("-semibolditalic"),
    var boldItalicKeywords: List<String> = listOf("-bolditalic"),
    var extraBoldItalicKeywords: List<String> = listOf("-extrabolditalic"),
    var blackItalicKeywords: List<String> = listOf("-blackitalic")
) {
    /**
     * @return Pair<isItalic, fontWeightIndex> or null if no match
     */
    fun checkType(fileName: String) : Pair<Boolean,Int>? {
        val italicMap = mapOf(
            extraLightItalicKeywords to 1,
            extraBoldItalicKeywords to 7,
            semiBoldItalicKeywords to 5,
            thinItalicKeywords to 0,
            lightItalicKeywords to 2,
            regularItalicKeywords to 3,
            mediumItalicKeywords to 4,
            boldItalicKeywords to 6,
            blackItalicKeywords to 8
        )
        for ((keywords,index) in italicMap) {
            if (keywords.any { fileName.contains(it, ignoreCase = true) }) {
                return true to index
            }
        }

        val regularMap = mapOf(
            extraLightKeywords to 1,
            extraBoldKeywords to 7,
            semiBoldKeywords to 5,
            thinKeywords to 0,
            lightKeywords to 2,
            regularKeywords to 3,
            mediumKeywords to 4,
            boldKeywords to 6,
            blackKeywords to 8
        )

        for ((keywords, index) in regularMap) {
            if (keywords.any { fileName.contains(it, ignoreCase = true) }) {
                return false to index
            }
        }

        return  null
    }
}