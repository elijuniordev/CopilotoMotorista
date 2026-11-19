package com.copilotomotorista.data.storage

import java.io.File

/**
 * Deletes only evidence files created directly inside the app's private trip_evidence directory.
 * A false result means the path was not owned by this manager or deletion failed.
 */
internal class TripEvidenceFileManager(filesDir: File) {
    private val evidenceDirectory = File(filesDir, DIRECTORY_NAME)

    fun deleteIfOwned(path: String?): Boolean {
        if (path.isNullOrBlank()) return true

        return runCatching {
            val canonicalDirectory = evidenceDirectory.canonicalFile
            val candidate = File(path).canonicalFile

            // Only generated JPEGs directly inside trip_evidence are eligible.
            if (candidate.parentFile != canonicalDirectory ||
                !candidate.name.startsWith(FILE_PREFIX) ||
                !candidate.name.endsWith(FILE_SUFFIX)
            ) {
                return false
            }

            if (!candidate.exists()) return true
            if (!candidate.isFile) return false

            candidate.delete() || !candidate.exists()
        }.getOrDefault(false)
    }

    private companion object {
        const val DIRECTORY_NAME = "trip_evidence"
        const val FILE_PREFIX = "offer_"
        const val FILE_SUFFIX = ".jpg"
    }
}
