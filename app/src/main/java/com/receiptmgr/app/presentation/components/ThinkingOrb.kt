package com.receiptmgr.app.presentation.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private fun hashD(a: Double, b: Double): Double {
    val h = sin(a * 12.9898 + b * 78.233) * 43758.5453
    return h - floor(h)
}

private class OrbMove(
    val axis: Int,
    val lo: Double,
    val hi: Double,
    val ang: Double
)

private class OrbSc(
    val amount: DoubleArray,
    val active: Int
)

private fun solveCycle(time: Double, count: Int, slotDur: Double, rest: Double): OrbSc {
    val cyc = 2 * count * slotDur + rest
    val tc = time % cyc
    val amount = DoubleArray(count) { 0.0 }
    var active = -1
    if (tc < 2 * count * slotDur) {
        val slot = floor(tc / slotDur).toInt()
        val p = (tc - slot * slotDur) / slotDur
        val cl = min(1.0, p / 0.7)
        val ep = 1.0 - (1.0 - cl).pow(3.0)
        if (slot < count) {
            for (i in 0 until slot) {
                amount[i] = 1.0
            }
            amount[slot] = ep
            active = slot
        } else {
            val u = 2 * count - 1 - slot
            for (i in 0 until u) {
                amount[i] = 1.0
            }
            amount[u] = 1.0 - ep
            active = u
        }
    }
    return OrbSc(amount, active)
}

private fun makeMoves(count: Int): List<OrbMove> {
    val moves = ArrayList<OrbMove>(count)
    for (i in 0 until count) {
        val axis = min(2, floor(hashD(i.toDouble(), 2.3) * 3).toInt())
        val lo = -1.0 + 0.5 * min(3, floor(hashD(i.toDouble(), 5.9) * 4).toInt())
        val dir = if (hashD(i.toDouble(), 7.7) < 0.5) 1.0 else -1.0
        moves.add(OrbMove(axis, lo, lo + 0.5, dir * PI / 2.0))
    }
    return moves
}

private class Pt3Res(
    val x: Double,
    val y: Double,
    val z: Double,
    val inActive: Boolean
)

private fun applyMoves(
    px: Double,
    py: Double,
    pz: Double,
    moves: List<OrbMove>,
    sc: OrbSc
): Pt3Res {
    var x = px
    var y = py
    var z = pz
    var inActive = false
    for (i in moves.indices) {
        if (sc.amount[i] <= 0.0) continue
        val mv = moves[i]
        val coord = if (mv.axis == 0) x else (if (mv.axis == 1) y else z)
        if (coord < mv.lo || coord >= mv.hi) continue
        if (i == sc.active) inActive = true
        val a = mv.ang * sc.amount[i]
        val ca = cos(a)
        val sa = sin(a)
        when (mv.axis) {
            0 -> {
                val y2 = y * ca - z * sa
                z = y * sa + z * ca
                y = y2
            }
            1 -> {
                val x2 = x * ca + z * sa
                z = -x * sa + z * ca
                x = x2
            }
            else -> {
                val x2 = x * ca - y * sa
                y = x * sa + y * ca
                x = x2
            }
        }
    }
    return Pt3Res(x, y, z, inActive)
}

private class DotPoint(
    val x: Float,
    val y: Float,
    val z: Double,
    val r: Float,
    val white: Double
)

@Composable
fun ThinkingOrb(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isDark: Boolean = false
) {
    val moves = remember { makeMoves(14) }
    val latRings = 9
    val lonDensity = 24

    val time by produceState(initialValue = 0.0) {
        var startMillis: Long? = null
        while (true) {
            withInfiniteAnimationFrameMillis { frameMillis ->
                if (startMillis == null) startMillis = frameMillis
                val elapsedSeconds = (frameMillis - startMillis!!) / 1000.0
                value = elapsedSeconds * 1.82
            }
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val radius = (this.size.width / 2f) * 0.82f

        val yaw = time * 0.55
        val tilt = 0.35 + 0.1 * sin(time * 0.9)

        val st = sin(tilt)
        val ct = cos(tilt)
        val sy = sin(yaw)
        val cyw = cos(yaw)

        fun project(x: Double, y: Double, z: Double): DoubleArray {
            val x1 = x * cyw + z * sy
            val z1 = -x * sy + z * cyw
            val y1 = y * ct - z1 * st
            val z2 = y * st + z1 * ct
            return doubleArrayOf(cx + x1 * radius, cy - y1 * radius, z2)
        }

        val rs = (this.size.width / 300.0).pow(0.6)
        val sc = solveCycle(time, 14, 0.42, 1.2)

        val rBase = 0.6 * 1.05
        val rDepth = 1.7 * 1.05
        val rActive = 0.3 * 1.05

        val inkFar = 0.62
        val inkSpan = 0.54

        val dots = ArrayList<DotPoint>()

        for (li in 0..latRings) {
            val lat = -PI / 2.0 + (li.toDouble() / latRings) * PI
            val cosLat = cos(lat)
            val sinLat = sin(lat)
            val lonCount = max(1, (kotlin.math.abs(cosLat) * lonDensity).toInt())
            for (lj in 0 until lonCount) {
                val lon = (lj.toDouble() / lonCount) * 2.0 * PI
                val res = applyMoves(
                    cosLat * cos(lon),
                    sinLat,
                    cosLat * sin(lon),
                    moves,
                    sc
                )
                val proj = project(res.x, res.y, res.z)
                val px = proj[0].toFloat()
                val py = proj[1].toFloat()
                val zr = proj[2]
                val depth = (zr + 1.0) / 2.0

                val r = ((rBase + rDepth * depth + (if (res.inActive) rActive else 0.0)) * rs).toFloat()
                val white = inkFar - inkSpan * depth - (if (res.inActive) 0.14 else 0.0)
                dots.add(DotPoint(x = px, y = py, z = zr, r = r, white = white))
            }
        }

        dots.sortBy { it.z }

        val rMin = 0.3f
        for (d in dots) {
            val w = min(1.0, max(0.0, d.white))
            val g = ((if (isDark) 1.0 - w else w) * 255.0).toInt().coerceIn(0, 255)
            val pointColor = Color(g, g, g)
            drawCircle(
                color = pointColor,
                radius = max(rMin, d.r),
                center = Offset(d.x, d.y)
            )
        }
    }
}
