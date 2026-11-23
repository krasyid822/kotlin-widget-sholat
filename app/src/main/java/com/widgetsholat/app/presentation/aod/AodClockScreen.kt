package com.widgetsholat.app.presentation.aod

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widgetsholat.app.domain.model.AodClockState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AodClockScreen(
    state: AodClockState
) {
    val density = LocalDensity.current
    val digitPaint = remember(density) {
        Paint().apply {
            textSize = with(density) { 14.sp.toPx() }
            color = android.graphics.Color.parseColor("#E0E0E0")
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.offset(x = state.shiftXDp.dp, y = state.shiftYDp.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(260.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 16.dp.toPx()

                drawCircle(
                    color = Color(0xFF424242),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                state.arcState?.let { arc ->
                    val arcColor = when {
                        arc.isSyuruq -> Color(0xFFFF5252)
                        arc.isWarning -> Color(0xFFFFAB40)
                        else -> Color(0xFF69F0AE)
                    }
                    drawArc(
                        color = arcColor,
                        startAngle = arc.startAngle,
                        sweepAngle = arc.sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                state.syuruqArc?.let { syuruq ->
                    drawArc(
                        color = Color(0xFFFF5252),
                        startAngle = syuruq.startAngle,
                        sweepAngle = syuruq.sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                for (hour in 1..12) {
                    val angleRad = Math.toRadians((hour * 30 - 90).toDouble())
                    val cosA = cos(angleRad).toFloat()
                    val sinA = sin(angleRad).toFloat()

                    val tickStart = Offset(
                        x = center.x + (radius - 8.dp.toPx()) * cosA,
                        y = center.y + (radius - 8.dp.toPx()) * sinA
                    )
                    val tickEnd = Offset(
                        x = center.x + (radius - 3.dp.toPx()) * cosA,
                        y = center.y + (radius - 3.dp.toPx()) * sinA
                    )
                    drawLine(
                        color = Color(0xFF757575),
                        start = tickStart,
                        end = tickEnd,
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    val digitRadius = radius - 22.dp.toPx()
                    val digitX = center.x + digitRadius * cosA
                    val digitY = center.y + digitRadius * sinA -
                        (digitPaint.descent() + digitPaint.ascent()) / 2f
                    drawContext.canvas.nativeCanvas.drawText(
                        hour.toString(),
                        digitX,
                        digitY,
                        digitPaint
                    )
                }

                val hourRad = Math.toRadians(state.hourAngle.toDouble())
                val hourEnd = Offset(
                    x = center.x + (radius * 0.44f) * cos(hourRad).toFloat(),
                    y = center.y + (radius * 0.44f) * sin(hourRad).toFloat()
                )
                drawLine(
                    color = Color.White,
                    start = center,
                    end = hourEnd,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val minRad = Math.toRadians(state.minuteAngle.toDouble())
                val minEnd = Offset(
                    x = center.x + (radius * 0.62f) * cos(minRad).toFloat(),
                    y = center.y + (radius * 0.62f) * sin(minRad).toFloat()
                )
                drawLine(
                    color = Color(0xFFE0E0E0),
                    start = center,
                    end = minEnd,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = state.timeText,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light
            )
            val infoText = listOfNotNull(
                state.dayText.takeIf { it.isNotBlank() },
                state.batteryPercent?.let { "$it%" }
            ).joinToString(" • ")
            if (infoText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = infoText,
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp
                )
            }
            state.arcState?.let { arc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = arc.label,
                    color = Color(0xFFBDBDBD),
                    fontSize = 14.sp
                )
            }
        }
    }
}
