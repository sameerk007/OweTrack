package com.owetrack.app.domain

import com.owetrack.app.data.*
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    private fun tx(id: Long, type: TransactionType, rupees: Long, date: Long=id) = TransactionEntity(id, 1, type, rupees*100, date, PaymentMethod.CASH, createdAt=id)
    @Test fun `balance follows lending sequence`() {
        val items = mutableListOf(tx(1,TransactionType.GIVEN,5000))
        assertEquals(500000L, Money.balance(items)); items += tx(2,TransactionType.RECEIVED,2000); assertEquals(300000L, Money.balance(items))
        items += tx(3,TransactionType.GIVEN,3000); assertEquals(600000L, Money.balance(items)); items += tx(4,TransactionType.RECEIVED,1500); assertEquals(450000L, Money.balance(items))
    }
    @Test fun `overpayment is a negative balance`() = assertEquals(-10000L, Money.balance(listOf(tx(1,TransactionType.GIVEN,100),tx(2,TransactionType.RECEIVED,200))))
    @Test fun `decimal values are exact`() { assertEquals(12345L, Money.parseToPaise("123.45")); assertNull(Money.parseToPaise("0")); assertNull(Money.parseToPaise("-1")); assertNull(Money.parseToPaise("1.999")) }
    @Test fun `same day uses creation order`() { val values=listOf(tx(2,TransactionType.RECEIVED,20,1),tx(1,TransactionType.GIVEN,50,1)); assertEquals(5000L,Money.runningBalances(values)[1]); assertEquals(3000L,Money.runningBalances(values)[2]) }
    @Test fun `editing recalculates balance from changed transaction`() { val values=mutableListOf(tx(1,TransactionType.GIVEN,5000),tx(2,TransactionType.RECEIVED,2000));values[1]=tx(2,TransactionType.RECEIVED,1500);assertEquals(350000L,Money.balance(values)) }
    @Test fun `deleting removes transaction from calculation`() { val values=mutableListOf(tx(1,TransactionType.GIVEN,5000),tx(2,TransactionType.RECEIVED,2000));values.removeAt(1);assertEquals(500000L,Money.balance(values)) }
    @Test fun `multiple repayments calculate correctly`() = assertEquals(200000L,Money.balance(listOf(tx(1,TransactionType.GIVEN,5000),tx(2,TransactionType.RECEIVED,1000),tx(3,TransactionType.RECEIVED,2000))))
    @Test fun `running balance follows transaction date not input order`() { val values=listOf(tx(3,TransactionType.RECEIVED,1000,3),tx(1,TransactionType.GIVEN,5000,1),tx(2,TransactionType.GIVEN,500,2));val result=Money.runningBalances(values);assertEquals(500000L,result[1]);assertEquals(550000L,result[2]);assertEquals(450000L,result[3]) }
    @Test fun `large values remain exact`() = assertEquals(9_000_000_000_00L, Money.balance(listOf(tx(1,TransactionType.GIVEN,9_000_000_000L))))
}
