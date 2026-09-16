package com.owetrack.app.data

import android.content.Context
import androidx.room.*

@Database(entities = [PersonEntity::class, TransactionEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class OweTrackDatabase : RoomDatabase() {
    abstract fun dao(): OweTrackDao
    companion object {
        @Volatile private var instance: OweTrackDatabase? = null
        fun get(context: Context): OweTrackDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, OweTrackDatabase::class.java, "owetrack.db").build().also { instance = it }
        }
    }
}
