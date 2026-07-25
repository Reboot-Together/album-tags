package com.albumtags.app

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import com.albumtags.app.di.AppContainer

class AlbumTagsApplication : Application(), SingletonImageLoader.Factory {
    val container: AppContainer by lazy { AppContainer(applicationContext) }

    override fun newImageLoader(context: android.content.Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
}
