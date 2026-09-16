package com.owetrack.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.owetrack.app.OweTrackViewModel
import com.owetrack.app.data.*
import com.owetrack.app.domain.Money
import com.owetrack.app.ui.components.ChoiceMenu
import kotlinx.coroutines.launch
import java.time.LocalDate

private val commonPurposes=listOf("Dinner","Emergency","Shopping","Rent","Travel","Loan","Ticket","Medical","Other")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable fun TransactionScreen(vm:OweTrackViewModel,personId:Long,type:TransactionType,transactionId:Long=0,onBack:()->Unit,onSaved:()->Unit){
    val context=LocalContext.current;val scope=rememberCoroutineScope();val prefs by vm.preferences.collectAsState();var amount by rememberSaveable{mutableStateOf("")};var date by rememberSaveable{mutableStateOf(LocalDate.now())};var method by rememberSaveable{mutableStateOf(prefs.defaultMethod)};var provider by rememberSaveable{mutableStateOf(UpiProvider.PHONEPE)};var purpose by rememberSaveable{mutableStateOf("")};var notes by rememberSaveable{mutableStateOf("")};var originalReceivedPaise by remember{mutableLongStateOf(0L)};var suggestions by remember{mutableStateOf(commonPurposes)};var error by remember{mutableStateOf<String?>(null)};var overpay by remember{mutableStateOf(false)};var largeConfirmed by remember{mutableStateOf(false)};val person by vm.repository.person(personId).collectAsState(initial=null)
    LaunchedEffect(personId,transactionId){suggestions=(vm.repository.purposeSuggestions()+commonPurposes).distinct();val existing=if(transactionId>0)vm.repository.transaction(transactionId)else null;val latest=vm.repository.latestFor(personId);if(existing!=null){amount=(existing.amountPaise/100.0).toString().removeSuffix(".0");date=LocalDate.ofEpochDay(existing.transactionDate);method=existing.paymentMethod;provider=existing.upiProvider?:UpiProvider.PHONEPE;purpose=existing.purpose.orEmpty();notes=existing.notes.orEmpty();if(existing.type==TransactionType.RECEIVED)originalReceivedPaise=existing.amountPaise}else if(latest!=null){method=latest.paymentMethod;provider=latest.upiProvider?:UpiProvider.PHONEPE}}
    fun submit(){val paise=Money.parseToPaise(amount);if(paise==null){error="Enter an amount greater than zero";return};if(paise>=100_00_000_00L&&!largeConfirmed){largeConfirmed=true;return};val available=(person?.balancePaise?:Long.MAX_VALUE)+originalReceivedPaise;if(type==TransactionType.RECEIVED && paise>available && !overpay){overpay=true;return};scope.launch{vm.saveTransaction(transactionId,personId,type,paise,date,method,provider,purpose.takeIf{type==TransactionType.GIVEN},notes);onSaved()}}
    Scaffold(topBar={TopAppBar({Text(if(transactionId>0)"Edit transaction" else if(type==TransactionType.GIVEN)"Give money" else "Receive money")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){pad->Column(Modifier.padding(pad).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        OutlinedTextField(amount,{amount=it;error=null},Modifier.fillMaxWidth(),label={Text("Amount")},prefix={Text("₹ ")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,isError=error!=null,supportingText=error?.let{{Text(it)}})
        OutlinedButton(onClick={DatePickerDialog(context,{_,y,m,d->date=LocalDate.of(y,m+1,d)},date.year,date.monthValue-1,date.dayOfMonth).show()},Modifier.fillMaxWidth()){Icon(Icons.Default.CalendarMonth,null);Spacer(Modifier.width(8.dp));Text(date.toString())}
        Text("Payment method",style=MaterialTheme.typography.labelLarge);FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){PaymentMethod.entries.forEach{m->FilterChip(method==m,{method=m},{Text(when(m){PaymentMethod.CASH->"Cash";PaymentMethod.UPI->"UPI";PaymentMethod.BANK_TRANSFER->"Bank transfer"})})}}
        if(method==PaymentMethod.UPI){ChoiceMenu("UPI app",provider,UpiProvider.entries.toList(),{when(it){UpiProvider.PHONEPE->"PhonePe";UpiProvider.GPAY->"GPay";UpiProvider.PAYTM->"Paytm";UpiProvider.OTHER->"Other"}}, {provider=it})}
        if(type==TransactionType.GIVEN){Text("Given for",style=MaterialTheme.typography.labelLarge);FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){suggestions.take(8).forEach{p->SuggestionChip({purpose=p},{Text(p)})}};OutlinedTextField(purpose,{purpose=it},Modifier.fillMaxWidth(),label={Text("Purpose")},singleLine=true)}
        OutlinedTextField(notes,{notes=it},Modifier.fillMaxWidth(),label={Text("Notes (optional)")},minLines=2)
        Button(::submit,Modifier.fillMaxWidth().height(54.dp)){Text(if(type==TransactionType.GIVEN)"Save transaction" else "Save repayment")}
    }}
    if(overpay)AlertDialog({overpay=false},confirmButton={TextButton({submit()}){Text("Save anyway")}},dismissButton={TextButton({overpay=false}){Text("Review")}},title={Text("Repayment exceeds outstanding")},text={Text("This will create a credit balance. You can still record it.")})
    if(largeConfirmed&&!overpay)AlertDialog({largeConfirmed=false},confirmButton={TextButton({submit()}){Text("Confirm amount")}},dismissButton={TextButton({largeConfirmed=false}){Text("Review")}},title={Text("Confirm large amount")},text={Text("You entered ${Money.parseToPaise(amount)?.let(Money::format)}. Please verify it before saving.")})
}
