package com.codekage.showup.v2.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object PdfExporter {

    private const val PAGE_W = 595f   // A4 portrait points
    private const val PAGE_H = 842f
    private const val MARGIN = 36f

    private object Palette {
        val ink = Color.parseColor("#1A1A1A")
        val body = Color.parseColor("#4A4A4A")
        val muted = Color.parseColor("#8A8A8A")
        val hairline = Color.parseColor("#E5E5E0")
        val cardFill = Color.parseColor("#F7F7F3")
        val accent = Color.parseColor("#3F8C3D")
        val office = Color.parseColor("#3F8C3D")
        val remote = Color.parseColor("#4DA3F0")
        val sick = Color.parseColor("#FF6E6C")
        val leave = Color.parseColor("#FFB343")
        val bankHoliday = Color.parseColor("#9D7BFF")
        val absent = Color.parseColor("#888A87")
        val progressTrack = Color.parseColor("#ECEAE3")
        val brandStrip = Color.parseColor("#1F4A1E")
    }

    private val sansRegular = Typeface.create("sans-serif", Typeface.NORMAL)
    private val sansMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val sansLight = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val sansBold = Typeface.create("sans-serif", Typeface.BOLD)

    fun generateMonthlyReport(context: Context, data: PdfReportData): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawHeader(canvas, data)
        var y = 168f
        y = drawSubject(canvas, data, y) + 24f
        y = drawKpiStrip(canvas, data, y) + 28f
        y = drawCategoryGrid(canvas, data, y) + 28f
        y = drawProgress(canvas, data, y) + 28f
        drawWeeklyTable(canvas, data, y)
        drawFooter(canvas)

        document.finishPage(page)

        val outDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = File(outDir, "showup_${timestamp}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    // ────────────────────────────────────────────────────────────────────
    // Header band: brand strip + title + subtitle
    private fun drawHeader(canvas: Canvas, data: PdfReportData) {
        val band = Paint().apply { color = Palette.brandStrip; isAntiAlias = true }
        canvas.drawRect(0f, 0f, PAGE_W, 6f, band)

        val brand = textPaint(sansBold, 11f, Palette.accent).apply {
            letterSpacing = 0.16f
        }
        canvas.drawText("SHOWUP · ATTENDANCE", MARGIN, 36f, brand)

        val title = textPaint(sansLight, 28f, Palette.ink)
        canvas.drawText("Monthly attendance report", MARGIN, 78f, title)

        val divider = Paint().apply { color = Palette.hairline; strokeWidth = 0.6f }
        canvas.drawLine(MARGIN, 100f, PAGE_W - MARGIN, 100f, divider)

        val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm"))
        val genLabel = textPaint(sansRegular, 9f, Palette.muted).apply {
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Generated $date", PAGE_W - MARGIN, 36f, genLabel)
    }

    // ────────────────────────────────────────────────────────────────────
    // Subject: job name + period
    private fun drawSubject(canvas: Canvas, data: PdfReportData, top: Float): Float {
        val labelPaint = textPaint(sansRegular, 9f, Palette.muted).apply {
            letterSpacing = 0.12f
        }
        canvas.drawText("EMPLOYER", MARGIN, top, labelPaint)
        canvas.drawText("PERIOD", PAGE_W / 2f, top, labelPaint)

        val valuePaint = textPaint(sansMedium, 16f, Palette.ink)
        canvas.drawText(data.jobName, MARGIN, top + 20f, valuePaint)
        canvas.drawText(data.period, PAGE_W / 2f, top + 20f, valuePaint)

        return top + 28f
    }

    // ────────────────────────────────────────────────────────────────────
    // KPI strip: 4 hero metrics
    private fun drawKpiStrip(canvas: Canvas, data: PdfReportData, top: Float): Float {
        val total = (PAGE_W - MARGIN * 2)
        val gap = 10f
        val cardW = (total - gap * 3) / 4f
        val cardH = 76f

        val verdict = if (data.officePercentage >= data.goalPercentage) "On track" else "Below goal"
        val verdictColor = if (data.officePercentage >= data.goalPercentage)
            Palette.office else Palette.sick

        val cards = listOf(
            KpiCard("OFFICE %", "%.1f".format(data.officePercentage) + "%", Palette.office),
            KpiCard("GOAL", "${data.goalPercentage}%", Palette.body),
            KpiCard("WORKING DAYS", data.totalWorkingDays.toString(), Palette.body),
            KpiCard("STATUS", verdict, verdictColor),
        )

        cards.forEachIndexed { i, card ->
            val x = MARGIN + (cardW + gap) * i
            drawKpiCard(canvas, RectF(x, top, x + cardW, top + cardH), card)
        }
        return top + cardH
    }

    private data class KpiCard(val label: String, val value: String, val valueColor: Int)

    private fun drawKpiCard(canvas: Canvas, r: RectF, card: KpiCard) {
        val fill = Paint().apply { color = Palette.cardFill; isAntiAlias = true }
        canvas.drawRoundRect(r, 8f, 8f, fill)

        val stroke = Paint().apply {
            color = Palette.hairline; isAntiAlias = true
            style = Paint.Style.STROKE; strokeWidth = 0.6f
        }
        canvas.drawRoundRect(r, 8f, 8f, stroke)

        val labelPaint = textPaint(sansMedium, 8.5f, Palette.muted).apply {
            letterSpacing = 0.14f
        }
        canvas.drawText(card.label, r.left + 12f, r.top + 18f, labelPaint)

        val valuePaint = textPaint(sansMedium, 22f, card.valueColor)
        canvas.drawText(card.value, r.left + 12f, r.top + 50f, valuePaint)
    }

    // ────────────────────────────────────────────────────────────────────
    // Category grid: 2 rows × 3 columns of category tiles
    private fun drawCategoryGrid(canvas: Canvas, data: PdfReportData, top: Float): Float {
        val sectionLabel = textPaint(sansMedium, 10f, Palette.muted).apply {
            letterSpacing = 0.14f
        }
        canvas.drawText("BREAKDOWN", MARGIN, top, sectionLabel)
        val gridTop = top + 12f

        val total = (PAGE_W - MARGIN * 2)
        val gap = 10f
        val tileW = (total - gap * 2) / 3f
        val tileH = 64f

        data class Tile(val label: String, val value: Int, val color: Int)
        val tiles = listOf(
            Tile("Office", data.officeDays, Palette.office),
            Tile("Remote", data.remoteDays, Palette.remote),
            Tile("Sick", data.sickDays, Palette.sick),
            Tile("Leave", data.leaveDays, Palette.leave),
            Tile("Bank holiday", data.bankHolidayDays, Palette.bankHoliday),
            Tile("Absent", data.absentDays, Palette.absent),
        )

        tiles.forEachIndexed { i, t ->
            val col = i % 3; val row = i / 3
            val x = MARGIN + (tileW + gap) * col
            val y = gridTop + (tileH + gap) * row
            drawCategoryTile(canvas, RectF(x, y, x + tileW, y + tileH), t.label, t.value, t.color)
        }
        return gridTop + tileH * 2 + gap
    }

    private fun drawCategoryTile(canvas: Canvas, r: RectF, label: String, value: Int, tint: Int) {
        val fill = Paint().apply { this.color = Palette.cardFill; isAntiAlias = true }
        canvas.drawRoundRect(r, 8f, 8f, fill)
        val stroke = Paint().apply {
            this.color = Palette.hairline; isAntiAlias = true
            style = Paint.Style.STROKE; strokeWidth = 0.6f
        }
        canvas.drawRoundRect(r, 8f, 8f, stroke)

        // accent dot
        val dot = Paint().apply { this.color = tint; isAntiAlias = true }
        canvas.drawCircle(r.left + 16f, r.top + 18f, 3.5f, dot)

        val labelPaint = textPaint(sansRegular, 10f, Palette.body)
        canvas.drawText(label, r.left + 26f, r.top + 22f, labelPaint)

        val valuePaint = textPaint(sansMedium, 22f, tint)
        canvas.drawText(value.toString(), r.left + 14f, r.top + 52f, valuePaint)

        val unitPaint = textPaint(sansRegular, 10f, Palette.muted)
        val valueWidth = valuePaint.measureText(value.toString())
        canvas.drawText("days", r.left + 18f + valueWidth, r.top + 52f, unitPaint)
    }

    // ────────────────────────────────────────────────────────────────────
    // Progress: office % vs goal, with progress track
    private fun drawProgress(canvas: Canvas, data: PdfReportData, top: Float): Float {
        val sectionLabel = textPaint(sansMedium, 10f, Palette.muted).apply {
            letterSpacing = 0.14f
        }
        canvas.drawText("PROGRESS TO GOAL", MARGIN, top, sectionLabel)

        val barTop = top + 18f
        val barH = 10f
        val barLeft = MARGIN
        val barRight = PAGE_W - MARGIN
        val track = RectF(barLeft, barTop, barRight, barTop + barH)

        val trackPaint = Paint().apply { color = Palette.progressTrack; isAntiAlias = true }
        canvas.drawRoundRect(track, barH / 2, barH / 2, trackPaint)

        val pct = (data.officePercentage / 100f).coerceIn(0f, 1f)
        val fillRight = barLeft + (barRight - barLeft) * pct
        if (fillRight > barLeft) {
            val fillRect = RectF(barLeft, barTop, fillRight, barTop + barH)
            val fillColor = when {
                data.officePercentage >= data.goalPercentage -> Palette.office
                data.officePercentage >= data.goalPercentage * 0.8f -> Palette.leave
                else -> Palette.sick
            }
            val fillPaint = Paint().apply { color = fillColor; isAntiAlias = true }
            canvas.drawRoundRect(fillRect, barH / 2, barH / 2, fillPaint)
        }

        // Goal marker
        val goalX = barLeft + (barRight - barLeft) * (data.goalPercentage / 100f).coerceIn(0f, 1f)
        val markerPaint = Paint().apply {
            color = Palette.ink; strokeWidth = 1.4f; isAntiAlias = true
        }
        canvas.drawLine(goalX, barTop - 4f, goalX, barTop + barH + 4f, markerPaint)
        val goalLabel = textPaint(sansRegular, 8.5f, Palette.muted).apply {
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Goal ${data.goalPercentage}%", goalX, barTop + barH + 16f, goalLabel)

        val officeLabel = textPaint(sansMedium, 10f, Palette.ink)
        canvas.drawText(
            "%.1f%% of working days in the office".format(data.officePercentage),
            MARGIN, barTop + barH + 16f, officeLabel,
        )

        return barTop + barH + 24f
    }

    // ────────────────────────────────────────────────────────────────────
    // Weekly breakdown table
    private fun drawWeeklyTable(canvas: Canvas, data: PdfReportData, top: Float) {
        val sectionLabel = textPaint(sansMedium, 10f, Palette.muted).apply {
            letterSpacing = 0.14f
        }
        canvas.drawText("WEEKLY BREAKDOWN", MARGIN, top, sectionLabel)

        if (data.weeklyStats.isEmpty()) {
            val empty = textPaint(sansRegular, 10f, Palette.muted)
            canvas.drawText("No weekly data for this period.", MARGIN, top + 20f, empty)
            return
        }

        val tableTop = top + 14f
        val colW = (PAGE_W - MARGIN * 2) / 7f
        val colXs = (0..6).map { MARGIN + colW * it }
        val rowH = 22f

        // Header row
        val headerFill = Paint().apply { color = Palette.cardFill; isAntiAlias = true }
        canvas.drawRoundRect(
            RectF(MARGIN, tableTop, PAGE_W - MARGIN, tableTop + rowH), 4f, 4f, headerFill,
        )

        val headerText = textPaint(sansMedium, 9.5f, Palette.body).apply {
            letterSpacing = 0.06f
        }
        val headers = listOf("Week", "Working", "Office", "Remote", "Sick", "Leave", "Holiday")
        headers.forEachIndexed { i, h ->
            val align = if (i == 0) Paint.Align.LEFT else Paint.Align.RIGHT
            headerText.textAlign = align
            val x = if (i == 0) colXs[i] + 12f else colXs[i] + colW - 12f
            canvas.drawText(h, x, tableTop + 14f, headerText)
        }

        // Data rows
        val rowText = textPaint(sansRegular, 10f, Palette.ink)
        val mutedRowText = textPaint(sansRegular, 10f, Palette.muted)
        var y = tableTop + rowH
        data.weeklyStats.forEachIndexed { idx, w ->
            // alternating row stripe
            if (idx % 2 == 1) {
                val stripe = Paint().apply { color = Palette.cardFill; alpha = 110; isAntiAlias = true }
                canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + rowH, stripe)
            }
            val cells = listOf(
                "Wk ${w.weekNumber}" to Palette.ink,
                w.totalWorkingDays.toString() to Palette.body,
                w.officeDays.toString() to if (w.officeDays > 0) Palette.office else Palette.muted,
                w.remoteDays.toString() to if (w.remoteDays > 0) Palette.remote else Palette.muted,
                w.sickDays.toString() to if (w.sickDays > 0) Palette.sick else Palette.muted,
                w.leaveDays.toString() to if (w.leaveDays > 0) Palette.leave else Palette.muted,
                w.bankHolidayDays.toString() to if (w.bankHolidayDays > 0) Palette.bankHoliday else Palette.muted,
            )
            cells.forEachIndexed { i, (txt, color) ->
                val align = if (i == 0) Paint.Align.LEFT else Paint.Align.RIGHT
                val paint = if (i == 0) {
                    rowText.apply { this.color = color; textAlign = align }
                } else if (color == Palette.muted) {
                    mutedRowText.apply { textAlign = align }
                } else {
                    rowText.apply { this.color = color; textAlign = align }
                }
                val x = if (i == 0) colXs[i] + 12f else colXs[i] + colW - 12f
                canvas.drawText(txt, x, y + 14f, paint)
            }
            y += rowH

            // hairline divider between rows
            val divider = Paint().apply { color = Palette.hairline; strokeWidth = 0.4f }
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, divider)
        }

        // Outer table border
        val outline = Paint().apply {
            color = Palette.hairline; isAntiAlias = true
            style = Paint.Style.STROKE; strokeWidth = 0.6f
        }
        canvas.drawRoundRect(RectF(MARGIN, tableTop, PAGE_W - MARGIN, y), 4f, 4f, outline)
    }

    // ────────────────────────────────────────────────────────────────────
    // Footer
    private fun drawFooter(canvas: Canvas) {
        val divider = Paint().apply { color = Palette.hairline; strokeWidth = 0.5f }
        canvas.drawLine(MARGIN, PAGE_H - 36f, PAGE_W - MARGIN, PAGE_H - 36f, divider)

        val left = textPaint(sansRegular, 8.5f, Palette.muted)
        canvas.drawText("ShowUp — your attendance, summarised.", MARGIN, PAGE_H - 22f, left)

        val right = textPaint(sansRegular, 8.5f, Palette.muted).apply {
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Page 1", PAGE_W - MARGIN, PAGE_H - 22f, right)
    }

    // ────────────────────────────────────────────────────────────────────
    private fun textPaint(typeface: Typeface, size: Float, color: Int): Paint = Paint().apply {
        this.typeface = typeface
        textSize = size
        this.color = color
        isAntiAlias = true
    }
}
