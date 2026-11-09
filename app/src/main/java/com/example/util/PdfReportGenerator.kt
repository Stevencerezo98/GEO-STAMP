package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.StampedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfReportConfig(
    val title: String = "Reporte Fotográfico",
    val subtitle: String = "Informe de Registro de Campo y Evidencias",
    val author: String = "",
    val organization: String = "",
    val layoutFormat: ReportLayout = ReportLayout.ONE_PER_PAGE,
    val includeGps: Boolean = true,
    val includeNotes: Boolean = true,
    val primaryColor: Int = Color.parseColor("#1E3A8A") // Deep Navy
)

enum class ReportLayout(val displayName: String, val photosPerPage: Int) {
    ONE_PER_PAGE("1 Foto por página (Detallado)", 1),
    TWO_PER_PAGE("2 Fotos por página (Ejecutivo)", 2),
    FOUR_PER_PAGE("4 Fotos por página (Cuadrícula)", 4)
}

object PdfReportGenerator {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    suspend fun generatePhotoReport(
        context: Context,
        photos: List<StampedPhoto>,
        config: PdfReportConfig
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val photosPerPage = config.layoutFormat.photosPerPage
        val totalPages = if (photos.isEmpty()) 1 else ((photos.size + photosPerPage - 1) / photosPerPage)

        var photoIndex = 0

        for (pageNumber in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Draw Page Background
            canvas.drawColor(Color.WHITE)

            // Draw Header Banner
            drawHeader(canvas, config, pageNumber, totalPages)

            // Draw Footer
            drawFooter(canvas, pageNumber, totalPages)

            // Draw Photos based on layout
            val contentTop = 95f
            val contentBottom = PAGE_HEIGHT - 45f

            when (config.layoutFormat) {
                ReportLayout.ONE_PER_PAGE -> {
                    if (photoIndex < photos.size) {
                        drawOnePhotoLayout(context, canvas, photos[photoIndex], contentTop, contentBottom, config)
                        photoIndex++
                    }
                }
                ReportLayout.TWO_PER_PAGE -> {
                    val halfHeight = (contentBottom - contentTop - 20f) / 2f
                    for (i in 0 until 2) {
                        if (photoIndex < photos.size) {
                            val slotTop = contentTop + i * (halfHeight + 20f)
                            drawTwoPhotoSlot(context, canvas, photos[photoIndex], slotTop, halfHeight, config)
                            photoIndex++
                        }
                    }
                }
                ReportLayout.FOUR_PER_PAGE -> {
                    val cols = 2
                    val rows = 2
                    val colWidth = (PAGE_WIDTH - 60f - 20f) / cols
                    val rowHeight = (contentBottom - contentTop - 20f) / rows
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            if (photoIndex < photos.size) {
                                val left = 30f + c * (colWidth + 20f)
                                val top = contentTop + r * (rowHeight + 20f)
                                drawFourPhotoSlot(context, canvas, photos[photoIndex], left, top, colWidth, rowHeight)
                                photoIndex++
                            }
                        }
                    }
                }
            }

