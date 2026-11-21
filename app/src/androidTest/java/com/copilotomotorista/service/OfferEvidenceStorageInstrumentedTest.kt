package com.copilotomotorista.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfferEvidenceStorageInstrumentedTest {
    @Test
    fun storesDecodableJpegOnlyInAppPrivateEvidenceDirectory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        var file: File? = null
        try {
            source.eraseColor(android.graphics.Color.GREEN)
            val path = OfferEvidenceStorage.save(context, source)
            assertNotNull("Evidence image must be persisted", path)
            file = File(requireNotNull(path))
            assertTrue(file.isFile)
            assertTrue(file.name.startsWith("offer_"))
            assertTrue(file.name.endsWith(".jpg"))
            assertTrue(file.canonicalFile.parentFile == File(context.filesDir, "trip_evidence").canonicalFile)
            val saved = BitmapFactory.decodeFile(file.absolutePath)
            assertNotNull("The saved evidence must remain a valid JPEG", saved)
            saved?.recycle()
        } finally {
            file?.delete()
            source.recycle()
        }
    }
}
