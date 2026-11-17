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
import android.view.Display
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.nio.ByteBuffer

/**
 * User-consented screenshot fallback for Android 8–10.
 * One MediaProjection token creates exactly one VirtualDisplay. Its ImageReader
 * is reused for screenshots and replaced only when the display dimensions change.
 */
class MediaProjectionCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var pendingCallback: ((Bitmap?) -> Unit)? = null
    private var nextCaptureRequestId = 0L
    private var pendingCaptureRequestId: Long? = null
    private var displayManager: DisplayManager? = null
    private var displayWidth = 0
    private var displayHeight = 0
    private var displayDensityDpi = 0

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                captureHandler?.post(::refreshDisplayConfiguration)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        captureThread = HandlerThread("LegacyScreenCapture").also { it.start() }
        captureHandler = Handler(captureThread!!.looper)
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (projection != null) {
            Log.w(TAG, "projection_start_ignored session_already_active")
            return START_NOT_STICKY
        }
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
            mediaProjection.registerCallback(projectionCallback, captureHandler)

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
            ) ?: throw IllegalStateException("VirtualDisplay creation returned null")
            displayWidth = width
            displayHeight = height
            displayDensityDpi = metrics.densityDpi
            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager?.registerDisplayListener(displayListener, captureHandler)
            setCaptureActive(true)
            Log.i(TAG, "projection_ready width=$width height=$height")
        } catch (error: Exception) {
            Log.e(TAG, "projection_start_failed", error)
            releaseCaptureResources()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun onImageAvailable(reader: ImageReader) {
        if (reader !== imageReader) {
            runCatching { reader.acquireLatestImage()?.close() }
            return
        }
        val image = try {
            reader.acquireLatestImage()
        } catch (error: Exception) {
            Log.w(TAG, "image_acquire_failed", error)
            null
        }
        val callback = synchronized(lock) {
            val current = pendingCallback
            pendingCallback = null
            pendingCaptureRequestId = null
            current
        }
        if (callback == null) {
            image?.close()
            return
        }
        val bitmap = try {
            if (image == null) {
                null
            } else {
                val plane = image.planes.firstOrNull()
                    ?: throw IllegalStateException("Captured image has no planes")
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                if (pixelStride <= 0 || rowStride <= 0) {
                    throw IllegalStateException("Captured image has invalid stride")
                }
                val paddedWidth = rowStride / pixelStride
                if (paddedWidth < image.width) {
                    throw IllegalStateException("Captured image row is narrower than its width")
                }
                val padded = Bitmap.createBitmap(
                    paddedWidth,
                    image.height,
                    Bitmap.Config.ARGB_8888
                )
                try {
                    val buffer: ByteBuffer = plane.buffer
                    buffer.rewind()
                    padded.copyPixelsFromBuffer(buffer)
                    Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
                } finally {
                    if (!padded.isRecycled) padded.recycle()
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "image_conversion_failed", error)
            null
        } finally {
            image?.close()
        }
        // Deliver exactly once, outside the conversion try/catch. A consumer callback
        // exception must never trigger a second callback with a null frame.
        callback(bitmap)
    }

    /**
     * Keeps the existing projection session and VirtualDisplay while adapting its
     * surface to rotation or resolution changes. A projection token must not create
     * a second VirtualDisplay during the same session.
     */
    private fun refreshDisplayConfiguration() {
        if (projection == null || virtualDisplay == null) return

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.getRealMetrics(metrics)
        val configuration = CaptureDisplayConfiguration(
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            densityDpi = metrics.densityDpi
        )
        if (!configuration.isValid || !configuration.requiresUpdateFrom(
                CaptureDisplayConfiguration(displayWidth, displayHeight, displayDensityDpi)
            )
        ) return
        val width = configuration.width
        val height = configuration.height
        val densityDpi = configuration.densityDpi

        val replacementReader = try {
            ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        } catch (error: Exception) {
            Log.e(TAG, "display_resize_reader_failed", error)
            return
        }
        replacementReader.setOnImageAvailableListener(::onImageAvailable, captureHandler)

        val pending = synchronized(lock) {
            val current = pendingCallback
            pendingCallback = null
            pendingCaptureRequestId = null
            current
        }
        pending?.invoke(null)

        try {
            val display = virtualDisplay ?: run {
                replacementReader.close()
                return
            }
            display.resize(width, height, densityDpi)
            display.setSurface(replacementReader.surface)

            val previousReader = imageReader
            imageReader = replacementReader
            previousReader?.setOnImageAvailableListener(null, null)
            previousReader?.close()
            displayWidth = width
            displayHeight = height
            displayDensityDpi = densityDpi
            Log.i(TAG, "projection_display_resized width=$width height=$height densityDpi=$densityDpi")
        } catch (error: Exception) {
            replacementReader.setOnImageAvailableListener(null, null)
            replacementReader.close()
            Log.e(TAG, "display_resize_failed", error)
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

    private fun releaseCaptureResources(stopProjection: Boolean = true) {
        setCaptureActive(false)
        val pending = synchronized(lock) {
            val current = pendingCallback
            pendingCallback = null
            pendingCaptureRequestId = null
            current
        }
        // Mark capture unavailable before notifying a pending request. Its callback
        // may drain queued work, but must not enqueue a new frame during teardown.
        displayManager?.unregisterDisplayListener(displayListener)
        displayManager = null
        displayWidth = 0
        displayHeight = 0
        displayDensityDpi = 0
        val reader = imageReader
        imageReader = null
        reader?.setOnImageAvailableListener(null, null)
        val display = virtualDisplay
        virtualDisplay = null
        display?.release()
        reader?.close()
        val activeProjection = projection
        projection = null
        activeProjection?.unregisterCallback(projectionCallback)
        if (stopProjection) activeProjection?.stop()
        pending?.invoke(null)
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "projection_stopped_by_system")
            releaseCaptureResources(stopProjection = false)
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
        const val ACTION_CAPTURE_STATE_CHANGED = "com.copilotomotorista.action.CAPTURE_STATE_CHANGED"
        const val EXTRA_CAPTURE_ACTIVE = "capture_active"
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"
        private val lock = Any()
        @Volatile private var instance: MediaProjectionCaptureService? = null
        @Volatile private var captureActive: Boolean = false

        fun isCaptureActive(): Boolean = captureActive

        private fun setCaptureActive(active: Boolean) {
            captureActive = active
            instance?.let { service ->
                val stateIntent = Intent(ACTION_CAPTURE_STATE_CHANGED)
                    .setPackage(service.packageName)
                    .putExtra(EXTRA_CAPTURE_ACTIVE, active)
                service.sendBroadcast(stateIntent)
            }
        }

        /** Returns false when the user has not enabled the capture session. */
        fun requestScreenshot(callback: (Bitmap?) -> Unit): Boolean {
            val service = instance ?: return false
            val handler = service.captureHandler ?: return false
            val requestId = synchronized(lock) {
                if (service.pendingCallback != null || service.imageReader == null) return false
                val nextId = ++service.nextCaptureRequestId
                service.pendingCallback = callback
                service.pendingCaptureRequestId = nextId
                nextId
            }
            val timeoutPosted = handler.postDelayed({
                val expired = synchronized(lock) {
                    if (service.pendingCaptureRequestId != requestId) {
                        null
                    } else {
                        val pending = service.pendingCallback
                        service.pendingCallback = null
                        service.pendingCaptureRequestId = null
                        pending
                    }
                }
                expired?.invoke(null)
            }, 1_500L)
            if (!timeoutPosted) {
                synchronized(lock) {
                    if (service.pendingCaptureRequestId == requestId) {
                        service.pendingCallback = null
                        service.pendingCaptureRequestId = null
                    }
                }
                return false
            }
            return true
        }
    }
}
