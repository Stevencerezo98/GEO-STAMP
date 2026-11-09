package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.util.Locale

data class CollageConfig(
    val title: String = "",
    val subtitle: String = "",
    val layoutType: CollageLayout = CollageLayout.GRID_2X2,
    val backgroundColor: Int = Color.WHITE,
    val spacing: Int = 16,
    val cornerRadius: Float = 12f,
    val targetWidth: Int = 1800,
    val targetHeight: Int = 2400
)

enum class CollageLayout(val displayName: String, val photoCount: Int) {
    HORIZONTAL_2("2 Fotos (Horizontal)", 2),
    VERTICAL_2("2 Fotos (Vertical)", 2),
    ONE_TOP_TWO_BOTTOM("3 Fotos (1 Arriba, 2 Abajo)", 3),
    GRID_2X2("4 Fotos (Cuadrícula 2x2)", 4),
    GRID_2X3("6 Fotos (3 Filas x 2 Col)", 6),
    GRID_3X3("9 Fotos (Cuadrícula 3x3)", 9)
}

object CollageMaker {

    fun createCollage(bitmaps: List<Bitmap>, config: CollageConfig): Bitmap {
        val width = config.targetWidth
        val height = config.targetHeight
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw background
        canvas.drawColor(config.backgroundColor)

        // Reserve top header space if title is provided
        val hasHeader = config.title.isNotBlank()
        val headerHeight = if (hasHeader) 160f else 40f

        if (hasHeader) {
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (config.backgroundColor == Color.WHITE) Color.parseColor("#0F172A") else Color.WHITE
                textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(config.title, width / 2f, 75f, titlePaint)

            if (config.subtitle.isNotBlank()) {
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (config.backgroundColor == Color.WHITE) Color.parseColor("#64748B") else Color.parseColor("#94A3B8")
                    textSize = 28f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(config.subtitle, width / 2f, 120f, subPaint)
            }
        }

        val contentRect = RectF(
            config.spacing.toFloat(),
            headerHeight,
            width - config.spacing.toFloat(),
            height - config.spacing.toFloat()
        )

        val slots = calculateSlots(config.layoutType, contentRect, config.spacing.toFloat())

        for (i in slots.indices) {
            if (i < bitmaps.size) {
                drawCroppedBitmap(canvas, bitmaps[i], slots[i], config.cornerRadius)
            } else {
                // Draw empty slot placeholder
                val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (config.backgroundColor == Color.WHITE) Color.parseColor("#F1F5F9") else Color.parseColor("#1E293B")
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(slots[i], config.cornerRadius, config.cornerRadius, emptyPaint)
            }
        }

        return output
    }

    private fun calculateSlots(layout: CollageLayout, bounds: RectF, spacing: Float): List<RectF> {
        val slots = mutableListOf<RectF>()
        val totalW = bounds.width()
        val totalH = bounds.height()

        when (layout) {
            CollageLayout.HORIZONTAL_2 -> {
                val slotW = (totalW - spacing) / 2f
                slots.add(RectF(bounds.left, bounds.top, bounds.left + slotW, bounds.bottom))
                slots.add(RectF(bounds.left + slotW + spacing, bounds.top, bounds.right, bounds.bottom))
            }
            CollageLayout.VERTICAL_2 -> {
                val slotH = (totalH - spacing) / 2f
                slots.add(RectF(bounds.left, bounds.top, bounds.right, bounds.top + slotH))
                slots.add(RectF(bounds.left, bounds.top + slotH + spacing, bounds.right, bounds.bottom))
            }
            CollageLayout.ONE_TOP_TWO_BOTTOM -> {
                val topH = (totalH - spacing) * 0.52f
                val botH = (totalH - spacing) * 0.48f
                val botW = (totalW - spacing) / 2f

                // 1 top large
                slots.add(RectF(bounds.left, bounds.top, bounds.right, bounds.top + topH))
                // 2 bottom
                val botTop = bounds.top + topH + spacing
                slots.add(RectF(bounds.left, botTop, bounds.left + botW, bounds.bottom))
                slots.add(RectF(bounds.left + botW + spacing, botTop, bounds.right, bounds.bottom))
            }
            CollageLayout.GRID_2X2 -> {
                val slotW = (totalW - spacing) / 2f
                val slotH = (totalH - spacing) / 2f
                slots.add(RectF(bounds.left, bounds.top, bounds.left + slotW, bounds.top + slotH))
                slots.add(RectF(bounds.left + slotW + spacing, bounds.top, bounds.right, bounds.top + slotH))
                slots.add(RectF(bounds.left, bounds.top + slotH + spacing, bounds.left + slotW, bounds.bottom))
                slots.add(RectF(bounds.left + slotW + spacing, bounds.top + slotH + spacing, bounds.right, bounds.bottom))
            }
            CollageLayout.GRID_2X3 -> {
                val cols = 2
                val rows = 3
                val slotW = (totalW - (spacing * (cols - 1))) / cols
                val slotH = (totalH - (spacing * (rows - 1))) / rows
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val left = bounds.left + c * (slotW + spacing)
                        val top = bounds.top + r * (slotH + spacing)
                        slots.add(RectF(left, top, left + slotW, top + slotH))
                    }
                }
            }
            CollageLayout.GRID_3X3 -> {
                val cols = 3
                val rows = 3
                val slotW = (totalW - (spacing * (cols - 1))) / cols
                val slotH = (totalH - (spacing * (rows - 1))) / rows
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val left = bounds.left + c * (slotW + spacing)
                        val top = bounds.top + r * (slotH + spacing)
                        slots.add(RectF(left, top, left + slotW, top + slotH))
                    }
                }
            }
        }
        return slots
    }

    private fun drawCroppedBitmap(canvas: Canvas, bitmap: Bitmap, target: RectF, cornerRadius: Float) {
        val srcW = bitmap.width
        val srcH = bitmap.height
        val dstW = target.width()
        val dstH = target.height()

        // Draw elegant card backing for the slot
        val slotBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
        }
        canvas.drawRoundRect(target, cornerRadius, cornerRadius, slotBgPaint)

        // Fit entire photo inside target slot to guarantee the watermark stamp is 100% visible
        val scale = minOf(dstW / srcW.toFloat(), dstH / srcH.toFloat())
        val fitW = srcW * scale
        val fitH = srcH * scale
        val fitLeft = target.left + (dstW - fitW) / 2f
        val fitTop = target.top + (dstH - fitH) / 2f
        val fitRect = RectF(fitLeft, fitTop, fitLeft + fitW, fitTop + fitH)

        canvas.save()
        if (cornerRadius > 0) {
            val path = android.graphics.Path().apply {
                addRoundRect(target, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(path)
        }
        canvas.drawBitmap(bitmap, null, fitRect, Paint(Paint.FILTER_BITMAP_FLAG))

        // Subtle thin luminous border around slot
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(45, 255, 255, 255)
        }
        canvas.drawRoundRect(target, cornerRadius, cornerRadius, borderPaint)
        canvas.restore()
    }
}
