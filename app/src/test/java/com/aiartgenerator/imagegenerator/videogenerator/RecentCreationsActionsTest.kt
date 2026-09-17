package com.aiartgenerator.imagegenerator.videogenerator

import android.content.Context
import com.aiartgenerator.imagegenerator.videogenerator.model.CreationActions
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCreationsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideosRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryItem
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryMediaType
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.LibrarySource
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RecentCreationsActionsTest {

    private lateinit var tempDir: File
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("creations_test").toFile()
        mockContext = object : android.content.ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
            override fun getPackageName(): String = "com.aiartgenerator.imagegenerator.videogenerator"
            override fun getApplicationContext(): Context = this
        }
    }

    @Test
    fun testGeneratedVideoFavoriteToggleAndPersistence() {
        val dummyBytes = ByteArray(16) { 1 }
        val video = GeneratedVideosRepository.add(
            context = mockContext,
            bytes = dummyBytes,
            prompt = "Sunset on the beach",
            styleTitle = "Cinematic",
            durationSeconds = 5,
            cameraMotion = "pan_right",
            aspectLabel = "16:9",
        )

        assertFalse("New video should not be favorite initially", video.isFavorite)

        // Toggle favorite to true
        val isFav1 = GeneratedVideosRepository.toggleFavorite(mockContext, video.id)
        assertTrue("Video favorite state should toggle to true", isFav1)

        val updatedVideo1 = GeneratedVideosRepository.findById(video.id)
        assertTrue("Repository findById should show isFavorite = true", updatedVideo1?.isFavorite == true)

        // Toggle favorite to false
        val isFav2 = GeneratedVideosRepository.toggleFavorite(mockContext, video.id)
        assertFalse("Video favorite state should toggle to false", isFav2)

        val updatedVideo2 = GeneratedVideosRepository.findById(video.id)
        assertFalse("Repository findById should show isFavorite = false", updatedVideo2?.isFavorite == true)
    }

    @Test
    fun testCreationActionsToggleFavoriteForVideoLibraryItem() {
        val dummyBytes = ByteArray(16) { 2 }
        val video = GeneratedVideosRepository.add(
            context = mockContext,
            bytes = dummyBytes,
            prompt = "Rain in the forest",
            styleTitle = "Realistic",
            durationSeconds = 5,
            cameraMotion = "zoom_in",
            aspectLabel = "1:1",
        )

        val item = LibraryItem(
            id = video.id,
            source = LibrarySource.Video,
            type = LibraryMediaType.Video,
            createdAt = video.createdAt,
            title = video.title,
            previewUri = null,
            isFavorite = false,
            accentColor = Color(0xFFFF9500),
        )

        val result = CreationActions.toggleFavorite(mockContext, item)
        assertTrue("Toggling favorite on video LibraryItem should return true", result)
        assertTrue(GeneratedVideosRepository.findById(video.id)?.isFavorite == true)
    }

    @Test
    fun testCreationActionsToggleFavoriteForImageLibraryItem() {
        val dummyBytes = ByteArray(16) { 3 }
        val creation = GeneratedCreationsRepository.add(
            context = mockContext,
            bytes = dummyBytes,
            prompt = "Mountain landscape",
            styleTitle = "Anime",
            aspectLabel = "16:9",
            width = 1024,
            height = 576,
        )

        val item = LibraryItem(
            id = creation.id,
            source = LibrarySource.Creation,
            type = LibraryMediaType.Image,
            createdAt = creation.createdAt,
            title = creation.prompt,
            previewUri = null,
            isFavorite = false,
            accentColor = Color(0xFF7C4DFF),
        )

        val result = CreationActions.toggleFavorite(mockContext, item)
        assertTrue("Toggling favorite on image LibraryItem should return true", result)
        assertTrue(GeneratedCreationsRepository.findById(creation.id)?.isFavorite == true)
    }

    @Test
    fun testLibraryRepositoryPropagatesVideoFavoriteState() {
        val dummyBytes = ByteArray(16) { 4 }
        val video = GeneratedVideosRepository.add(
            context = mockContext,
            bytes = dummyBytes,
            prompt = "Futuristic city",
            styleTitle = "Sci-Fi",
            durationSeconds = 10,
            cameraMotion = "orbit",
            aspectLabel = "16:9",
        )

        GeneratedVideosRepository.toggleFavorite(mockContext, video.id)

        val items = LibraryRepository.buildItems(mockContext)
        val videoItem = items.firstOrNull { it.id == video.id && it.source == LibrarySource.Video }

        assertEquals(true, videoItem?.isFavorite)
    }

    @Test
    fun testDownloadImageFromPayloadHandlesEmptyOrNullGracefully() = kotlinx.coroutines.runBlocking {
        val emptyPayload = com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedImageStore.Payload(
            bytes = ByteArray(0),
            uri = null,
            prompt = "test",
            styleTitle = "test",
            aspectLabel = "1:1",
            width = 512,
            height = 512,
            creationId = null,
        )
        val result = CreationActions.downloadImageFromPayload(mockContext, emptyPayload)
        assertFalse("Downloading empty payload should fail gracefully", result)
    }

    @Test
    fun testDownloadVideoFromPayloadHandlesEmptyOrNullGracefully() = kotlinx.coroutines.runBlocking {
        val emptyPayload = com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideoStore.Payload(
            uri = null,
            prompt = "test",
            styleTitle = "test",
            durationSeconds = 5,
            cameraMotion = "none",
            aspectLabel = "16:9",
            creationId = null,
        )
        val result = CreationActions.downloadVideoFromPayload(mockContext, emptyPayload)
        assertFalse("Downloading empty video payload should fail gracefully", result)
    }
}
