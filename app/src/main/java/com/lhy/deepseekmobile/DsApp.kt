package com.lhy.deepseekmobile

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class DsApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        container = AppContainer(this)
    }
}
