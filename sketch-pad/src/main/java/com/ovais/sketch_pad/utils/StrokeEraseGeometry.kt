package com.ovais.sketch_pad.utils

import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.SketchPoint
import kotlin.math.hypot
import kotlin.math.sqrt

private const val Eps = 1e-4f

internal fun strokeTouchesDisc(
    stroke: ActiveStroke,
    cx: Float,
    cy: Float,
    radius: Float
): Boolean {
    val pts = stroke.points
    if (pts.isEmpty()) return false
    if (pts.size == 1) {
        return hypot(
            (cx - pts.first().x).toDouble(),
            (cy - pts.first().y).toDouble()
        ).toFloat() < radius
    }
    for (j in 0 until pts.size - 1) {
        val d = distanceToSegment(
            cx, cy,
            pts[j].x, pts[j].y,
            pts[j + 1].x, pts[j + 1].y
        )
        if (d < radius) return true
    }
    return false
}

/**
 * Returns new strokes after removing portions of [stroke] that fall inside the closed disc
 * (center [cx],[cy], radius [r]). Polylines with fewer than two points are dropped.
 */
internal fun clipStrokeOutsideDisc(
    stroke: ActiveStroke,
    cx: Float,
    cy: Float,
    r: Float
): List<ActiveStroke> {
    val polylines = clipPolylineOutsideDisc(stroke.points, cx, cy, r)
    if (polylines.isEmpty()) return emptyList()
    return polylines.map { pts ->
        ActiveStroke(
            points = pts,
            color = stroke.color,
            strokeWidth = stroke.strokeWidth
        )
    }
}

private fun clipPolylineOutsideDisc(
    points: List<SketchPoint>,
    cx: Float,
    cy: Float,
    r: Float
): List<List<SketchPoint>> {
    if (points.isEmpty()) return emptyList()
    val r2 = r * r
    fun inside(p: SketchPoint): Boolean {
        val dx = p.x - cx
        val dy = p.y - cy
        return dx * dx + dy * dy < r2
    }

    if (points.size == 1) {
        val p = points.first()
        return if (inside(p)) emptyList() else listOf(listOf(p))
    }

    val expanded = mutableListOf<SketchPoint>()
    fun addPt(p: SketchPoint) {
        val last = expanded.lastOrNull()
        if (last == null || hypot((p.x - last.x).toDouble(), (p.y - last.y).toDouble()) > Eps.toDouble()) {
            expanded.add(p)
        }
    }

    addPt(points.first())
    for (i in 0 until points.size - 1) {
        val a = points[i]
        val b = points[i + 1]
        val ts = segmentCircleIntersectionTs(a.x, a.y, b.x, b.y, cx, cy, r)
            .filter { it > Eps && it < 1f - Eps }
            .sorted()
        for (t in ts) {
            addPt(lerp(a, b, t))
        }
        addPt(b)
    }

    if (expanded.size < 2) return emptyList()

    val keptEdges = BooleanArray(expanded.size - 1)
    for (j in 0 until expanded.lastIndex) {
        val p0 = expanded[j]
        val p1 = expanded[j + 1]
        val mx = (p0.x + p1.x) * 0.5f
        val my = (p0.y + p1.y) * 0.5f
        val d2 = (mx - cx) * (mx - cx) + (my - cy) * (my - cy)
        keptEdges[j] = d2 >= r2
    }

    val result = mutableListOf<MutableList<SketchPoint>>()
    var piece: MutableList<SketchPoint>? = null

    for (j in keptEdges.indices) {
        if (!keptEdges[j]) {
            piece = null
            continue
        }
        val p0 = expanded[j]
        val p1 = expanded[j + 1]
        if (piece == null) {
            piece = mutableListOf(p0, p1)
            result.add(piece)
        } else {
            val last = piece.last()
            if (kotlin.math.abs(last.x - p0.x) < Eps && kotlin.math.abs(last.y - p0.y) < Eps) {
                if (kotlin.math.abs(last.x - p1.x) > Eps || kotlin.math.abs(last.y - p1.y) > Eps) {
                    piece.add(p1)
                }
            } else {
                piece = mutableListOf(p0, p1)
                result.add(piece)
            }
        }
    }

    return result.map { it.toList() }.filter { it.size >= 2 }
}

private fun lerp(a: SketchPoint, b: SketchPoint, t: Float): SketchPoint {
    return SketchPoint(
        x = a.x + (b.x - a.x) * t,
        y = a.y + (b.y - a.y) * t
    )
}

/**
 * Intersection parameters t in [0, 1] along segment A + t(B - A) with circle at (cx, cy), radius r.
 */
private fun segmentCircleIntersectionTs(
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float,
    cx: Float,
    cy: Float,
    r: Float
): FloatArray {
    val dx = bx - ax
    val dy = by - ay
    val fx = ax - cx
    val fy = ay - cy
    val aCoef = dx * dx + dy * dy
    if (aCoef < Eps * Eps) return floatArrayOf()
    val bCoef = 2f * (fx * dx + fy * dy)
    val cCoef = fx * fx + fy * fy - r * r
    val disc = bCoef * bCoef - 4f * aCoef * cCoef
    if (disc < 0f) return floatArrayOf()
    val sd = sqrt(disc)
    var t0 = (-bCoef - sd) / (2f * aCoef)
    var t1 = (-bCoef + sd) / (2f * aCoef)
    if (t0 > t1) {
        val tmp = t0
        t0 = t1
        t1 = tmp
    }
    val out = FloatArray(2)
    var n = 0
    fun add(t: Float) {
        if (t < -Eps || t > 1f + Eps) return
        val tc = t.coerceIn(0f, 1f)
        if (n == 0 || kotlin.math.abs(tc - out[n - 1]) > Eps) {
            out[n++] = tc
        }
    }
    add(t0)
    add(t1)
    return out.copyOf(n)
}
