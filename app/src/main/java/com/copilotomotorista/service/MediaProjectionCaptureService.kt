package com.copilotomotorista.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.nio.ByteBuffer

/**
 * User-consented screenshot fallback for Android 8–10.
 * One MediaProjection token creates exactly one VirtualDisplay; the same display
 * and ImageReader are reused for individual screenshots until the session stops.
 */
class MediaProjectionCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var pendingCallback: ((Bitmap?) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        captureThread = HandlerThread("LegacyScreenCapture").also { it.start() }
        captureHandler = Handler(captureThread!!.looper)
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultCode == Int.MIN_VALUE || resultData == null) {
            Log.e(TAG, "projection_start_rejected missing_consent_data")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = manager.getMediaProjection(resultCode, resultData)
            if (mediaProjection == null) {
                Log.e(TAG, "projection_start_failed projection_null")
                stopSelf()
                return START_NOT_STICKY
            }
            projection = mediaProjection
            mediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "projection_stopped_by_system")
                    releaseCaptureResources()
                    stopSelf()
                }
            }, captureHandler)

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            if (width <= 0 || height <= 0) {
                Log.e(TAG, "projection_start_failed invalid_display_size")
                stopSelf()
                return START_NOT_STICKY
            }

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            reader.setOnImageAvailableListener(::onImageAvailable, captureHandler)
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "CopilotoMotoristaScreenCapture",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                captureHandler
            )
            Log.i(TAG, "projection_ready width=$width height=$height")
        } catch (error: Exception) {
            Log.e(TAG, "projection_start_failed", error)
            releaseCaptureResources()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun onImageAvailable(reader: ImageReader) {
        val callback = synchronized(lock) {
            val current = pendingCallback ?: return
            pendingCallback = null
            current
        }
        val image = try {
            reader.acquireLatestImage()
        } catch (error: Exception) {
            Log.w(TAG, "image_acquire_failed", error)
            null
        }
        if (image == null) {
            callback(null)
            return
        }
        try {
            val plane = image.planes.firstOrNull() ?: run {
                callback(null)
                return
            }
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val paddedWidth = rowStride / pixelStride
            val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
            val buffer: ByteBuffer = plane.buffer
            buffer.rewind()
            padded.copyPixelsFromBuffer(buffer)
            val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
            if (cropped !== padded) padded.recycle()
            callback(cropped)
        } catch (error: Exception) {
            Log.e(TAG, "image_conversion_failed", error)
            callback(null)
        } finally {
            image.close()
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Copiloto Motorista ativo")
            .setContentText("Captura de tela autorizada para leitura local de ofertas.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Captura de tela",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun releaseCaptureResources() {
        synchronized(lock) {
            pendingCallback?.invoke(null)
            pendingCallback = null
        }
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        projection?.unregisterCallback(projectionCallback)
        projection?.stop()
        projection = null
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            releaseCaptureResources()
            stopSelf()
        }
    }

    override fun onDestroy() {
        releaseCaptureResources()
        if (instance === this) instance = null
        captureThread?.quitSafely()
        captureThread = null
        captureHandler = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LegacyScreenCapture"
        private const val CHANNEL_ID = "legacy_screen_capture"
        private const val NOTIFICATION_ID = 4102
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"
        private val lock = Any()
        @Volatile private var instance: MediaProjectionCaptureService? = null

        /** Returns false when the user has not enabled the capture session. */
        fun requestScreenshot(callback: (Bitmap?) -> Unit): Boolean {
            val service = instance ?: return false
            synchronized(lock) {
                if (service.pendingCallback != null || service.imageReader == null) return false
                service.pendingCallback = callback
            }
            service.captureHandler?.postDelayed({
                synchronized(lock) {
                    val pending = service.pendingCallback ?: return@postDelayed
                    service.pendingCallback = null
                    pending(null)
                }
            }, 1_500L)
            return true
        }
    }
}
