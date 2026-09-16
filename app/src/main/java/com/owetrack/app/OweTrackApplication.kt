package com.owetrack.app

import android.app.Application
import com.owetrack.app.data.*

class OweTrackApplication : Application() {
    val database by lazy { OweTrackDatabase.get(this) }
    val repository by lazy { LendingRepository(database.dao()) }
    val preferences by lazy { AppPreferences(this) }
}
