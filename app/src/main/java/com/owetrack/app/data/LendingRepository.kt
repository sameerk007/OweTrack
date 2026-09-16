package com.owetrack.app.data

import kotlinx.coroutines.flow.Flow

class LendingRepository(private val dao: OweTrackDao) {
    val people = dao.observePeople()
    val allTransactions = dao.observeAllTransactions()
    fun person(id: Long) = dao.observePerson(id)
    fun transactions(id: Long) = dao.observeTransactions(id)
    fun recent(limit: Int = 8) = dao.observeRecent(limit)
    suspend fun addPerson(name: String, phone: String?, notes: String?) = dao.insertPerson(PersonEntity(name=name.trim(), phone=phone?.trim()?.ifBlank { null }, notes=notes?.trim()?.ifBlank { null }))
    suspend fun saveTransaction(value: TransactionEntity) = if (value.transactionId == 0L) dao.insertTransaction(value) else { dao.updateTransaction(value); value.transactionId }
    suspend fun transaction(id: Long) = dao.transaction(id)
    suspend fun deleteTransaction(value: TransactionEntity) = dao.deleteTransaction(value)
    suspend fun latestFor(personId: Long) = dao.latestFor(personId)
    suspend fun purposeSuggestions() = dao.purposeSuggestions()
    suspend fun nameSuggestions() = dao.nameSuggestions()
    suspend fun allPeople() = dao.allPeople()
    suspend fun allTransactionsSnapshot() = dao.allTransactions()
    suspend fun restore(people: List<PersonEntity>, transactions: List<TransactionEntity>) = dao.replaceAll(people,transactions)
}
