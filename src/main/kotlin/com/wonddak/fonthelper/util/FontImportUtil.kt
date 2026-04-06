package com.wonddak.fonthelper.util

import com.wonddak.fonthelper.model.FontData
import com.wonddak.fonthelper.setting.FontMatchSettingsState
import java.io.File

internal fun analyzeImportedFonts(
    files: List<File>,
    settings: FontMatchSettingsState,
): ImportAnalysis {
    // Group every matched file by its target slot so callers can auto-apply singles and surface real conflicts.
    val groups = linkedMapOf<FontSlotKey, MutableList<File>>()
    var matchedCount = 0
    files.forEach { file ->
        val fileName = file.name.lowercase()
        val match = settings.checkType(fileName) ?: return@forEach
        val key = FontSlotKey(isItalic = match.first, weight = match.second)
        groups.getOrPut(key) { mutableListOf() }.add(file)
        matchedCount += 1
    }
    return ImportAnalysis(groups = groups.mapValues { it.value.toList() }, matchedCount = matchedCount)
}

internal fun applyImportedFonts(
    current: FontData,
    assignments: Map<FontSlotKey, File>,
): FontData {
    var updated = current
    assignments.forEach { (slot, file) ->
        updated = if (slot.isItalic) {
            updated.updateItalicFont(slot.weight, file.absolutePath)
        } else {
            updated.updateNormalFont(slot.weight, file.absolutePath)
        }
    }
    return updated
}

internal fun removeManagedDownloadedPaths(current: FontData): FontData {
    // Only clear files that were downloaded into the plugin-managed cache; keep user-owned paths intact.
    val cleanedNormal = current.normalFontPath.map { font ->
        if (font != null && GoogleFontsUtil.isManagedDownloadedFile(font.path)) null else font
    }
    val cleanedItalic = current.italicFontPath.map { font ->
        if (font != null && GoogleFontsUtil.isManagedDownloadedFile(font.path)) null else font
    }
    return current.copy(normalFontPath = cleanedNormal, italicFontPath = cleanedItalic)
}

internal data class GoogleFileSelectionState(
    val family: String,
    val autoSelectedBySlot: Map<FontSlotKey, GoogleFontFileRef>,
    val conflictCandidatesBySlot: Map<FontSlotKey, List<GoogleFontFileRef>>,
)

internal data class ImportAnalysis(
    val groups: Map<FontSlotKey, List<File>>,
    val matchedCount: Int,
)

internal data class ImportConflictSelectionState(
    val sourceLabel: String,
    val currentFontData: FontData,
    val conflicts: List<ImportConflict>,
)

internal data class ImportConflict(
    val slot: FontSlotKey,
    val candidates: List<File>,
)

internal data class FontSlotKey(
    val isItalic: Boolean,
    val weight: Int,
) {
    fun displayText(): String {
        val style = if (isItalic) "Italic" else "Normal"
        return "${FontUtil.getWeightTextByIndex(weight)} $style"
    }
}