            pdfDocument.finishPage(page)
        }

        val outputFile = FileUtils.createPdfFile(context, config.title)
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        outputFile
    }

    suspend fun convertImagesToPdf(
        context: Context,
        imageFiles: List<File>,
        title: String = "Documento"
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        for ((index, file) in imageFiles.withIndex()) {
            val bitmap = FileUtils.loadBitmapFromFile(file.absolutePath, 1600) ?: continue
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            // Header line
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("$title - Imagen ${index + 1} de ${imageFiles.size}", 30f, 25f, titlePaint)

            // Scale and draw bitmap inside margin
            val margin = 30f
            val maxW = PAGE_WIDTH - (margin * 2)
            val maxH = PAGE_HEIGHT - 60f

            val scale = minOf(maxW / bitmap.width.toFloat(), maxH / bitmap.height.toFloat())
            val drawW = bitmap.width * scale
            val drawH = bitmap.height * scale
            val left = (PAGE_WIDTH - drawW) / 2f
            val top = 35f + (maxH - drawH) / 2f

            val dstRect = RectF(left, top, left + drawW, top + drawH)
            canvas.drawBitmap(bitmap, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

            pdfDocument.finishPage(page)
        }

        val outputFile = FileUtils.createPdfFile(context, title)
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        outputFile
    }

    suspend fun exportCollageToPdf(
        context: Context,
        collageBitmap: Bitmap,
        title: String = "Collage Fotográfico"
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        // Draw modern header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E3A8A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 45f, headerPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, 25f, 28f, headerTextPaint)

        val margin = 25f
        val maxW = PAGE_WIDTH - (margin * 2)
        val maxH = PAGE_HEIGHT - 80f

        val scale = minOf(maxW / collageBitmap.width.toFloat(), maxH / collageBitmap.height.toFloat())
        val drawW = collageBitmap.width * scale
        val drawH = collageBitmap.height * scale
        val left = (PAGE_WIDTH - drawW) / 2f
        val top = 55f + (maxH - drawH) / 2f

        val dstRect = RectF(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(collageBitmap, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Exportado desde FotoReporte • $dateStr", PAGE_WIDTH / 2f, PAGE_HEIGHT - 15f, footerPaint)

        pdfDocument.finishPage(page)

        val outputFile = FileUtils.createPdfFile(context, title)
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        outputFile
    }

    private fun drawHeader(canvas: Canvas, config: PdfReportConfig, pageNumber: Int, totalPages: Int) {
        // Top banner
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.primaryColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 65f, bannerPaint)

        // Accent strip
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0284C7") // Sky blue
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 65f, PAGE_WIDTH.toFloat(), 69f, accentPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(config.title, 30f, 32f, titlePaint)

        // Subtitle & Organization
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 10f
        }
        val subText = listOfNotNull(
            config.subtitle.takeIf { it.isNotBlank() },
            config.organization.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
        canvas.drawText(subText.ifBlank { "Reporte de Campo y Evidencias" }, 30f, 50f, subPaint)

        // Page info & date right aligned
        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Fecha: $dateStr", PAGE_WIDTH - 30f, 32f, rightPaint)
        if (config.author.isNotBlank()) {
            canvas.drawText("Inspector: ${config.author}", PAGE_WIDTH - 30f, 50f, rightPaint)
        }
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }
        canvas.drawLine(30f, PAGE_HEIGHT - 35f, PAGE_WIDTH - 30f, PAGE_HEIGHT - 35f, linePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9f
        }
        canvas.drawText("FotoReporte - Documento Generado Oficialmente", 30f, PAGE_HEIGHT - 18f, footerPaint)

        footerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Página $pageNumber de $totalPages", PAGE_WIDTH - 30f, PAGE_HEIGHT - 18f, footerPaint)
    }

    private fun drawOnePhotoLayout(
        context: Context,
        canvas: Canvas,
        photo: StampedPhoto,
        top: Float,
        bottom: Float,
        config: PdfReportConfig
    ) {
        val bitmap = FileUtils.loadBitmapFromFile(photo.filePath, 1200)

        val margin = 30f
        val photoW = PAGE_WIDTH - (margin * 2)
        val photoMaxH = (bottom - top) * 0.58f

        if (bitmap != null) {
            val scale = minOf(photoW / bitmap.width.toFloat(), photoMaxH / bitmap.height.toFloat())
            val actualW = bitmap.width * scale
            val actualH = bitmap.height * scale
            val left = margin + (photoW - actualW) / 2f
            val photoTop = top + 10f

            // Photo frame
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            val rect = RectF(left, photoTop, left + actualW, photoTop + actualH)
            canvas.drawBitmap(bitmap, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.drawRoundRect(rect, 4f, 4f, borderPaint)

            // Metadata Card below photo
            val cardTop = photoTop + actualH + 16f
            val cardBottom = bottom - 5f
            drawMetadataCard(canvas, photo, margin, cardTop, PAGE_WIDTH - margin, cardBottom, config)
        } else {
            // Draw placeholder and metadata
            drawMetadataCard(canvas, photo, margin, top + 20f, PAGE_WIDTH - margin, bottom - 10f, config)
        }
    }

    private fun drawTwoPhotoSlot(
        context: Context,
        canvas: Canvas,
        photo: StampedPhoto,
        slotTop: Float,
        slotHeight: Float,
        config: PdfReportConfig
    ) {
        val margin = 30f
        val slotWidth = PAGE_WIDTH - (margin * 2)
        val bitmap = FileUtils.loadBitmapFromFile(photo.filePath, 800)

        // Photo on left, metadata table on right
        val photoBoxW = slotWidth * 0.44f
        val photoBoxH = slotHeight - 10f

        if (bitmap != null) {
            val scale = minOf(photoBoxW / bitmap.width.toFloat(), photoBoxH / bitmap.height.toFloat())
            val actualW = bitmap.width * scale
            val actualH = bitmap.height * scale
            val photoLeft = margin + (photoBoxW - actualW) / 2f
            val photoTop = slotTop + (photoBoxH - actualH) / 2f

            val rect = RectF(photoLeft, photoTop, photoLeft + actualW, photoTop + actualH)
            canvas.drawBitmap(bitmap, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(rect, 4f, 4f, borderPaint)
        }

        // Metadata on right
        val rightLeft = margin + photoBoxW + 16f
        val rightRight = PAGE_WIDTH - margin
        drawCompactMetadata(canvas, photo, rightLeft, slotTop, rightRight, slotTop + slotHeight)

        // Separator below slot
        val sepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            strokeWidth = 1f
        }
        canvas.drawLine(margin, slotTop + slotHeight + 8f, PAGE_WIDTH - margin, slotTop + slotHeight + 8f, sepPaint)
    }

    private fun drawFourPhotoSlot(
        context: Context,
        canvas: Canvas,
        photo: StampedPhoto,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        val bitmap = FileUtils.loadBitmapFromFile(photo.filePath, 600)
        val photoH = height * 0.65f

        if (bitmap != null) {
            val scale = minOf(width / bitmap.width.toFloat(), photoH / bitmap.height.toFloat())
            val actualW = bitmap.width * scale
            val actualH = bitmap.height * scale
            val pLeft = left + (width - actualW) / 2f
            val pTop = top + (photoH - actualH) / 2f
            val rect = RectF(pLeft, pTop, pLeft + actualW, pTop + actualH)
            canvas.drawBitmap(bitmap, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        val textTop = top + photoH + 6f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 8.5f
        }

        val title = photo.title.ifBlank { photo.category }.ifBlank { "Foto" }
        canvas.drawText(title.take(28), left, textTop + 8f, titlePaint)
        canvas.drawText(photo.formattedDateTime, left, textTop + 20f, infoPaint)
        if (photo.latitude != null && photo.longitude != null) {
            canvas.drawText(String.format(Locale.US, "GPS: %.4f, %.4f", photo.latitude, photo.longitude), left, textTop + 31f, infoPaint)
        }
    }

    private fun drawMetadataCard(
        canvas: Canvas,
        photo: StampedPhoto,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        config: PdfReportConfig
    ) {
        val cardRect = RectF(left, top, right, bottom)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0284C7")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val title = photo.title.ifBlank { "Detalles de la Fotografía" }
        canvas.drawText(title, left + 14f, top + 22f, headerPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
        }

        var y = top + 42f
        val lineSpacing = 19f

        val items = mutableListOf<Pair<String, String>>()
        items.add("Fecha y Hora:" to photo.formattedDateTime)

        if (config.includeGps && photo.latitude != null && photo.longitude != null) {
            val alt = if (photo.altitude != null) " (Altitud: ${String.format(Locale.US, "%.1fm", photo.altitude)})" else ""
            items.add("Coordenadas GPS:" to "${String.format(Locale.US, "%.5f, %.5f", photo.latitude, photo.longitude)}$alt")
        }
        if (photo.address.isNotBlank()) {
            items.add("Dirección / Ubicación:" to photo.address)
        }
        if (photo.category.isNotBlank()) {
            items.add("Categoría / Etiqueta:" to photo.category)
        }
        if (photo.project.isNotBlank()) {
            items.add("Referencia:" to photo.project)
        }
        if (photo.author.isNotBlank()) {
            items.add("Registrado por:" to photo.author)
        }
        if (config.includeNotes && photo.notes.isNotBlank()) {
            items.add("Observaciones / Notas:" to photo.notes)
        }

        val labelColWidth = 130f
        for ((label, value) in items) {
            if (y > bottom - 15f) break
            canvas.drawText(label, left + 14f, y, labelPaint)
            val availableW = right - (left + 14f + labelColWidth) - 10f
            val truncatedVal = truncate(value, valPaint, availableW)
            canvas.drawText(truncatedVal, left + 14f + labelColWidth, y, valPaint)
            y += lineSpacing
        }
    }

    private fun drawCompactMetadata(
        canvas: Canvas,
        photo: StampedPhoto,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 9.5f
        }

        val title = photo.title.ifBlank { photo.category }.ifBlank { "Evidencia" }
        canvas.drawText(title, left, top + 18f, titlePaint)

        var y = top + 36f
        val lineSpacing = 16f
        val maxWidth = right - left

        fun drawLine(label: String, value: String) {
            if (value.isBlank() || y > bottom - 10f) return
            val labelStr = "$label: "
            val lWidth = labelPaint.measureText(labelStr)
            canvas.drawText(labelStr, left, y, labelPaint)
            val trunc = truncate(value, valPaint, maxWidth - lWidth)
            canvas.drawText(trunc, left + lWidth, y, valPaint)
            y += lineSpacing
        }

        drawLine("Fecha", photo.formattedDateTime)
        if (photo.latitude != null && photo.longitude != null) {
            drawLine("GPS", String.format(Locale.US, "%.5f, %.5f", photo.latitude, photo.longitude))
        }
        if (photo.address.isNotBlank()) drawLine("Lugar", photo.address)
        if (photo.project.isNotBlank()) drawLine("Proyecto", photo.project)
        if (photo.notes.isNotBlank()) drawLine("Nota", photo.notes)
    }

    private fun truncate(text: String, paint: Paint, maxWidth: Float): String {
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
