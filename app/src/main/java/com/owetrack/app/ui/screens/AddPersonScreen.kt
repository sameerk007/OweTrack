package com.owetrack.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.owetrack.app.OweTrackViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AddPersonScreen(vm:OweTrackViewModel,onBack:()->Unit,onSaved:(Long)->Unit){
    var name by rememberSaveable{mutableStateOf("")};var phone by rememberSaveable{mutableStateOf("")};var notes by rememberSaveable{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)};val scope=rememberCoroutineScope()
    Scaffold(topBar={TopAppBar({Text("Add person")},navigationIcon={IconButton(onBack){Icon(Icons.Default.ArrowBack,null)}})}){pad->
        Column(Modifier.padding(pad).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            OutlinedTextField(name,{name=it;error=null},Modifier.fillMaxWidth(),label={Text("Name *")},singleLine=true,isError=error!=null,supportingText=error?.let{{Text(it)}})
            OutlinedTextField(phone,{phone=it},Modifier.fillMaxWidth(),label={Text("Phone number (optional)")},singleLine=true)
            OutlinedTextField(notes,{notes=it},Modifier.fillMaxWidth(),label={Text("Notes (optional)")},minLines=3)
            Button(onClick={if(name.isBlank())error="Name cannot be blank" else scope.launch{runCatching{vm.addPerson(name,phone,notes)}.onSuccess(onSaved).onFailure{error=if(it.message?.contains("UNIQUE")==true)"A person with this name already exists" else "Could not save person"}}},Modifier.fillMaxWidth().height(52.dp)){Text("Save person")}
        }
    }
}
