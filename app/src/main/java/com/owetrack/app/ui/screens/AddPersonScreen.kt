package com.owetrack.app.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.owetrack.app.OweTrackViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AddPersonScreen(vm:OweTrackViewModel,onBack:()->Unit,onSaved:(Long)->Unit){
    val context=LocalContext.current
    var name by rememberSaveable{mutableStateOf("")};var phone by rememberSaveable{mutableStateOf("")};var notes by rememberSaveable{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)};var contactError by remember{mutableStateOf<String?>(null)};val scope=rememberCoroutineScope()
    val pickContact=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode==Activity.RESULT_OK){
            val uri=result.data?.data
            if(uri==null){contactError="Could not read the selected contact"}
            else runCatching{
                context.contentResolver.query(uri,arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER),null,null,null)?.use{cursor->
                    if(!cursor.moveToFirst())null else {
                        val selectedName=cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)).orEmpty()
                        val selectedPhone=cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)).orEmpty()
                        selectedName to selectedPhone
                    }
                }
            }.onSuccess{selected->
                if(selected==null||selected.first.isBlank())contactError="Could not read the selected contact"
                else{name=selected.first;phone=selected.second;error=null;contactError=null}
            }.onFailure{contactError="Could not read the selected contact"}
        }
    }
    Scaffold(topBar={TopAppBar({Text("Add person")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){pad->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            OutlinedButton(onClick={try{pickContact.launch(Intent(Intent.ACTION_PICK,ContactsContract.CommonDataKinds.Phone.CONTENT_URI))}catch(_:ActivityNotFoundException){contactError="No contacts app is available"}},Modifier.fillMaxWidth()){
                Icon(Icons.Default.Contacts,null);Spacer(Modifier.width(8.dp));Text("Choose from contacts")
            }
            contactError?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
            OutlinedTextField(name,{name=it;error=null},Modifier.fillMaxWidth(),label={Text("Name *")},singleLine=true,isError=error!=null,supportingText=error?.let{{Text(it)}})
            OutlinedTextField(phone,{phone=it},Modifier.fillMaxWidth(),label={Text("Phone number (optional)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone))
            OutlinedTextField(notes,{notes=it},Modifier.fillMaxWidth(),label={Text("Notes (optional)")},minLines=3)
            Button(onClick={if(name.isBlank())error="Name cannot be blank" else scope.launch{runCatching{vm.addPerson(name,phone,notes)}.onSuccess(onSaved).onFailure{error=if(it.message?.contains("UNIQUE")==true)"A person with this name already exists" else "Could not save person"}}},Modifier.fillMaxWidth().height(52.dp)){Text("Save person")}
        }
    }
}
