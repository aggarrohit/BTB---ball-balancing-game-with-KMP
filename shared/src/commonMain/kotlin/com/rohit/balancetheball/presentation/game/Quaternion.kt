package com.rohit.balancetheball.presentation.game

import kotlin.math.sqrt

/**
 * Minimal quaternion for composing the ball's roll frame-to-frame. Rolling direction can change
 * every physics tick (the player is free to tilt any way at any time), so the rotation has to be
 * built up by *composing* each tick's small rotation into a running total — accumulating two
 * independent Euler angles (rotationX/rotationY tracked separately) doesn't compose correctly
 * once both axes are active at once, which is what caused the roll to look wrong part of the time.
 */
data class Quaternion(val w: Float, val x: Float, val y: Float, val z: Float) {
    companion object {
        val IDENTITY = Quaternion(1f, 0f, 0f, 0f)
    }
}

/** Hamilton product — composes [other] as having been applied first, then this. */
operator fun Quaternion.times(other: Quaternion): Quaternion = Quaternion(
    w = w * other.w - x * other.x - y * other.y - z * other.z,
    x = w * other.x + x * other.w + y * other.z - z * other.y,
    y = w * other.y - x * other.z + y * other.w + z * other.x,
    z = w * other.z + x * other.y - y * other.x + z * other.w
)

fun Quaternion.normalized(): Quaternion {
    val length = sqrt(w * w + x * x + y * y + z * z)
    return if (length > 0f) Quaternion(w / length, x / length, y / length, z / length) else Quaternion.IDENTITY
}

/** Rotates the 3D point (px, py, pz) by this (unit) quaternion. */
fun Quaternion.rotate(px: Float, py: Float, pz: Float): Triple<Float, Float, Float> {
    val tx = 2f * (y * pz - z * py)
    val ty = 2f * (z * px - x * pz)
    val tz = 2f * (x * py - y * px)
    return Triple(
        px + w * tx + (y * tz - z * ty),
        py + w * ty + (z * tx - x * tz),
        pz + w * tz + (x * ty - y * tx)
    )
}
