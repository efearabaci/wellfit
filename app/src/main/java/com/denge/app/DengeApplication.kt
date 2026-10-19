package com.denge.app

import android.app.Application
import androidx.room.Room
import com.denge.app.data.*
import com.denge.app.integration.*

class AppGraph(val app: Application) {
    private val db = Room.databaseBuilder(app, DengeDatabase::class.java, "denge.db").build()
    val repo = JournalRepository(app, db)
    val secrets = SecretStore(app)
    val remote = RemoteServices()
    val steps = StepService(app)
    val backup = BackupService(app)
}
class DengeApplication : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}
