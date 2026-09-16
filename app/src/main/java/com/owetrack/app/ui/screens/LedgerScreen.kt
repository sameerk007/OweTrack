package com.owetrack.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owetrack.app.OweTrackViewModel
import com.owetrack.app.data.*
import com.owetrack.app.domain.Money
import com.owetrack.app.export.Exporter
import com.owetrack.app.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LedgerScreen(vm:OweTrackViewModel,personId:Long,onBack:()->Unit,onAdd:(TransactionType)->Unit,onEdit:(Long,TransactionType)->Unit,context:Context){
    val person by vm.repository.person(personId).collectAsState(initial=null);val txs by vm.repository.transactions(personId).collectAsState(initial=emptyList());val prefs by vm.preferences.collectAsState();val running=remember(txs){Money.runningBalances(txs)};var selected by remember{mutableStateOf<TransactionEntity?>(null)};var deleteTarget by remember{mutableStateOf<TransactionEntity?>(null)};var menu by remember{mutableStateOf(false)};val scope=rememberCoroutineScope()
    Scaffold(topBar={TopAppBar({Text(person?.person?.name ?: "Ledger")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}},actions={IconButton({menu=true}){Icon(Icons.Default.MoreVert,null)};DropdownMenu(menu,{menu=false}){DropdownMenuItem({Text("Share balance")},{menu=false;person?.let{p->val message=if(p.balancePaise>=0)"${p.person.name} currently has ${Money.format(p.balancePaise)} outstanding." else "${p.person.name} has a credit of ${Money.format(-p.balancePaise)}.";context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,message)},"Share balance"))}});DropdownMenuItem({Text("Share PDF statement")},{menu=false;scope.launch{person?.let{Exporter.sharePdf(context,it,txs)}}});DropdownMenuItem({Text("Export CSV")},{menu=false;scope.launch{person?.let{Exporter.shareCsv(context,it,txs)}}})}})}){pad->
        LazyColumn(Modifier.padding(pad).fillMaxSize(),contentPadding=PaddingValues(bottom=40.dp)){
            person?.let{p->item{Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text("OUTSTANDING",style=MaterialTheme.typography.labelMedium);BalanceText(p.balancePaise,large=true);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("Given",color=MaterialTheme.colorScheme.onSurfaceVariant);Text(Money.format(p.totalGivenPaise),fontWeight=FontWeight.Bold)};Column{Text("Received",color=MaterialTheme.colorScheme.onSurfaceVariant);Text(Money.format(p.totalReceivedPaise),fontWeight=FontWeight.Bold)}};Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Button({onAdd(TransactionType.GIVEN)},Modifier.weight(1f)){Icon(Icons.Default.NorthEast,null);Text(" Give money")};FilledTonalButton({onAdd(TransactionType.RECEIVED)},Modifier.weight(1f)){Icon(Icons.Default.SouthWest,null);Text(" Receive")}};if(p.transactionCount>0)Text("${p.transactionCount} transactions • ${p.firstActivity?.asDate("d MMM yyyy")} to ${p.lastActivity?.asDate("d MMM yyyy")}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text("Transactions",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}}
            if(txs.isEmpty())item{EmptyState(Icons.Default.ReceiptLong,"No transactions yet","Record money given or received.")}
            items(txs,key={it.transactionId}){tx->TransactionRow(tx,if(prefs.showRunningBalance)running[tx.transactionId]else null,onClick={selected=tx});HorizontalDivider(Modifier.padding(horizontal=20.dp))}
        }
    }
    selected?.let{tx->ModalBottomSheet({selected=null}){ListItem({Text("Edit transaction")},leadingContent={Icon(Icons.Default.Edit,null)},modifier=Modifier.clickable{onEdit(tx.transactionId,tx.type);selected=null});ListItem({Text("Delete transaction",color=MaterialTheme.colorScheme.error)},leadingContent={Icon(Icons.Default.Delete,null,tint=MaterialTheme.colorScheme.error)},modifier=Modifier.clickable{deleteTarget=tx;selected=null});Spacer(Modifier.height(24.dp))}}
    deleteTarget?.let{tx->AlertDialog({deleteTarget=null},title={Text("Delete this transaction?")},text={Text("This will recalculate the person's balance. This action cannot be undone.")},confirmButton={TextButton({vm.deleteTransaction(tx);deleteTarget=null}){Text("Delete",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton({deleteTarget=null}){Text("Cancel")}})}
}
