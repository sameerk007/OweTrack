package com.owetrack.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("preferences")
enum class AppTheme { SYSTEM, LIGHT, DARK }
data class UserPreferences(val appLock: Boolean=false, val defaultMethod: PaymentMethod=PaymentMethod.UPI, val theme: AppTheme=AppTheme.SYSTEM, val showRunningBalance: Boolean=true)

class AppPreferences(private val context: Context) {
    private object Keys { val lock=booleanPreferencesKey("app_lock"); val method=stringPreferencesKey("default_method"); val theme=stringPreferencesKey("theme"); val running=booleanPreferencesKey("running_balance") }
    val values = context.dataStore.data.map { p -> UserPreferences(p[Keys.lock] ?: false, p[Keys.method]?.let(PaymentMethod::valueOf) ?: PaymentMethod.UPI, p[Keys.theme]?.let(AppTheme::valueOf) ?: AppTheme.SYSTEM, p[Keys.running] ?: true) }
    suspend fun setLock(value:Boolean)=context.dataStore.edit { it[Keys.lock]=value }
    suspend fun setMethod(value:PaymentMethod)=context.dataStore.edit { it[Keys.method]=value.name }
    suspend fun setTheme(value:AppTheme)=context.dataStore.edit { it[Keys.theme]=value.name }
    suspend fun setRunning(value:Boolean)=context.dataStore.edit { it[Keys.running]=value }
}
