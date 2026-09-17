package com.owetrack.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.owetrack.app.OweTrackViewModel
import com.owetrack.app.data.TransactionType
import com.owetrack.app.ui.components.BalanceText
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun QuickTransactionScreen(vm:OweTrackViewModel,onBack:()->Unit,onChoose:(Long,TransactionType)->Unit){
    val people by vm.allPeople.collectAsState();var query by rememberSaveable{mutableStateOf("")};var personId by remember{mutableStateOf<Long?>(null)};val filtered=people.filter{it.person.name.contains(query,true)}
    Scaffold(topBar={TopAppBar({Text("Add transaction")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,null)}})}){pad->Column(Modifier.padding(pad)){OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(16.dp),placeholder={Text("Select person")},leadingIcon={Icon(Icons.Default.Search,null)});LazyColumn{items(filtered,key={it.person.personId}){p->ListItem({Text(p.person.name)},trailingContent={BalanceText(p.balancePaise)},modifier=Modifier.clickable{personId=p.person.personId});HorizontalDivider()}}}}
    personId?.let{id->AlertDialog({personId=null},title={Text("What happened?")},text={Text("Record money for ${people.firstOrNull{it.person.personId==id}?.person?.name.orEmpty()}.")},confirmButton={Button({onChoose(id,TransactionType.GIVEN);personId=null}){Text("Give money")}},dismissButton={OutlinedButton({onChoose(id,TransactionType.RECEIVED);personId=null}){Text("Receive")}})}
}
