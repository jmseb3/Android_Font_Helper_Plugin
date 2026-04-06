package com.wonddak.fonthelper.util

import com.wonddak.fonthelper.model.FontData
import com.wonddak.fonthelper.setting.FontMatchSettingsState
import java.io.File

internal fun prepareZipImportResult(
    current: FontData,
    downloaded: List<File>,
    settings: FontMatchSettingsState,
): ZipImportResult {
    val analysis = analyzeImportedFonts(downloaded, settings)
    val autoAssignments = analysis.groups
        .filterValues { it.size == 1 }
        .mapValues { it.value.first() }
    val updated = applyImportedFonts(current, autoAssignments)

    val conflicts = analysis.groups
        .filterValues { it.size > 1 }
        .entries
        .sortedBy { it.key.displayText() }
        .map { (slot, files) -> ImportConflict(slot, files.sortedBy { it.name.lowercase() }) }

    val message = when {
        analysis.matchedCount == 0 -> "ZIP imported, but no variants matched current keywords."
        conflicts.isEmpty() -> "ZIP imported. ${analysis.matchedCount} variants mapped."
        else -> "ZIP imported. ${autoAssignments.size} auto-mapped, ${conflicts.size} conflicts need selection."
    }

    val conflictState = if (conflicts.isEmpty()) {
        null
    } else {
        ImportConflictSelectionState(
            sourceLabel = "ZIP",
            currentFontData = updated,
            conflicts = conflicts,
        )
    }

    return ZipImportResult(
        updatedFontData = updated,
        message = message,
        conflictSelectionState = conflictState,
    )
}

internal fun prepareGoogleFontSelectionResult(
    family: String,
    refs: List<GoogleFontFileRef>,
    settings: FontMatchSettingsState,
): GoogleFontSelectionResult {
    val slotCandidates = refs
        .mapNotNull { ref ->
            val shortName = ref.filename.substringAfterLast('/').substringAfterLast('\\')
            val match = settings.checkType(shortName.lowercase()) ?: return@mapNotNull null
            FontSlotKey(isItalic = match.first, weight = match.second) to ref
        }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .toSortedMap(compareBy<FontSlotKey> { it.isItalic }.thenBy { it.weight })

    if (slotCandidates.isEmpty()) {
        return GoogleFontSelectionResult(
            message = "\"$family\" list loaded, but no variants matched current keywords.",
        )
    }

    val normalizedBySlot = slotCandidates.mapValues { (_, list) ->
        list.distinctBy { it.url }.sortedBy { it.filename.lowercase() }
    }
    val autoSelectedBySlot = normalizedBySlot
        .filterValues { it.size == 1 }
        .mapValues { (_, refsBySlot) -> refsBySlot.first() }
    val conflictCandidatesBySlot = normalizedBySlot
        .filterValues { it.size > 1 }

    if (conflictCandidatesBySlot.isEmpty()) {
        return GoogleFontSelectionResult(
            autoSelectedBySlot = autoSelectedBySlot,
            message = null,
        )
    }

    return GoogleFontSelectionResult(
        autoSelectedBySlot = autoSelectedBySlot,
        selectionState = GoogleFileSelectionState(
            family = family,
            autoSelectedBySlot = autoSelectedBySlot,
            conflictCandidatesBySlot = conflictCandidatesBySlot,
        ),
        message = "\"$family\" list loaded. ${autoSelectedBySlot.size} auto-selected, choose ${conflictCandidatesBySlot.size} conflict slots.",
    )
}

internal fun resolveDownloadedAssignments(
    selections: Map<FontSlotKey, GoogleFontFileRef>,
    downloadedByUrl: Map<String, File>,
): Map<FontSlotKey, File> {
    return selections.mapNotNull { (slot, ref) ->
        downloadedByUrl[ref.url]?.let { slot to it }
    }.toMap()
}

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

internal data class ZipImportResult(
    val updatedFontData: FontData,
    val message: String,
    val conflictSelectionState: ImportConflictSelectionState?,
)

internal data class GoogleFontSelectionResult(
    val autoSelectedBySlot: Map<FontSlotKey, GoogleFontFileRef> = emptyMap(),
    val selectionState: GoogleFileSelectionState? = null,
    val message: String?,
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
