package com.wonddak.fonthelper.util

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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
        val zipBytes = requestBytes(url)

        return withContext(Dispatchers.IO) {
            val tempRoot = Files.createTempDirectory("fonthelper-google-fonts").toFile()
            val targetDir = File(tempRoot, family.replace(Regex("[^a-zA-Z0-9._-]"), "_")).apply {
                mkdirs()
            }

            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipInput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.substringAfterLast('/')
                        if (name.isSupportedFontFile()) {
                            val outFile = File(targetDir, name)
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
                throw IllegalStateException("No font files found in Google Fonts package.")
            }
            files
        }
    }

    private suspend fun requestText(url: String): String {
        val response = client.get(URI.create(url).toURL())
        if (!response.status.isSuccess()) {
            val errorMessage = response.bodyAsText()
            throw IllegalStateException(
                "Google Fonts request failed: HTTP ${response.status.value} $errorMessage"
            )
        }
        return response.bodyAsText()
    }

    private suspend fun requestBytes(url: String): ByteArray {
        val response = client.get(URI.create(url).toURL())
        if (!response.status.isSuccess()) {
            val errorMessage = response.bodyAsText()
            throw IllegalStateException(
                "Google Fonts request failed: HTTP ${response.status.value} $errorMessage"
            )
        }
        return response.body()
    }

    private fun String.isSupportedFontFile(): Boolean {
        val lowered = lowercase()
        return lowered.endsWith(".ttf") || lowered.endsWith(".otf")
    }
}
