package com.duckgba.data

import java.io.File

/**
 * Metadata representing a single ROM available in the user's library.
 *
 * @property id Stable identifier (file name without extension, sanitized).
 * @property displayName Title shown on the UI.
 * @property file Local file location inside the app sandbox.
 * @property sizeBytes Size of the ROM file.
 * @property isColor Whether the ROM is a Game Boy Color title.
 */
data class RomEntry(
    val id: String,
    val displayName: String,
    val file: File,
    val sizeBytes: Long,
    val isColor: Boolean
)
