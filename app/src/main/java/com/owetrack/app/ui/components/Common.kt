package com.owetrack.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.owetrack.app.data.*
import com.owetrack.app.domain.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable fun BalanceText(balance:Long, modifier:Modifier=Modifier, large:Boolean=false) {
    val color=when { balance>0 -> MaterialTheme.colorScheme.error; balance<0 -> Color(0xFF2563EB); else -> MaterialTheme.colorScheme.primary }
    val label=when { balance>0 -> "${Money.format(balance)} Due"; balance<0 -> "${Money.format(-balance)} Credit"; else -> "Settled" }
    Text(label, modifier, color=color, fontWeight=FontWeight.SemiBold, fontSize=if(large) 30.sp else 16.sp)
}

@Composable fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title:String, message:String, action:String?=null, onAction:()->Unit={}) {
    Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Icon(icon,null,Modifier.size(56.dp),tint=MaterialTheme.colorScheme.primary); Text(title,style=MaterialTheme.typography.titleMedium); Text(message,color=MaterialTheme.colorScheme.onSurfaceVariant)
        action?.let { Button(onClick=onAction){ Text(it) } }
    }
}

fun TransactionEntity.methodLabel():String = when(paymentMethod){ PaymentMethod.CASH->"Cash"; PaymentMethod.BANK_TRANSFER->"Bank transfer"; PaymentMethod.UPI->when(upiProvider){UpiProvider.PHONEPE->"PhonePe";UpiProvider.GPAY->"GPay";UpiProvider.PAYTM->"Paytm";else->"UPI"} }
fun Long.asDate(pattern:String="d MMM yyyy"):String = LocalDate.ofEpochDay(this).format(DateTimeFormatter.ofPattern(pattern))

@Composable fun TransactionRow(tx:TransactionEntity, running:Long?=null, personName:String?=null, onClick:()->Unit={}) {
    ListItem(
        headlineContent={Text((if(tx.type==TransactionType.GIVEN) "Gave " else "Received ")+Money.format(tx.amountPaise),fontWeight=FontWeight.SemiBold)},
        supportingContent={Column { Text(listOfNotNull(personName,tx.methodLabel(),tx.purpose).joinToString(" • ")); tx.notes?.let{Text(it,maxLines=1)}; running?.let{Text("Balance ${Money.format(it)}",color=MaterialTheme.colorScheme.onSurfaceVariant)} }},
        leadingContent={FilledTonalIconButton(onClick=onClick){Icon(if(tx.type==TransactionType.GIVEN) Icons.Default.NorthEast else Icons.Default.SouthWest,null,tint=if(tx.type==TransactionType.GIVEN) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)}},
        trailingContent={Text(tx.transactionDate.asDate("d MMM"))}, modifier=Modifier.fillMaxWidth().clickable(onClick=onClick)
    )
}

@Composable fun <T> ChoiceMenu(label:String, selected:T, values:List<T>, display:(T)->String, onSelect:(T)->Unit) {
    var open by remember{mutableStateOf(false)}
    Box { OutlinedButton(onClick={open=true}){Text("$label: ${display(selected)}");Icon(Icons.Default.ArrowDropDown,null)}
        DropdownMenu(open,{open=false}){values.forEach{v->DropdownMenuItem({Text(display(v))},{onSelect(v);open=false})}}
    }
}
