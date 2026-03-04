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

data class GoogleFontFileRef(
    val filename: String,
    val url: String
)

object GoogleFontsUtil {
    private val log = Logger.getInstance(GoogleFontsUtil::class.java)
    private const val METADATA_URL = "https://fonts.google.com/metadata/fonts"
    private const val DOWNLOAD_LIST_URL = "https://fonts.google.com/download/list?family="
    private const val MANIFEST_CACHE_TTL_MS = 10 * 60 * 1000L
    private val json = Json { ignoreUnknownKeys = true }
    private val manifestCache = mutableMapOf<String, ManifestCacheEntry>()
    private val cacheLock = Any()
    private val downloadCacheRoot: File by lazy {
        File(System.getProperty("java.io.tmpdir"), "fonthelper-google-fonts-cache").apply {
            mkdirs()
        }
    }

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
        val encoded = URLEncoder.encode(family, StandardCharsets.UTF_8)
        val listUrl = DOWNLOAD_LIST_URL + encoded
        log.info("Downloading Google Fonts manifest: $listUrl")

        val refs = requestFontFileRefs(listUrl)
        if (refs.isNotEmpty()) {
            return downloadFromFileRefs(
                family = family,
                fileRefs = refs.map { GoogleFontFileRef(filename = it.first, url = it.second) }
            ).values.toList()
        }
        throw IllegalStateException("No downloadable font refs found in Google Fonts manifest.")
    }

    suspend fun fetchFamilyFontFileRefs(family: String): List<GoogleFontFileRef> {
        val key = family.trim().lowercase()
        synchronized(cacheLock) {
            val cached = manifestCache[key]
            if (cached != null && System.currentTimeMillis() - cached.cachedAtMs <= MANIFEST_CACHE_TTL_MS) {
                return cached.refs
            }
        }

        val encoded = URLEncoder.encode(family, StandardCharsets.UTF_8)
        val listUrl = DOWNLOAD_LIST_URL + encoded
        log.info("Fetching Google Fonts manifest only: $listUrl")
        val refs = requestFontFileRefs(listUrl).map { GoogleFontFileRef(filename = it.first, url = it.second) }
        synchronized(cacheLock) {
            manifestCache[key] = ManifestCacheEntry(
                cachedAtMs = System.currentTimeMillis(),
                refs = refs
            )
        }
        return refs
    }

    suspend fun downloadFamilyFontRefs(
        family: String,
        refs: List<GoogleFontFileRef>
    ): Map<String, File> {
        return downloadFromFileRefs(family = family, fileRefs = refs)
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

    private suspend fun downloadFromFileRefs(
        family: String,
        fileRefs: List<GoogleFontFileRef>
    ): Map<String, File> {
        return withContext(Dispatchers.IO) {
            val targetDir = File(
                downloadCacheRoot,
                family.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            ).apply {
                mkdirs()
            }
            val downloadedByUrl = linkedMapOf<String, File>()

            fileRefs.forEach { ref ->
                val filename = ref.filename
                val url = ref.url
                val fileName = filename
                    .replace('\\', '/')
                    .trimStart('/')
                    .replace("/", "__")
                if (!fileName.isSupportedFontFile()) return@forEach
                val outFile = File(targetDir, fileName)
                if (!outFile.exists() || outFile.length() == 0L) {
                    log.info("Downloading Google Fonts file: $url")
                    val bytes = requestBytes(url)
                    FileOutputStream(outFile).use { output ->
                        output.write(bytes)
                    }
                } else {
                    log.info("Using cached Google Fonts file: ${outFile.absolutePath} (source: $url)")
                }
                downloadedByUrl[url] = outFile
            }

            if (downloadedByUrl.isEmpty()) {
                throw IllegalStateException("No .ttf or .otf files found in manifest download.")
            }
            downloadedByUrl
        }
    }

    suspend fun clearDownloadedCache(): Int {
        return withContext(Dispatchers.IO) {
            var deleted = 0
            if (downloadCacheRoot.exists()) {
                deleted = deleteRecursivelyCount(downloadCacheRoot)
            }
            downloadCacheRoot.mkdirs()
            synchronized(cacheLock) {
                manifestCache.clear()
            }
            deleted
        }
    }

    fun isManagedDownloadedFile(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        return normalized.contains("/fonthelper-google-fonts-cache/")
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

    private suspend fun requestFontFileRefs(url: String): List<Pair<String, String>> {
        val body = requestText(url)
        val cleanJson = body.removePrefix(")]}'").trimStart()
        val root = json.parseToJsonElement(cleanJson).jsonObject
        val refs = root["manifest"]
            ?.jsonObject
            ?.get("fileRefs")
            ?.jsonArray
            .orEmpty()

        return refs.mapNotNull { element ->
            val obj = element.jsonObject
            val filename = obj["filename"]?.jsonPrimitive?.contentOrNull
            val fileUrl = obj["url"]?.jsonPrimitive?.contentOrNull
            if (filename.isNullOrBlank() || fileUrl.isNullOrBlank()) null else filename to fileUrl
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

    private fun deleteRecursivelyCount(file: File): Int {
        var count = 0
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                count += deleteRecursivelyCount(child)
            }
        }
        if (file.delete()) {
            count += 1
        }
        return count
    }

    private data class ManifestCacheEntry(
        val cachedAtMs: Long,
        val refs: List<GoogleFontFileRef>
    )

}
