package com.albumtags.app

import android.app.Application
import com.albumtags.app.di.AppContainer

class AlbumTagsApplication : Application() {
    val container: AppContainer by lazy { AppContainer(applicationContext) }
}
