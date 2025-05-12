package com.wonddak.fonthelper.setting

data class FontMatchSettingsState(
    var thinKeywords: List<String> = listOf("-Thin", "-thin"),
    var extraLightKeywords: List<String> = listOf("-ExtraLight", "-extraLight"),
    var lightKeywords: List<String> = listOf("-Light", "-light"),
    var regularKeywords: List<String> = listOf("-Regular", "-regular"),
    var mediumKeywords: List<String> = listOf("-Medium", "-medium"),
    var semiBoldKeywords: List<String> = listOf("-SemiBold", "-semiBold"),
    var boldKeywords: List<String> = listOf("-Bold", "-bold"),
    var extraBoldKeywords: List<String> = listOf("-ExtraBold", "-extraBold"),
    var blackKeywords: List<String> = listOf("-Black", "-black"),
    var italicKeywords: List<String> = listOf("-Italic", "-italic"),
    var thinItalicKeywords: List<String> = listOf("-ThinItalic", "-thinItalic"),
    var extraLightItalicKeywords: List<String> = listOf("-ExtraLightItalic", "-extraLightItalic"),
    var lightItalicKeywords: List<String> = listOf("-LightItalic", "-lightItalic"),
    var regularItalicKeywords: List<String> = listOf("-RegularItalic", "-regularItalic"),
    var mediumItalicKeywords: List<String> = listOf("-MediumItalic", "-mediumItalic"),
    var semiBoldItalicKeywords: List<String> = listOf("-SemiBoldItalic", "-semiBoldItalic"),
    var boldItalicKeywords: List<String> = listOf("-BoldItalic", "-boldItalic"),
    var extraBoldItalicKeywords: List<String> = listOf("-ExtraBoldItalic", "-extraBoldItalic"),
    var blackItalicKeywords: List<String> = listOf("-BlackItalic", "-blackItalic")
)