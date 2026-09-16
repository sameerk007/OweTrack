package com.owetrack.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owetrack.app.*
import com.owetrack.app.domain.Money
import com.owetrack.app.ui.components.*

@Composable fun HomeScreen(vm:OweTrackViewModel,onAddPerson:()->Unit,onPerson:(Long)->Unit,onQuickTransaction:()->Unit,onSearch:()->Unit) {
    val people by vm.people.collectAsState()
    val allPeople by vm.allPeople.collectAsState()
    val txs by vm.allTransactions.collectAsState()
    val options by vm.homeOptions.collectAsState()
    val outstanding=allPeople.sumOf { it.balancePaise.coerceAtLeast(0) }
    var sortOpen by remember{mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=96.dp)) {
        item {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("OweTrack",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);IconButton(onSearch){Icon(Icons.Default.ManageSearch,"Search transactions")}}
                ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text("TOTAL OUTSTANDING",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant); Text(Money.format(outstanding),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold); Text("${allPeople.count{it.balancePaise>0}} people currently owe you") } }
                Button(onClick=onQuickTransaction,modifier=Modifier.fillMaxWidth().height(52.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Add transaction")}
                OutlinedTextField(options.query,vm::setQuery,Modifier.fillMaxWidth(),placeholder={Text("Search people")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,trailingIcon={if(options.query.isNotBlank())IconButton({vm.setQuery("")}){Icon(Icons.Default.Clear,null)}})
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){ PeopleFilter.entries.forEach { f->FilterChip(options.filter==f,{vm.setFilter(f)},{Text(f.name.lowercase().replaceFirstChar(Char::uppercase))}) } }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("People",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Box{TextButton({sortOpen=true}){Icon(Icons.Default.Sort,null);Text(options.sort.name.lowercase().replace('_',' ').replaceFirstChar(Char::uppercase))};DropdownMenu(sortOpen,{sortOpen=false}){PeopleSort.entries.forEach{ s->DropdownMenuItem({Text(s.name.lowercase().replace('_',' ').replaceFirstChar(Char::uppercase))},{vm.setSort(s);sortOpen=false})}}}}
            }
        }
        if(people.isEmpty()) item { EmptyState(Icons.Default.People,"No people here",if(allPeople.isEmpty()) "You haven't added anyone yet." else "Try a different search or filter.",if(allPeople.isEmpty()) "Add person" else null,onAddPerson) }
        items(people,key={it.person.personId}) { item ->
            ListItem(headlineContent={Text(item.person.name,fontWeight=FontWeight.SemiBold)},supportingContent={Text("${item.transactionCount} transactions")},trailingContent={BalanceText(item.balancePaise)},leadingContent={Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.primaryContainer){Text(item.person.name.take(1).uppercase(),Modifier.padding(14.dp),fontWeight=FontWeight.Bold)}},modifier=Modifier.clickable{onPerson(item.person.personId)})
            HorizontalDivider(Modifier.padding(horizontal=20.dp))
        }
        if(txs.isNotEmpty()) { item { Text("Recent activity",Modifier.padding(20.dp),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }; items(txs.take(6),key={it.transaction.transactionId}){row->TransactionRow(row.transaction,personName=row.personName,onClick={onPerson(row.transaction.personId)})} }
        item { TextButton(onClick=onAddPerson,modifier=Modifier.fillMaxWidth().padding(16.dp)){Icon(Icons.Default.PersonAdd,null);Spacer(Modifier.width(8.dp));Text("Add person")} }
    }
}
