package com.widgetsholat.app.presentation.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.widgetsholat.app.domain.model.AnalogPrayerMarker
import com.widgetsholat.app.domain.model.AnalogWidgetDialState
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders an [AnalogWidgetDialState] onto a bounded [Bitmap] for Jetpack Glance.
 * Includes a 12-hour analog clock face with all 12 numbers (1..12), active prayer arc,
 * hour/minute hands, and all 6 prayer times attached around the dial.
 */
object AnalogPrayerClockBitmapRenderer {

    private const val MIN_DIM_PX = 280
    private const val MAX_DIM_PX = 860

    fun render(state: AnalogWidgetDialState, widthPx: Int, heightPx: Int): Bitmap {
        val w = widthPx.coerceIn(MIN_DIM_PX, MAX_DIM_PX)
        val h = heightPx.coerceIn(MIN_DIM_PX, MAX_DIM_PX)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val minDim = min(w, h).toFloat()
        val scale = minDim / 360f

        drawBackground(canvas, w.toFloat(), h.toFloat(), scale)

        val topHeaderSpace = 22f * scale
        val bottomFooterSpace = 26f * scale
        val availableHeight = h - topHeaderSpace - bottomFooterSpace
        val centerX = w / 2f
        val centerY = topHeaderSpace + availableHeight / 2f
        val dialRadius = (min(w.toFloat(), availableHeight) * 0.30f).coerceAtLeast(48f * scale)

        drawHeaderAndFooter(canvas, state, w.toFloat(), h.toFloat(), scale)
        drawDialAndArc(canvas, state, centerX, centerY, dialRadius, scale)
        drawClockNumbersAndTicks(canvas, centerX, centerY, dialRadius, scale)
        drawAttachedPrayerMarkers(canvas, state.markers, centerX, centerY, dialRadius, scale)
        drawClockHands(canvas, state, centerX, centerY, dialRadius, scale)

        return bitmap
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float, scale: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121417")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A2F36")
            style = Paint.Style.STROKE
            strokeWidth = 2f * scale
        }
        val corner = 20f * scale
        val rect = RectF(1f * scale, 1f * scale, w - 1f * scale, h - 1f * scale)
        canvas.drawRoundRect(rect, corner, corner, bgPaint)
        canvas.drawRoundRect(rect, corner, corner, borderPaint)
    }

    private fun drawHeaderAndFooter(
        canvas: Canvas,
        state: AnalogWidgetDialState,
        w: Float,
        h: Float,
        scale: Float
    ) {
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9AA0A6")
            textSize = 10.5f * scale
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(state.locationLabel, w / 2f, 16f * scale, headerPaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                state.activeArc?.isSyuruq == true -> Color.parseColor("#FF5252")
                state.activeArc?.isWarning == true -> Color.parseColor("#FFAB40")
                else -> Color.parseColor("#69F0AE")
            }
            textSize = 11f * scale
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(state.statusLabel, w / 2f, h - 10f * scale, footerPaint)
    }

    private fun drawDialAndArc(
        canvas: Canvas,
        state: AnalogWidgetDialState,
        cx: Float,
        cy: Float,
        radius: Float,
        scale: Float
    ) {
        val dialFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#181B20")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, dialFillPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#383E47")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * scale
        }
        canvas.drawCircle(cx, cy, radius, ringPaint)

        state.activeArc?.let { arc ->
            val arcColor = when {
                arc.isSyuruq -> Color.parseColor("#FF5252")
                arc.isWarning -> Color.parseColor("#FFAB40")
                else -> Color.parseColor("#69F0AE")
            }
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = arcColor
                style = Paint.Style.STROKE
                strokeWidth = 5f * scale
                strokeCap = Paint.Cap.ROUND
            }
            val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(arcRect, arc.startAngle, arc.sweepAngle, false, arcPaint)
        }

        state.syuruqArc?.let { syuruq ->
            val syuruqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF5252")
                style = Paint.Style.STROKE
                strokeWidth = 6f * scale
                strokeCap = Paint.Cap.ROUND
            }
            val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(arcRect, syuruq.startAngle, syuruq.sweepAngle, false, syuruqPaint)
        }
    }

    private fun drawClockNumbersAndTicks(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        scale: Float
    ) {
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#757B84")
            strokeWidth = 1.5f * scale
            strokeCap = Paint.Cap.ROUND
        }
        val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8EAED")
            textSize = 11.5f * scale
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val baselineOffset = (numberPaint.descent() + numberPaint.ascent()) / 2f

        for (hour in 1..12) {
            val angleRad = Math.toRadians((hour * 30 - 90).toDouble())
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()

            val tickStart = radius - 6f * scale
            val tickEnd = radius - 2f * scale
            canvas.drawLine(
                cx + tickStart * cosA,
                cy + tickStart * sinA,
                cx + tickEnd * cosA,
                cy + tickEnd * sinA,
                tickPaint
            )

            val numRadius = radius - 16f * scale
            canvas.drawText(
                hour.toString(),
                cx + numRadius * cosA,
                cy + numRadius * sinA - baselineOffset,
                numberPaint
            )
        }
    }

    private fun drawAttachedPrayerMarkers(
        canvas: Canvas,
        markers: List<AnalogPrayerMarker>,
        cx: Float,
        cy: Float,
        radius: Float,
        scale: Float
    ) {
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val leaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * scale
        }
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.1f * scale
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f * scale
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val labelOrbitRadius = radius + 28f * scale
        val pillHeight = 15f * scale
        val pillPadH = 5f * scale

        for (marker in markers) {
            val accentColor = when {
                marker.isSyuruq -> Color.parseColor("#FF5252")
                marker.isActive -> Color.parseColor("#69F0AE")
                else -> Color.parseColor("#8AB4F8")
            }

            val anchorRad = Math.toRadians(marker.dialAngleDegrees.toDouble())
            val anchorX = cx + radius * cos(anchorRad).toFloat()
            val anchorY = cy + radius * sin(anchorRad).toFloat()

            val labelRad = Math.toRadians(marker.labelAngleDegrees.toDouble())
            val labelX = cx + labelOrbitRadius * cos(labelRad).toFloat()
            val labelY = cy + labelOrbitRadius * sin(labelRad).toFloat()

            // Leader line from dial rim to attached pill
            leaderPaint.color = accentColor
            leaderPaint.alpha = 170
            canvas.drawLine(anchorX, anchorY, labelX, labelY, leaderPaint)

            // Anchor dot on the dial rim
            dotPaint.color = accentColor
            canvas.drawCircle(anchorX, anchorY, 3.2f * scale, dotPaint)

            // Compact pill with prayer name + time
            val text = "${marker.prayerName.displayName} ${marker.timeText}"
            val textWidth = labelPaint.measureText(text)
            val pillHalfW = (textWidth / 2f) + pillPadH
            val pillHalfH = pillHeight / 2f
            val pillRect = RectF(
                labelX - pillHalfW,
                labelY - pillHalfH,
                labelX + pillHalfW,
                labelY + pillHalfH
            )

            pillBgPaint.color = if (marker.isActive) {
                Color.parseColor("#1E3A2F")
            } else {
                Color.parseColor("#20242A")
            }
            pillBorderPaint.color = accentColor
            canvas.drawRoundRect(pillRect, 6f * scale, 6f * scale, pillBgPaint)
            canvas.drawRoundRect(pillRect, 6f * scale, 6f * scale, pillBorderPaint)

            labelPaint.color = if (marker.isActive || marker.isSyuruq) {
                accentColor
            } else {
                Color.parseColor("#E8EAED")
            }
            val textBaseline = labelY - (labelPaint.descent() + labelPaint.ascent()) / 2f
            canvas.drawText(text, labelX, textBaseline, labelPaint)
        }
    }

    private fun drawClockHands(
        canvas: Canvas,
        state: AnalogWidgetDialState,
        cx: Float,
        cy: Float,
        radius: Float,
        scale: Float
    ) {
        val hourRad = Math.toRadians(state.hourAngle.toDouble())
        val hourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 3.5f * scale
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            cx,
            cy,
            cx + (radius * 0.45f) * cos(hourRad).toFloat(),
            cy + (radius * 0.45f) * sin(hourRad).toFloat(),
            hourPaint
        )

        val minRad = Math.toRadians(state.minuteAngle.toDouble())
        val minPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CFD8DC")
            strokeWidth = 2.2f * scale
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            cx,
            cy,
            cx + (radius * 0.65f) * cos(minRad).toFloat(),
            cy + (radius * 0.65f) * sin(minRad).toFloat(),
            minPaint
        )

        val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, 3.5f * scale, pivotPaint)
    }
}
