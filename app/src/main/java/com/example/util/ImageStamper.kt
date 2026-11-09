package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class StampPosition(val displayName: String) {
    BOTTOM_RIGHT("Abajo Derecha"),
    BOTTOM_LEFT("Abajo Izquierda"),
    BOTTOM_CENTER("Abajo Centro"),
    TOP_RIGHT("Arriba Derecha"),
    TOP_LEFT("Arriba Izquierda")
}

enum class StampBgStyle(val displayName: String) {
    GLASS_CARD("Tarjeta Glass"),
    SHADOW_ONLY("Solo Sombra (Limpio)"),
    DARK_BANNER("Franja Oscura")
}

data class StampConfig(
    // 5 Key TimeStamp Elements matching user request
    val showDate: Boolean = true,
    val showTime: Boolean = true,
    val showDateTime: Boolean = true,
    val dateString: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val timeString: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val dateTime: String = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()),
    val showLocationAddress: Boolean = true,
    val address: String = "",
    val showGpsCoordinates: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val showAltitude: Boolean = false,
    val altitude: Double? = null,
    val showCustomText: Boolean = true,
    val customText: String = "",
    // Appearance & Positioning
    val position: StampPosition = StampPosition.BOTTOM_RIGHT,
    val textColor: Int = Color.WHITE,
    val bgStyle: StampBgStyle = StampBgStyle.GLASS_CARD,
    // Legacy support fields
    val project: String = "",
    val author: String = "",
    val category: String = "",
    val notes: String = "",
    val style: String = "GLASS_CARD"
)

object ImageStamper {

    fun applyStamp(originalBitmap: Bitmap, config: StampConfig): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // Draw original photo
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        val scale = (width.coerceAtLeast(height) / 1200f).coerceIn(0.7f, 3.5f)

        // Gather active lines to stamp
        val lines = mutableListOf<String>()

        val dateLine = when {
            config.showDate && config.showTime -> {
                if (config.dateString.isNotBlank() && config.timeString.isNotBlank()) "${config.dateString} ${config.timeString}"
                else config.dateTime
            }
            config.showDate -> config.dateString.ifBlank { config.dateTime.split(" ").firstOrNull() ?: "" }
            config.showTime -> config.timeString.ifBlank { config.dateTime.split(" ").getOrNull(1) ?: "" }
            config.showDateTime && config.dateTime.isNotBlank() -> config.dateTime
            else -> ""
        }
        if (dateLine.isNotBlank()) {
            lines.add(dateLine)
        }

        if (config.showGpsCoordinates && config.latitude != null && config.longitude != null) {
            val altText = if (config.showAltitude && config.altitude != null) {
                String.format(Locale.US, " • Alt: %.1fm", config.altitude)
            } else ""
            lines.add(String.format(Locale.US, "GPS: %.5f, %.5f%s", config.latitude, config.longitude, altText))
        } else if (config.showAltitude && config.altitude != null) {
            lines.add(String.format(Locale.US, "Altitud: %.1fm", config.altitude))
        }

        if (config.showLocationAddress && config.address.isNotBlank()) {
            lines.add(config.address)
        }

        val extraCustom = when {
            config.showCustomText && config.customText.isNotBlank() -> config.customText
            config.notes.isNotBlank() -> config.notes
            config.project.isNotBlank() -> config.project
            else -> ""
        }
        if (extraCustom.isNotBlank()) {
            lines.add(extraCustom)
        }

        if (lines.isEmpty()) {
            return outputBitmap
        }

        // Font and layout metrics
        val textSize = 17f * scale
        val lineHeight = 28f * scale
        val padding = 16f * scale
        val margin = 20f * scale

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.textColor
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            if (config.bgStyle == StampBgStyle.SHADOW_ONLY) {
                setShadowLayer(6f * scale, 1.5f * scale, 1.5f * scale, Color.BLACK)
            }
        }

        // Measure max text width
        var maxTextWidth = 0f
        for (line in lines) {
            val w = textPaint.measureText(line)
            if (w > maxTextWidth) maxTextWidth = w
        }
        val maxAllowedWidth = width - (margin * 2) - (padding * 2)
        val boxWidth = minOf(maxTextWidth, maxAllowedWidth) + (padding * 2)
        val boxHeight = (lines.size * lineHeight) + (padding * 1.5f)

        // Calculate card coordinates based on position
        val (boxLeft, boxTop) = when (config.position) {
            StampPosition.BOTTOM_RIGHT -> Pair(width - margin - boxWidth, height - margin - boxHeight)
            StampPosition.BOTTOM_LEFT -> Pair(margin, height - margin - boxHeight)
            StampPosition.BOTTOM_CENTER -> Pair((width - boxWidth) / 2f, height - margin - boxHeight)
            StampPosition.TOP_RIGHT -> Pair(width - margin - boxWidth, margin)
            StampPosition.TOP_LEFT -> Pair(margin, margin)
        }
        val boxRight = boxLeft + boxWidth
        val boxBottom = boxTop + boxHeight

        // Draw Background
        when (config.bgStyle) {
            StampBgStyle.GLASS_CARD -> {
                val cardRect = RectF(boxLeft, boxTop, boxRight, boxBottom)
                // Frosted glass dark fill
                val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(175, 15, 23, 42) // Semi-transparent obsidian slate
                    style = Paint.Style.FILL
                }
                val cornerRadius = 14f * scale
                canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, glassPaint)

                // Subtle frosted border stroke
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(80, 255, 255, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f * scale
                }
                canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

                // Accent dot
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = config.textColor
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(boxLeft + (10f * scale), boxTop + padding + (lineHeight * 0.35f), 3f * scale, dotPaint)
            }
            StampBgStyle.DARK_BANNER -> {
                val bannerTop = if (config.position == StampPosition.TOP_LEFT || config.position == StampPosition.TOP_RIGHT) 0f else height - boxHeight - (margin * 1.5f)
                val bannerBottom = if (config.position == StampPosition.TOP_LEFT || config.position == StampPosition.TOP_RIGHT) boxHeight + (margin * 1.5f) else height.toFloat()
                val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(200, 0, 0, 0)
                }
                canvas.drawRect(0f, bannerTop, width.toFloat(), bannerBottom, bannerPaint)
            }
            StampBgStyle.SHADOW_ONLY -> {
                // No background box, only text drop shadows
            }
        }

        // Draw Text lines
        var currentY = boxTop + padding + (lineHeight * 0.7f)
        val textStartX = if (config.bgStyle == StampBgStyle.GLASS_CARD) boxLeft + padding + (8f * scale) else boxLeft + padding
        val textAvailableWidth = boxWidth - (padding * 2) - (10f * scale)

        for (line in lines) {
            val truncated = truncateText(line, textPaint, textAvailableWidth)
            canvas.drawText(truncated, textStartX, currentY, textPaint)
            currentY += lineHeight
        }

        return outputBitmap
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "..."
        val available = maxWidth - paint.measureText(ellipsis)
        if (available <= 0) return ""

        var low = 0
        var high = text.length
        var best = 0
        while (low <= high) {
            val mid = (low + high) / 2
            if (paint.measureText(text.substring(0, mid)) <= available) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return text.substring(0, best) + ellipsis
    }
}
