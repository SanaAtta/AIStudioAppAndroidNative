package com.example.myapplication.model

data class VideoResolution(
    val id: String,
    val label: String,
    val subtitle: String,
)

data class VideoDuration(
    val id: String,
    val label: String,
)

object VideoCatalog {
    val resolutions = listOf(
        VideoResolution("360", "360 P", "Fastest"),
        VideoResolution("540", "540 P", "SD"),
        VideoResolution("720", "720 P", "HD"),
        VideoResolution("1080", "1080 P", "Best Quality"),
    )

    val durations = listOf(
        VideoDuration("2", "2s"),
        VideoDuration("3", "3s"),
        VideoDuration("5", "5s"),
        VideoDuration("10", "10s"),
    )
}
