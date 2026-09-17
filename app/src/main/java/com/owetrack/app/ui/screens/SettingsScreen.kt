package com.owetrack.app.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.owetrack.app.OweTrackViewModel
import com.owetrack.app.data.*
import com.owetrack.app.export.BackupCodec
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SettingsScreen(vm:OweTrackViewModel,context:Context){
    val prefs by vm.preferences.collectAsState();val scope=rememberCoroutineScope();var status by remember{mutableStateOf<String?>(null)};var pendingRestore by remember{mutableStateOf<Pair<List<PersonEntity>,List<TransactionEntity>>?>(null)}
    val backup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->uri?.let{scope.launch{runCatching{val raw=BackupCodec.encode(vm.repository.allPeople(),vm.repository.allTransactionsSnapshot());requireNotNull(context.contentResolver.openOutputStream(uri)){"Could not open backup file"}.bufferedWriter().use{it.write(raw)}}.onSuccess{status="Backup saved"}.onFailure{status="Backup failed"}}}}
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{scope.launch{runCatching{val raw=context.contentResolver.openInputStream(uri)!!.bufferedReader().use{it.readText()};BackupCodec.decode(raw)}.onSuccess{pendingRestore=it}.onFailure{status="Invalid backup file"}}}}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Settings",style=MaterialTheme.typography.headlineMedium)
        SettingSwitch("App lock","Use device PIN, fingerprint, or face",prefs.appLock,vm::setLock)
        SettingSwitch("Running balance","Show balance after each ledger entry",prefs.showRunningBalance,vm::setRunning)
        HorizontalDivider();Text("Default payment method",style=MaterialTheme.typography.titleMedium);SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){PaymentMethod.entries.forEachIndexed{i,m->SegmentedButton(prefs.defaultMethod==m,{vm.setMethod(m)},SegmentedButtonDefaults.itemShape(i,3)){Text(if(m==PaymentMethod.BANK_TRANSFER)"Bank" else m.name)}}}
        HorizontalDivider();Text("Theme",style=MaterialTheme.typography.titleMedium);SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){AppTheme.entries.forEachIndexed{i,t->SegmentedButton(prefs.theme==t,{vm.setTheme(t)},SegmentedButtonDefaults.itemShape(i,3)){Text(t.name.lowercase().replaceFirstChar(Char::uppercase))}}}
        HorizontalDivider();Button({backup.launch("owetrack-backup.json")},Modifier.fillMaxWidth()){Text("Create full backup")};OutlinedButton({restore.launch(arrayOf("application/json","text/plain"))},Modifier.fillMaxWidth()){Text("Restore from backup")};Text("Restore replaces the current local ledger. Export a backup first.",style=MaterialTheme.typography.bodySmall)
        Text("Currency: Indian Rupee (INR)",style=MaterialTheme.typography.bodyMedium);Text("Date format: d MMM yyyy",style=MaterialTheme.typography.bodyMedium);status?.let{Snackbar{Text(it)}}
    }
    pendingRestore?.let{data->AlertDialog({pendingRestore=null},title={Text("Replace current ledger?")},text={Text("Restoring this backup will replace every current person and transaction.")},confirmButton={TextButton({scope.launch{vm.repository.restore(data.first,data.second);status="Backup restored";pendingRestore=null}}){Text("Restore")}},dismissButton={TextButton({pendingRestore=null}){Text("Cancel")}})}
}
@Composable private fun SettingSwitch(title:String,detail:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(title);Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Switch(checked,onChange)}}
