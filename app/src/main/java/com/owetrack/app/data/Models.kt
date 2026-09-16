package com.owetrack.app.data

import androidx.room.*

enum class TransactionType { GIVEN, RECEIVED }
enum class PaymentMethod { CASH, UPI, BANK_TRANSFER }
enum class UpiProvider { PHONEPE, GPAY, PAYTM, OTHER }

@Entity(tableName = "people", indices = [Index(value = ["name"])])
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val personId: Long = 0,
    val name: String,
    val phone: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(entity = PersonEntity::class, parentColumns = ["personId"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personId"), Index("transactionDate")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val transactionId: Long = 0,
    val personId: Long,
    val type: TransactionType,
    /** Exact amount in paise; never floating point. */ val amountPaise: Long,
    /** Local calendar date stored as epoch day. */ val transactionDate: Long,
    val paymentMethod: PaymentMethod,
    val upiProvider: UpiProvider? = null,
    val purpose: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class PersonSummary(
    @Embedded val person: PersonEntity,
    val totalGivenPaise: Long,
    val totalReceivedPaise: Long,
    val lastActivity: Long?,
    val firstActivity: Long?,
    val transactionCount: Int
) { val balancePaise get() = totalGivenPaise - totalReceivedPaise }

data class TransactionWithPerson(@Embedded val transaction: TransactionEntity, @ColumnInfo(name="personName") val personName: String)

data class MonthlyTotal(val monthKey: String, val type: TransactionType, val totalPaise: Long)
data class MethodTotal(val paymentMethod: PaymentMethod, val totalPaise: Long)
data class UpiTotal(val upiProvider: UpiProvider?, val totalPaise: Long)

class Converters {
    @TypeConverter fun transactionType(value: String) = TransactionType.valueOf(value)
    @TypeConverter fun transactionType(value: TransactionType) = value.name
    @TypeConverter fun paymentMethod(value: String) = PaymentMethod.valueOf(value)
    @TypeConverter fun paymentMethod(value: PaymentMethod) = value.name
    @TypeConverter fun upiProvider(value: String?) = value?.let(UpiProvider::valueOf)
    @TypeConverter fun upiProvider(value: UpiProvider?) = value?.name
}
