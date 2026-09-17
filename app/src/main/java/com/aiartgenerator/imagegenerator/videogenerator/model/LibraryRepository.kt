package com.aiartgenerator.imagegenerator.videogenerator.model

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.aiartgenerator.imagegenerator.videogenerator.R

enum class LibraryFilter(@StringRes val labelRes: Int) {
    All(R.string.library_tab_all),
    Images(R.string.library_tab_images),
    Videos(R.string.library_tab_videos),
    Music(R.string.library_tab_music),
    Saved(R.string.library_tab_saved),
}

enum class FavouritesFilter(@StringRes val labelRes: Int) {
    All(R.string.library_tab_all),
    Images(R.string.library_tab_images),
    Videos(R.string.library_tab_videos),
    Music(R.string.library_tab_music),
}

enum class LibraryMediaType(val badge: String) {
    Image("Image"),
    Video("Video"),
    Music("Music"),
    Cover("Cover"),
}

enum class LibrarySource {
    Creation,
    Video,
    Music,
    Cover,
}

data class LibraryItem(
    val id: String,
    val source: LibrarySource,
    val type: LibraryMediaType,
    val createdAt: Long,
    val title: String,
    val previewUri: Uri?,
    val isFavorite: Boolean,
    val accentColor: Color,
)

object LibraryRepository {
    fun buildItems(context: Context): List<LibraryItem> = buildList {
        GeneratedCreationsRepository.items.forEach { creation ->
            add(
                LibraryItem(
                    id = creation.id,
                    source = LibrarySource.Creation,
                    type = LibraryMediaType.Image,
                    createdAt = creation.createdAt,
                    title = creation.prompt.take(40).ifBlank { "AI Image" },
                    previewUri = GeneratedCreationsRepository.getUri(context, creation),
                    isFavorite = creation.isFavorite,
                    accentColor = Color(0xFF7C4DFF),
                ),
            )
        }
        GeneratedVideosRepository.items.forEach { video ->
            add(
                LibraryItem(
                    id = video.id,
                    source = LibrarySource.Video,
                    type = LibraryMediaType.Video,
                    createdAt = video.createdAt,
                    title = video.title,
                    previewUri = GeneratedVideosRepository.getUri(context, video),
                    isFavorite = video.isFavorite,
                    accentColor = Color(0xFFFF9500),
                ),
            )
        }
        GeneratedMusicsRepository.items.forEach { music ->
            add(
                LibraryItem(
                    id = music.id,
                    source = LibrarySource.Music,
                    type = LibraryMediaType.Music,
                    createdAt = music.createdAt,
                    title = music.title,
                    previewUri = GeneratedMusicsRepository.getUri(context, music),
                    isFavorite = music.isFavorite,
                    accentColor = Color(0xFF26C6DA),
                ),
            )
        }
        GeneratedCoversRepository.items.forEach { cover ->
            add(
                LibraryItem(
                    id = cover.id,
                    source = LibrarySource.Cover,
                    type = LibraryMediaType.Cover,
                    createdAt = cover.createdAt,
                    title = cover.title,
                    previewUri = GeneratedCoversRepository.getUri(context, cover),
                    isFavorite = cover.isFavorite,
                    accentColor = Color(0xFF7C4DFF),
                ),
            )
        }
    }.sortedByDescending { it.createdAt }

    fun filterItems(items: List<LibraryItem>, filter: LibraryFilter): List<LibraryItem> =
        when (filter) {
            LibraryFilter.All -> items
            LibraryFilter.Images -> items.filter { it.type == LibraryMediaType.Image }
            LibraryFilter.Videos -> items.filter { it.type == LibraryMediaType.Video }
            LibraryFilter.Music -> items.filter {
                it.type == LibraryMediaType.Music || it.type == LibraryMediaType.Cover
            }
            LibraryFilter.Saved -> items.filter { it.isFavorite }
        }

    fun filterFavourites(items: List<LibraryItem>, filter: FavouritesFilter): List<LibraryItem> {
        val favourites = items.filter { it.isFavorite }
        return when (filter) {
            FavouritesFilter.All -> favourites
            FavouritesFilter.Images -> favourites.filter { it.type == LibraryMediaType.Image }
            FavouritesFilter.Videos -> favourites.filter { it.type == LibraryMediaType.Video }
            FavouritesFilter.Music -> favourites.filter {
                it.type == LibraryMediaType.Music || it.type == LibraryMediaType.Cover
            }
        }
    }

    fun openItem(context: Context, item: LibraryItem): Boolean {
        return when (item.source) {
            LibrarySource.Creation -> {
                GeneratedCreationsRepository.findById(item.id)?.let {
                    GeneratedCreationsRepository.loadIntoStore(context, it)
                    true
                } ?: false
            }
            LibrarySource.Video -> {
                GeneratedVideosRepository.findById(item.id)?.let {
                    GeneratedVideosRepository.loadIntoStore(context, it)
                    true
                } ?: false
            }
            LibrarySource.Music -> {
                GeneratedMusicsRepository.findById(item.id)?.let {
                    GeneratedMusicsRepository.loadIntoStore(context, it)
                    true
                } ?: false
            }
            LibrarySource.Cover -> {
                GeneratedCoversRepository.findById(item.id)?.let {
                    GeneratedCoversRepository.loadIntoStore(context, it)
                    true
                } ?: false
            }
        }
    }
}
