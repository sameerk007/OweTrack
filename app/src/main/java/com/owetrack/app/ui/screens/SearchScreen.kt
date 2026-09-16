package com.owetrack.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.owetrack.app.OweTrackViewModel
import com.owetrack.app.data.*
import com.owetrack.app.ui.components.TransactionRow
import java.time.LocalDate
import androidx.compose.runtime.saveable.rememberSaveable

private enum class SearchPeriod(val label:String){ALL("Any date"),TODAY("Today"),MONTH("This month"),YEAR("This year"),CUSTOM("Custom")}

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun SearchScreen(vm:OweTrackViewModel,onBack:()->Unit,onPerson:(Long)->Unit){
    val all by vm.allTransactions.collectAsState();var query by rememberSaveable{mutableStateOf("")};var type by remember{mutableStateOf<TransactionType?>(null)};var method by remember{mutableStateOf<PaymentMethod?>(null)};var period by remember{mutableStateOf(SearchPeriod.ALL)};var customStart by remember{mutableStateOf(LocalDate.now().minusMonths(1))};var customEnd by remember{mutableStateOf(LocalDate.now())};val today=LocalDate.now();val start=when(period){SearchPeriod.ALL->null;SearchPeriod.TODAY->today;SearchPeriod.MONTH->today.withDayOfMonth(1);SearchPeriod.YEAR->today.withDayOfYear(1);SearchPeriod.CUSTOM->customStart};val end=if(period==SearchPeriod.CUSTOM)customEnd else today
    val filtered=all.filter{r->(query.isBlank()||listOf(r.personName,r.transaction.purpose,r.transaction.notes).any{it?.contains(query,true)==true})&&(type==null||r.transaction.type==type)&&(method==null||r.transaction.paymentMethod==method)&&(start==null||r.transaction.transactionDate>=start.toEpochDay())&&r.transaction.transactionDate<=end.toEpochDay()}
    Scaffold(topBar={TopAppBar({Text("Search transactions")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){pad->LazyColumn(Modifier.padding(pad),contentPadding=PaddingValues(bottom=30.dp)){
        item{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),placeholder={Text("Person, purpose, or notes")},leadingIcon={Icon(Icons.Default.Search,null)});FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(type==null,{type=null},{Text("All")});TransactionType.entries.forEach{v->FilterChip(type==v,{type=v},{Text(v.name.lowercase().replaceFirstChar(Char::uppercase))})}};FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){PaymentMethod.entries.forEach{v->FilterChip(method==v,{method=if(method==v)null else v},{Text(v.name.replace('_',' ').lowercase().replaceFirstChar(Char::uppercase))})}};FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){SearchPeriod.entries.forEach{p->FilterChip(period==p,{period=p},{Text(p.label)})}};if(period==SearchPeriod.CUSTOM)SearchDateRange(customStart,customEnd,{customStart=it},{customEnd=it});Text("${filtered.size} results")}}
        items(filtered,key={it.transaction.transactionId}){r->TransactionRow(r.transaction,personName=r.personName,onClick={onPerson(r.transaction.personId)});HorizontalDivider()}
    }}
}
@Composable private fun SearchDateRange(start:LocalDate,end:LocalDate,onStart:(LocalDate)->Unit,onEnd:(LocalDate)->Unit){val c=LocalContext.current;Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({android.app.DatePickerDialog(c,{_,y,m,d->onStart(LocalDate.of(y,m+1,d))},start.year,start.monthValue-1,start.dayOfMonth).show()},Modifier.weight(1f)){Text("From $start")};OutlinedButton({android.app.DatePickerDialog(c,{_,y,m,d->onEnd(LocalDate.of(y,m+1,d))},end.year,end.monthValue-1,end.dayOfMonth).show()},Modifier.weight(1f)){Text("To $end")}}}
