package com.copilotomotorista.data.storage

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TripEvidenceFileManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun deletesGeneratedEvidenceInsidePrivateDirectory() {
        val filesDir = temporaryFolder.newFolder("app")
        val evidenceDirectory = File(filesDir, "trip_evidence").apply { mkdirs() }
        val evidence = File(evidenceDirectory, "offer_test.jpg").apply { writeText("image") }

        val deleted = TripEvidenceFileManager(filesDir).deleteIfOwned(evidence.absolutePath)

        assertTrue(deleted)
        assertFalse(evidence.exists())
    }

    @Test
    fun rejectsFileOutsideEvidenceDirectory() {
        val filesDir = temporaryFolder.newFolder("app")
        val outside = File(temporaryFolder.root, "offer_outside.jpg").apply { writeText("keep") }

        val deleted = TripEvidenceFileManager(filesDir).deleteIfOwned(outside.absolutePath)

        assertFalse(deleted)
        assertTrue(outside.exists())
    }

    @Test
    fun rejectsPathTraversalAndDoesNotDeleteTarget() {
        val filesDir = temporaryFolder.newFolder("app")
        val evidenceDirectory = File(filesDir, "trip_evidence").apply { mkdirs() }
        val outside = File(filesDir, "offer_escape.jpg").apply { writeText("keep") }
        val traversalPath = File(evidenceDirectory, "../offer_escape.jpg").path

        val deleted = TripEvidenceFileManager(filesDir).deleteIfOwned(traversalPath)

        assertFalse(deleted)
        assertTrue(outside.exists())
    }

    @Test
    fun rejectsUnexpectedFileNameInsideEvidenceDirectory() {
        val filesDir = temporaryFolder.newFolder("app")
        val evidenceDirectory = File(filesDir, "trip_evidence").apply { mkdirs() }
        val unrelated = File(evidenceDirectory, "keep.txt").apply { writeText("keep") }

        val deleted = TripEvidenceFileManager(filesDir).deleteIfOwned(unrelated.absolutePath)

        assertFalse(deleted)
        assertTrue(unrelated.exists())
    }

    @Test
    fun missingEvidenceIsAlreadyCleaned() {
        val filesDir = temporaryFolder.newFolder("app")
        val missing = File(File(filesDir, "trip_evidence"), "offer_missing.jpg")

        assertTrue(TripEvidenceFileManager(filesDir).deleteIfOwned(missing.absolutePath))
    }

    @Test
    fun nullEvidenceNeedsNoFileDeletion() {
        val filesDir = temporaryFolder.newFolder("app")

        assertTrue(TripEvidenceFileManager(filesDir).deleteIfOwned(null))
    }
}
