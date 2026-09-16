package com.owetrack.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OweTrackDao {
    @Query("""SELECT p.*, COALESCE(SUM(CASE WHEN t.type='GIVEN' THEN t.amountPaise ELSE 0 END),0) totalGivenPaise, COALESCE(SUM(CASE WHEN t.type='RECEIVED' THEN t.amountPaise ELSE 0 END),0) totalReceivedPaise, MAX(t.transactionDate) lastActivity, MIN(t.transactionDate) firstActivity, COUNT(t.transactionId) transactionCount FROM people p LEFT JOIN transactions t ON p.personId=t.personId GROUP BY p.personId""")
    fun observePeople(): Flow<List<PersonSummary>>

    @Query("""SELECT p.*, COALESCE(SUM(CASE WHEN t.type='GIVEN' THEN t.amountPaise ELSE 0 END),0) totalGivenPaise, COALESCE(SUM(CASE WHEN t.type='RECEIVED' THEN t.amountPaise ELSE 0 END),0) totalReceivedPaise, MAX(t.transactionDate) lastActivity, MIN(t.transactionDate) firstActivity, COUNT(t.transactionId) transactionCount FROM people p LEFT JOIN transactions t ON p.personId=t.personId WHERE p.personId=:id GROUP BY p.personId""")
    fun observePerson(id: Long): Flow<PersonSummary?>

    @Query("SELECT * FROM people WHERE personId=:id") suspend fun person(id: Long): PersonEntity?
    @Query("SELECT name FROM people ORDER BY updatedAt DESC") suspend fun nameSuggestions(): List<String>
    @Insert suspend fun insertPerson(person: PersonEntity): Long
    @Update suspend fun updatePerson(person: PersonEntity)
    @Delete suspend fun deletePerson(person: PersonEntity)

    @Query("SELECT * FROM transactions WHERE personId=:personId ORDER BY transactionDate DESC, createdAt DESC")
    fun observeTransactions(personId: Long): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE transactionId=:id") suspend fun transaction(id: Long): TransactionEntity?
    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, createdAt DESC LIMIT :limit") fun observeRecent(limit: Int): Flow<List<TransactionEntity>>
    @Query("SELECT t.*, p.name personName FROM transactions t JOIN people p ON p.personId=t.personId ORDER BY t.transactionDate DESC, t.createdAt DESC") fun observeAllTransactions(): Flow<List<TransactionWithPerson>>
    @Insert suspend fun insertTransaction(transaction: TransactionEntity): Long
    @Update suspend fun updateTransaction(transaction: TransactionEntity)
    @Delete suspend fun deleteTransaction(transaction: TransactionEntity)
    @Query("SELECT DISTINCT purpose FROM transactions WHERE purpose IS NOT NULL AND purpose != '' ORDER BY updatedAt DESC LIMIT 20") suspend fun purposeSuggestions(): List<String>
    @Query("SELECT * FROM transactions WHERE personId=:personId ORDER BY updatedAt DESC LIMIT 1") suspend fun latestFor(personId: Long): TransactionEntity?
    @Query("SELECT * FROM transactions ORDER BY transactionDate ASC, createdAt ASC") suspend fun allTransactions(): List<TransactionEntity>
    @Query("SELECT * FROM people ORDER BY name") suspend fun allPeople(): List<PersonEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePeople(items: List<PersonEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreTransactions(items: List<TransactionEntity>)
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM people") suspend fun clearPeople()
    @Transaction suspend fun replaceAll(people:List<PersonEntity>,transactions:List<TransactionEntity>){clearTransactions();clearPeople();restorePeople(people);restoreTransactions(transactions)}
}
