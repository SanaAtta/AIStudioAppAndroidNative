package com.aiartgenerator.imagegenerator.videogenerator.model

data class CameraMotion(
    val id: String,
    val label: String,
)

data class VideoDuration(
    val id: String,
    val label: String,
    val seconds: Int,
)

object VideoCatalog {
    val durations = listOf(
        VideoDuration("3", "3s", 3),
        VideoDuration("5", "5s", 5),
        VideoDuration("10", "10s", 10),
    )

    val cameraMotions = listOf(
        CameraMotion("zoom_in", "Zoom In"),
        CameraMotion("pan_left", "Pan Left"),
        CameraMotion("pan_right", "Pan Right"),
        CameraMotion("orbit", "Orbit"),
        CameraMotion("static", "Static"),
        CameraMotion("tilt_up", "Tilt Up"),
    )

    fun durationSeconds(id: String): Int =
        durations.find { it.id == id }?.seconds ?: 5

    fun cameraMotionHint(id: String): String = when (id) {
        "zoom_in" -> "smooth zoom in camera motion"
        "pan_left" -> "pan left camera motion"
        "pan_right" -> "pan right camera motion"
        "orbit" -> "orbiting camera motion"
        "static" -> "static camera"
        "tilt_up" -> "tilt up camera motion"
        else -> ""
    }

    /** Maps UI aspect ids to deAPI aspect ratio values. */
    fun videoAspectRatio(aspectId: String): String = when (aspectId) {
        "9:16" -> "9:16"
        "1:1" -> "1:1"
        "4:3" -> "4:3"
        else -> "16:9"
    }
}