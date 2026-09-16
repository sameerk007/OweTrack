package com.owetrack.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owetrack.app.OweTrackViewModel
import com.owetrack.app.data.*
import com.owetrack.app.domain.Money
import java.time.LocalDate
import java.time.YearMonth

enum class AnalyticsPeriod(val label:String){MONTH("This month"),THREE("3 months"),SIX("6 months"),YEAR("This year"),ALL("All time"),CUSTOM("Custom")}
data class MonthPair(val month:YearMonth,val given:Long,val received:Long)

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun AnalyticsScreen(vm:OweTrackViewModel){
    val people by vm.allPeople.collectAsState();val allTx by vm.allTransactions.collectAsState();var period by remember{mutableStateOf(AnalyticsPeriod.SIX)}
    var customStart by remember{mutableStateOf(LocalDate.now().minusMonths(1))};var customEnd by remember{mutableStateOf(LocalDate.now())};val today=LocalDate.now();val start=when(period){AnalyticsPeriod.MONTH->today.withDayOfMonth(1);AnalyticsPeriod.THREE->YearMonth.now().minusMonths(2).atDay(1);AnalyticsPeriod.SIX->YearMonth.now().minusMonths(5).atDay(1);AnalyticsPeriod.YEAR->today.withDayOfYear(1);AnalyticsPeriod.ALL->null;AnalyticsPeriod.CUSTOM->customStart};val end=if(period==AnalyticsPeriod.CUSTOM)customEnd else today;val tx=allTx.map{it.transaction}.filter{(start==null||it.transactionDate>=start.toEpochDay())&&it.transactionDate<=end.toEpochDay()};val given=tx.filter{it.type==TransactionType.GIVEN}.sumOf{it.amountPaise};val received=tx.filter{it.type==TransactionType.RECEIVED}.sumOf{it.amountPaise};val monthly=tx.groupBy{YearMonth.from(LocalDate.ofEpochDay(it.transactionDate))}.toSortedMap().map{(m,v)->MonthPair(m,v.filter{it.type==TransactionType.GIVEN}.sumOf{it.amountPaise},v.filter{it.type==TransactionType.RECEIVED}.sumOf{it.amountPaise})}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{Text("Analytics",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}
        item{FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){AnalyticsPeriod.entries.forEach{p->FilterChip(period==p,{period=p},{Text(p.label)})}}}
        if(period==AnalyticsPeriod.CUSTOM)item{DateRange(customStart,customEnd,{customStart=it},{customEnd=it})}
        item{ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp)){Text("TOTAL OUTSTANDING");Text(Money.format(people.sumOf{it.balancePaise.coerceAtLeast(0)}),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold)}}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){StatCard("Given",given,Modifier.weight(1f));StatCard("Received",received,Modifier.weight(1f))}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){CountCard("Active",people.count{it.balancePaise!=0L},Modifier.weight(1f));CountCard("Settled",people.count{it.balancePaise==0L},Modifier.weight(1f))}}
        item{ChartCard("Outstanding by person"){OutstandingBars(people.filter{it.balancePaise>0}.sortedByDescending{it.balancePaise}.take(6))}}
        item{ChartCard("Lending over time"){MonthlyBars(monthly.takeLast(6))}}
        item{ChartCard("Payment methods"){MethodBars(tx.groupBy{it.paymentMethod}.mapValues{it.value.sumOf{v->v.amountPaise}})}}
        item{ChartCard("UPI breakdown"){MethodBars(tx.filter{it.paymentMethod==PaymentMethod.UPI}.groupBy{it.upiProvider?.name?:"Other"}.mapValues{it.value.sumOf{v->v.amountPaise}})}}
    }
}
@Composable private fun DateRange(start:LocalDate,end:LocalDate,onStart:(LocalDate)->Unit,onEnd:(LocalDate)->Unit){val c=LocalContext.current;Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({android.app.DatePickerDialog(c,{_,y,m,d->onStart(LocalDate.of(y,m+1,d))},start.year,start.monthValue-1,start.dayOfMonth).show()},Modifier.weight(1f)){Text("From $start")};OutlinedButton({android.app.DatePickerDialog(c,{_,y,m,d->onEnd(LocalDate.of(y,m+1,d))},end.year,end.monthValue-1,end.dayOfMonth).show()},Modifier.weight(1f)){Text("To $end")}}}
@Composable private fun StatCard(label:String,value:Long,modifier:Modifier){ElevatedCard(modifier){Column(Modifier.padding(16.dp)){Text(label);Text(Money.format(value),fontWeight=FontWeight.Bold)}}}
@Composable private fun CountCard(label:String,value:Int,modifier:Modifier){ElevatedCard(modifier){Column(Modifier.padding(16.dp)){Text(label);Text(value.toString(),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}}}
@Composable private fun ChartCard(title:String,chart: @Composable () -> Unit){ElevatedCard(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);chart()}}}
@Composable private fun OutstandingBars(items:List<PersonSummary>){if(items.isEmpty())Text("No outstanding balances") else {val max=items.maxOf{it.balancePaise}.toFloat();items.forEach{p->Column{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(p.person.name);Text(Money.format(p.balancePaise))};LinearProgressIndicator({p.balancePaise/max},Modifier.fillMaxWidth())}}}}
@Composable private fun MonthlyBars(items:List<MonthPair>){if(items.isEmpty())Text("No transactions in this period")else {val given=MaterialTheme.colorScheme.error;val received=MaterialTheme.colorScheme.primary;val max=items.maxOf{maxOf(it.given,it.received)}.coerceAtLeast(1).toFloat();Canvas(Modifier.fillMaxWidth().height(150.dp)){val group=size.width/items.size;items.forEachIndexed{i,m->val x=i*group;drawRect(given,Offset(x,size.height-size.height*m.given/max),androidx.compose.ui.geometry.Size(group*.36f,size.height*m.given/max));drawRect(received,Offset(x+group*.4f,size.height-size.height*m.received/max),androidx.compose.ui.geometry.Size(group*.36f,size.height*m.received/max))}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround){items.forEach{Text(it.month.month.name.take(3).lowercase().replaceFirstChar(Char::uppercase),style=MaterialTheme.typography.labelSmall)}};Text("Given (red) • Received (green)",color=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun <T> MethodBars(values:Map<T,Long>){if(values.isEmpty())Text("No data")else{val max=values.values.max().coerceAtLeast(1).toFloat();values.entries.sortedByDescending{it.value}.forEach{(k,v)->Column{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(k.toString().replace('_',' ').lowercase().replaceFirstChar(Char::uppercase));Text(Money.format(v))};LinearProgressIndicator({v/max},Modifier.fillMaxWidth())}}}}
