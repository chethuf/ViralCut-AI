
package com.viralcut.ai

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private val PICK_VIDEO = 100
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(30, 30, 30, 30)
        layout.setBackgroundColor(Color.rgb(15, 18, 30))

        val title = TextView(this)
        title.text = "VIRALCUT AI"
        title.textSize = 30f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER

        val subtitle = TextView(this)
        subtitle.text = "Turn Long Videos Into Viral Shorts"
        subtitle.textSize = 15f
        subtitle.setTextColor(Color.LTGRAY)
        subtitle.gravity = Gravity.CENTER

        val selectButton = Button(this)
        selectButton.text = "SELECT VIDEO"

        status = TextView(this)
        status.text = "No video selected"
        status.textSize = 15f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER
        status.setPadding(10, 30, 10, 30)

        selectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "video/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            startActivityForResult(intent, PICK_VIDEO)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(selectButton)
        layout.addView(status)

        setContentView(layout)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK) {
            val videoUri: Uri? = data?.data

            if (videoUri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        videoUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                var fileName = "Selected video"

                contentResolver.query(
                    videoUri,
                    null,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val column = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )
                    if (cursor.moveToFirst() && column >= 0) {
                        fileName = cursor.getString(column)
                    }
                }

                status.text = "VIDEO SELECTED\n\n$fileName\n\nReady for processing."
            }
        }
    }
}
