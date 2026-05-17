package com.duckgba.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Stores ROMs imported by the user inside the app private storage and
 * exposes them as a reactive list. Save files (battery RAM) and save states
 * are kept in sibling directories.
 */
class RomRepository(private val context: Context) {

    private val romsDir: File = File(context.filesDir, "roms").apply { mkdirs() }
    private val savesDir: File = File(context.filesDir, "saves").apply { mkdirs() }
    private val statesDir: File = File(context.filesDir, "states").apply { mkdirs() }

    private val _roms = MutableStateFlow<List<RomEntry>>(emptyList())
    val roms: StateFlow<List<RomEntry>> = _roms.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val list = romsDir.listFiles().orEmpty()
            .filter { it.isFile && SUPPORTED_EXTENSIONS.any { ext -> it.name.endsWith(ext, ignoreCase = true) } }
            .map { file ->
                val nameNoExt = file.nameWithoutExtension
                RomEntry(
                    id = sanitizeId(nameNoExt),
                    displayName = nameNoExt,
                    file = file,
                    sizeBytes = file.length(),
                    isColor = file.name.endsWith(".gbc", ignoreCase = true)
                )
            }
            .sortedBy { it.displayName.lowercase() }
        _roms.value = list
    }

    suspend fun importRom(uri: Uri, displayName: String?): Result<RomEntry> = withContext(Dispatchers.IO) {
        try {
            val resolvedName = displayName?.takeIf { it.isNotBlank() } ?: queryDisplayName(uri) ?: "rom_${System.currentTimeMillis()}"
            val ext = resolvedName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            val baseName = resolvedName.substringBeforeLast('.')
            val sanitizedBase = baseName.replace(Regex("[^A-Za-z0-9._\\- ]"), "_").trim().ifEmpty { "rom" }

            val target = when {
                ext == "zip" -> extractZipToRom(uri, sanitizedBase)
                ext == "gb" || ext == "gbc" || ext == "rom" -> copyToRom(uri, "$sanitizedBase.$ext")
                else -> copyToRom(uri, "$sanitizedBase.gb")
            }
            refresh()
            Result.success(roms.value.first { it.file == target })
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun deleteRom(entry: RomEntry): Boolean = withContext(Dispatchers.IO) {
        val deleted = entry.file.delete()
        // Also remove related save / state files
        savesDir.listFiles { _, name -> name.startsWith(entry.id) }?.forEach { it.delete() }
        statesDir.listFiles { _, name -> name.startsWith(entry.id) }?.forEach { it.delete() }
        refresh()
        deleted
    }

    fun saveFileFor(entry: RomEntry): File = File(savesDir, "${entry.id}.sav")

    fun stateFileFor(entry: RomEntry, slot: Int = 0): File = File(statesDir, "${entry.id}.state$slot")

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = try {
            context.contentResolver.query(uri, null, null, null, null)
        } catch (_: Throwable) {
            null
        } ?: return uri.lastPathSegment
        cursor.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex >= 0) {
                return it.getString(nameIndex)
            }
        }
        return uri.lastPathSegment
    }

    private fun copyToRom(uri: Uri, fileName: String): File {
        val target = uniqueTarget(fileName)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open input stream for $uri" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target
    }

    private fun extractZipToRom(uri: Uri, baseName: String): File {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open input stream for $uri" }
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (!entry.isDirectory && (ext == "gb" || ext == "gbc" || ext == "rom")) {
                        val target = uniqueTarget("$baseName.$ext")
                        target.outputStream().use { out -> zis.copyTo(out) }
                        return target
                    }
                    entry = zis.nextEntry
                }
            }
        }
        throw IOException("ZIP file does not contain a .gb / .gbc / .rom entry")
    }

    private fun uniqueTarget(fileName: String): File {
        var candidate = File(romsDir, fileName)
        if (!candidate.exists()) return candidate
        val baseName = fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
        val ext = fileName.substringAfterLast('.', missingDelimiterValue = "")
        var index = 1
        while (candidate.exists()) {
            val newName = if (ext.isEmpty()) "${baseName}_$index" else "${baseName}_$index.$ext"
            candidate = File(romsDir, newName)
            index++
        }
        return candidate
    }

    private fun sanitizeId(name: String): String =
        name.replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(120)

    companion object {
        private val SUPPORTED_EXTENSIONS = listOf(".gb", ".gbc", ".rom", ".zip")
    }
}
