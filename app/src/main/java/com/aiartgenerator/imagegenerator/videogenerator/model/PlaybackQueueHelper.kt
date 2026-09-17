package com.aiartgenerator.imagegenerator.videogenerator.model

import kotlin.random.Random

object PlaybackQueueHelper {
    const val SKIP_MS = 10_000
    const val RESTART_THRESHOLD_MS = 3_000

    fun musicQueue(): List<GeneratedMusic> =
        GeneratedMusicsRepository.items.sortedByDescending { it.createdAt }

    fun coverQueue(): List<GeneratedCover> =
        GeneratedCoversRepository.items.sortedByDescending { it.createdAt }

    fun videoQueue(): List<GeneratedVideo> =
        GeneratedVideosRepository.items.sortedByDescending { it.createdAt }

    fun nextMusic(currentId: String?, shuffle: Boolean): GeneratedMusic? {
        val queue = musicQueue()
        if (queue.isEmpty()) return null
        if (currentId == null) return queue.first()
        if (shuffle) {
            val others = queue.filter { it.id != currentId }
            return if (others.isNotEmpty()) others[Random.nextInt(others.size)] else queue.first()
        }
        val index = queue.indexOfFirst { it.id == currentId }
        if (index < 0 || index >= queue.lastIndex) return null
        return queue[index + 1]
    }

    fun previousMusic(currentId: String?): GeneratedMusic? {
        val queue = musicQueue()
        if (queue.isEmpty() || currentId == null) return null
        val index = queue.indexOfFirst { it.id == currentId }
        if (index <= 0) return null
        return queue[index - 1]
    }

    fun nextCover(currentId: String?, shuffle: Boolean): GeneratedCover? {
        val queue = coverQueue()
        if (queue.isEmpty()) return null
        if (currentId == null) return queue.first()
        if (shuffle) {
            val others = queue.filter { it.id != currentId }
            return if (others.isNotEmpty()) others[Random.nextInt(others.size)] else queue.first()
        }
        val index = queue.indexOfFirst { it.id == currentId }
        if (index < 0 || index >= queue.lastIndex) return null
        return queue[index + 1]
    }

    fun previousCover(currentId: String?): GeneratedCover? {
        val queue = coverQueue()
        if (queue.isEmpty() || currentId == null) return null
        val index = queue.indexOfFirst { it.id == currentId }
        if (index <= 0) return null
        return queue[index - 1]
    }

    fun nextVideo(currentId: String?, shuffle: Boolean): GeneratedVideo? {
        val queue = videoQueue()
        if (queue.isEmpty()) return null
        if (currentId == null) return queue.first()
        if (shuffle) {
            val others = queue.filter { it.id != currentId }
            return if (others.isNotEmpty()) others[Random.nextInt(others.size)] else queue.first()
        }
        val index = queue.indexOfFirst { it.id == currentId }
        if (index < 0 || index >= queue.lastIndex) return null
        return queue[index + 1]
    }

    fun previousVideo(currentId: String?): GeneratedVideo? {
        val queue = videoQueue()
        if (queue.isEmpty() || currentId == null) return null
        val index = queue.indexOfFirst { it.id == currentId }
        if (index <= 0) return null
        return queue[index - 1]
    }
}
