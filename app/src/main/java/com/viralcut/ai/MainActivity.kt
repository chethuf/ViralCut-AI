
package com.viralcut.ai

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.media.MediaMetadataRetriever
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import android.os.Handler
import android.os.Looper
import java.util.Locale

class MainActivity : Activity() {

    private val PICK_VIDEO = 100
    private var videoUri: Uri? = null
    private var processing = false
    private var startTime = 0L

    private lateinit var status: TextView
    private lateinit var timer: TextView
    private lateinit var details: TextView
    private lateinit var startButton: Button
    private lateinit var progress: ProgressBar

    private val handler = Handler(Looper.getMainLooper())

    private val timerTask = object : Runnable {
        override fun run() {
            if (processing) {
                val seconds =
                    (System.currentTimeMillis() - startTime) / 1000
                timer.text = "Time elapsed: ${formatTime(seconds)}"
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(25, 25, 25, 25)
            setBackgroundColor(Color.rgb(15, 18, 30))
        }

        fun makeText(value: String, size: Float): TextView {
            return TextView(this).apply {
                text = value
                textSize = size
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(8, 12, 8, 12)
            }
        }

        val title = makeText("VIRALCUT AI", 30f)
        val subtitle =
            makeText("Turn Long Videos Into Viral Shorts", 16f)

        val selectButton = Button(this).apply {
            text = "SELECT VIDEO"
        }

        startButton = Button(this).apply {
            text = "START PROCESSING"
            isEnabled = false
        }

        status = makeText("No video selected", 17f)
        timer = makeText("Time elapsed: 00:00", 17f)
        details = makeText("", 15f)

        progress = ProgressBar(
            this, null, android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
            visibility = View.GONE
        }

        selectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            startActivityForResult(intent, PICK_VIDEO)
        }

        startButton.setOnClickListener {
            inspectVideo()
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(selectButton)
        layout.addView(status)
        layout.addView(startButton)
        layout.addView(timer)
        layout.addView(progress)
        layout.addView(details)

        setContentView(layout)
    }

    private fun inspectVideo() {
        val uri = videoUri ?: return
        if (processing) return

        processing = true
        startTime = System.currentTimeMillis()

        startButton.isEnabled = false
        status.text = "INSPECTING VIDEO..."
        details.text = "Reading video information..."
        progress.visibility = View.VISIBLE
        progress.isIndeterminate = true

        handler.post(timerTask)

        Thread {
            val retriever = MediaMetadataRetriever()

            try {
                retriever.setDataSource(this, uri)

                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L

                val width = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                ) ?: "Unknown"

                val height = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                ) ?: "Unknown"

                val elapsed =
                    (System.currentTimeMillis() - startTime) / 1000

                runOnUiThread {
                    processing = false
                    handler.removeCallbacks(timerTask)

                    status.text = "VIDEO INSPECTION COMPLETED"
                    timer.text =
                        "Processing time: ${formatTime(elapsed)}"

                    details.text =
                        "Video duration: ${formatTime(duration / 1000)}\n" +
                        "Resolution: $width × $height\n\n" +
                        "Inspection completed successfully."

                    progress.isIndeterminate = false
                    progress.progress = 100
                    startButton.text = "INSPECTION COMPLETE"
                    startButton.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    processing = false
                    handler.removeCallbacks(timerTask)

                    status.text = "INSPECTION FAILED"
                    details.text =
                        e.message ?: "Unable to read this video."

                    progress.visibility = View.GONE
                    startButton.text = "TRY AGAIN"
                    startButton.isEnabled = true
                }
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }.start()
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format(
                Locale.US, "%02d:%02d:%02d",
                hours, minutes, secs
            )
        } else {
            String.format(
                Locale.US, "%02d:%02d",
                minutes, secs
            )
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            videoUri = uri

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            status.text = "VIDEO SELECTED"
            details.text = "Ready for inspection."
            timer.text = "Time elapsed: 00:00"

            progress.visibility = View.GONE
            startButton.text = "START PROCESSING"
            startButton.isEnabled = true
        }
    }

    override fun onDestroy() {
        processing = false
        handler.removeCallbacks(timerTask)
        super.onDestroy()
    }
}
