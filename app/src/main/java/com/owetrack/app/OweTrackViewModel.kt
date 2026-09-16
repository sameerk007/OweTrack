package com.owetrack.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.biometric.BiometricManager
import com.owetrack.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class PeopleFilter { ACTIVE, SETTLED, ALL }
enum class PeopleSort { OUTSTANDING, NAME, RECENT, OLDEST }
data class HomeOptions(val query:String="", val filter:PeopleFilter=PeopleFilter.ALL, val sort:PeopleSort=PeopleSort.OUTSTANDING)

class OweTrackViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app as OweTrackApplication
    val repository = container.repository
    val preferences = container.preferences.values.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())
    private val options = MutableStateFlow(HomeOptions())
    val homeOptions = options.asStateFlow()
    val people = combine(repository.people, options) { items, o ->
        items.filter { it.person.name.contains(o.query, true) }.filter {
            when(o.filter){ PeopleFilter.ACTIVE -> it.balancePaise != 0L; PeopleFilter.SETTLED -> it.balancePaise == 0L; PeopleFilter.ALL -> true }
        }.let { filtered -> when(o.sort) {
            PeopleSort.OUTSTANDING -> filtered.sortedWith(compareByDescending<PersonSummary>{it.balancePaise > 0}.thenByDescending{it.balancePaise}.thenBy{it.person.name})
            PeopleSort.NAME -> filtered.sortedBy { it.person.name.lowercase() }
            PeopleSort.RECENT -> filtered.sortedByDescending { it.lastActivity ?: (it.person.createdAt / 86_400_000L) }
            PeopleSort.OLDEST -> filtered.sortedBy { it.firstActivity ?: Long.MAX_VALUE }
        }}
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allPeople = repository.people.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allTransactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun setQuery(value:String){ options.update { it.copy(query=value) } }
    fun setFilter(value:PeopleFilter){ options.update { it.copy(filter=value) } }
    fun setSort(value:PeopleSort){ options.update { it.copy(sort=value) } }
    suspend fun addPerson(name:String,phone:String?,notes:String?) = repository.addPerson(name,phone,notes)
    suspend fun saveTransaction(id:Long, personId:Long, type:TransactionType, amountPaise:Long, date:LocalDate, method:PaymentMethod, provider:UpiProvider?, purpose:String?, notes:String?) = repository.saveTransaction(
        TransactionEntity(id,personId,type,amountPaise,date.toEpochDay(),method,if(method==PaymentMethod.UPI) provider else null,purpose?.trim()?.ifBlank{null},notes?.trim()?.ifBlank{null}, createdAt=repository.transaction(id)?.createdAt ?: System.currentTimeMillis(), updatedAt=System.currentTimeMillis())
    )
    fun deleteTransaction(tx:TransactionEntity)=viewModelScope.launch { repository.deleteTransaction(tx) }
    fun setLock(v:Boolean)=viewModelScope.launch {
        val authenticators=BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if(!v || BiometricManager.from(getApplication()).canAuthenticate(authenticators)==BiometricManager.BIOMETRIC_SUCCESS) container.preferences.setLock(v)
    }
    fun setMethod(v:PaymentMethod)=viewModelScope.launch { container.preferences.setMethod(v) }
    fun setTheme(v:AppTheme)=viewModelScope.launch { container.preferences.setTheme(v) }
    fun setRunning(v:Boolean)=viewModelScope.launch { container.preferences.setRunning(v) }
}
