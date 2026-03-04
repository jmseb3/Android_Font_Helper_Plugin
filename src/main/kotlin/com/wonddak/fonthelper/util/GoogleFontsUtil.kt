package com.wonddak.fonthelper.util

import com.intellij.openapi.diagnostic.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipInputStream

data class GoogleFontFamily(
    val family: String,
    val category: String?
)

object GoogleFontsUtil {
    private val log = Logger.getInstance(GoogleFontsUtil::class.java)
    private const val METADATA_URL = "https://fonts.google.com/metadata/fonts"
    private const val DOWNLOAD_URL = "https://fonts.google.com/download?family="
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(CIO) {
        followRedirects = true
    }

    suspend fun fetchFamilies(): List<GoogleFontFamily> {
        val metadata = requestText(METADATA_URL)
        val cleanJson = metadata.removePrefix(")]}'").trimStart()
        val root = json.parseToJsonElement(cleanJson).jsonObject
        val list = root["familyMetadataList"]?.jsonArray ?: return emptyList()

        return list.mapNotNull { element ->
            val obj = element.jsonObject
            val family = obj["family"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (family.isBlank()) {
                return@mapNotNull null
            }
            GoogleFontFamily(
                family = family,
                category = obj["category"]?.jsonPrimitive?.contentOrNull
            )
        }.sortedBy { it.family.lowercase() }
    }

    suspend fun downloadFamilyFonts(family: String): List<File> {
        val url = DOWNLOAD_URL + URLEncoder.encode(family, StandardCharsets.UTF_8)
        log.info("Downloading Google Fonts package: $url")
        val zipBytes = requestBytes(url)

        return withContext(Dispatchers.IO) {
            if (!zipBytes.looksLikeZip()) {
                throw IllegalStateException(
                    "Google Fonts response was not a ZIP file. " +
                        "Try 'Import Downloaded ZIP' with a file downloaded from fonts.google.com."
                )
            }
            extractFontsFromZip(
                zipProvider = { ZipInputStream(ByteArrayInputStream(zipBytes)) },
                directoryHint = family
            )
        }
    }

    suspend fun importFontsFromZip(zipFile: File): List<File> {
        return withContext(Dispatchers.IO) {
            if (!zipFile.exists() || !zipFile.isFile) {
                throw IllegalArgumentException("ZIP file not found: ${zipFile.absolutePath}")
            }
            extractFontsFromZip(
                zipProvider = { ZipInputStream(FileInputStream(zipFile)) },
                directoryHint = zipFile.nameWithoutExtension
            )
        }
    }

    private fun extractFontsFromZip(
        zipProvider: () -> ZipInputStream,
        directoryHint: String
    ): List<File> {
        val tempRoot = Files.createTempDirectory("fonthelper-google-fonts").toFile()
        val targetDir = File(tempRoot, directoryHint.replace(Regex("[^a-zA-Z0-9._-]"), "_")).apply {
            mkdirs()
        }

        zipProvider().use { zipInput ->
            var entry = zipInput.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name.substringAfterLast('/').substringAfterLast('\\')
                    if (entryName.isSupportedFontFile()) {
                        val outFile = uniqueFile(targetDir, entryName)
                        FileOutputStream(outFile).use { output ->
                            zipInput.copyTo(output)
                        }
                    }
                }
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }

        val files = targetDir.listFiles()
            ?.filter { it.isFile && it.name.isSupportedFontFile() }
            .orEmpty()
            .sortedBy { it.name.lowercase() }

        if (files.isEmpty()) {
            throw IllegalStateException("No .ttf or .otf files found in ZIP.")
        }
        return files
    }

    private fun uniqueFile(dir: File, name: String): File {
        val baseName = name.substringBeforeLast('.', missingDelimiterValue = name)
        val ext = name.substringAfterLast('.', missingDelimiterValue = "")
        var index = 0
        var candidate = File(dir, name)
        while (candidate.exists()) {
            index += 1
            val resolvedName = if (ext.isBlank()) "$baseName-$index" else "$baseName-$index.$ext"
            candidate = File(dir, resolvedName)
        }
        return candidate
    }

    private suspend fun requestText(url: String): String {
        return withContext(Dispatchers.IO) {
            val response = client.get(URI.create(url).toURL())
            if (!response.status.isSuccess()) {
                val errorMessage = response.bodyAsText()
                throw IllegalStateException(
                    "Google Fonts request failed: HTTP ${response.status.value} $errorMessage"
                )
            }
            response.bodyAsText()
        }
    }

    private suspend fun requestBytes(url: String): ByteArray {
        return withContext(Dispatchers.IO) {
            val response = client.get(URI.create(url).toURL())
            if (!response.status.isSuccess()) {
                val errorMessage = response.bodyAsText()
                throw IllegalStateException(
                    "Google Fonts request failed: HTTP ${response.status.value} $errorMessage"
                )
            }
            response.readRawBytes()
        }
    }

    private fun String.isSupportedFontFile(): Boolean {
        val lowered = lowercase()
        return lowered.endsWith(".ttf") || lowered.endsWith(".otf")
    }

    private fun ByteArray.looksLikeZip(): Boolean {
        if (size < 4) return false
        return this[0] == 'P'.code.toByte() && this[1] == 'K'.code.toByte()
    }
}
