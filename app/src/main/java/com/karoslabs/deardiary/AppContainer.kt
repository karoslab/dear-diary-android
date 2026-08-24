package com.karoslabs.deardiary

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.karoslabs.deardiary.data.audio.AudioStorage
import com.karoslabs.deardiary.data.backup.BackupManager
import com.karoslabs.deardiary.data.db.AppDatabase
import com.karoslabs.deardiary.data.prefs.UserPrefs
import com.karoslabs.deardiary.data.repository.JournalRepository
import com.karoslabs.deardiary.data.speech.OnDeviceSpeech

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val db: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "dear_diary.db",
    ).fallbackToDestructiveMigration(dropAllTables = true)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }
        })
        .build()

    val audioStorage = AudioStorage(appContext)
    val backup = BackupManager(audioStorage)
    val repository = JournalRepository(db.journalDao(), audioStorage, backup)
    val userPrefs = UserPrefs(appContext)
    val speech = OnDeviceSpeech(appContext)
}

class DearDiaryApp : android.app.Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
