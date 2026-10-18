package com.lhy.deepseekmobile

import android.content.Context
import com.lhy.deepseekmobile.data.ConversationStore
import com.lhy.deepseekmobile.data.ModeStore
import com.lhy.deepseekmobile.data.SettingsStore
import com.lhy.deepseekmobile.ingest.TextIngestor
import com.lhy.deepseekmobile.media.ImageStore
import com.lhy.deepseekmobile.media.OcrEngine
import com.lhy.deepseekmobile.net.ChatApi
import java.io.File

class AppContainer(context: Context) {

    private val filesDir: File = context.filesDir
    private val appContext: Context = context.applicationContext

    val settings = SettingsStore(File(filesDir, "settings.json"))
    val modes = ModeStore(File(filesDir, "modes.json"))
    val conversations = ConversationStore(File(filesDir, "conversations"))
    val api = ChatApi()
    val ocr = OcrEngine(appContext)
    val images = ImageStore(appContext, filesDir)
    val ingestor = TextIngestor(appContext)

    val attachmentsDir: File = File(filesDir, "attachments").apply { mkdirs() }
}
