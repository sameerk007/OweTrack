package com.owetrack.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.owetrack.app.data.TransactionType
import com.owetrack.app.ui.screens.*
import com.owetrack.app.ui.theme.OweTrackTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { val vm:OweTrackViewModel=viewModel();val prefs by vm.preferences.collectAsState();OweTrackTheme(prefs.theme){OweTrackApp(vm)}} }
}

private sealed class Root(val route:String,val label:String){data object Home:Root("home","Home");data object Analytics:Root("analytics","Analytics");data object Settings:Root("settings","Settings")}
@Composable private fun OweTrackApp(vm:OweTrackViewModel){
    val nav=rememberNavController();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route.orEmpty();val roots=listOf(Root.Home,Root.Analytics,Root.Settings);val context=androidx.compose.ui.platform.LocalContext.current
    Scaffold(bottomBar={if(route in roots.map{it.route})NavigationBar{roots.forEach{r->NavigationBarItem(route==r.route,{nav.navigate(r.route){popUpTo(Root.Home.route){saveState=true};launchSingleTop=true;restoreState=true}},icon={Icon(when(r){Root.Home->Icons.Default.Home;Root.Analytics->Icons.Default.BarChart;Root.Settings->Icons.Default.Settings},null)},label={Text(r.label)})}}}){pad->NavHost(nav,Root.Home.route,Modifier.padding(pad)){
        composable(Root.Home.route){HomeScreen(vm,{nav.navigate("person/add")},{nav.navigate("ledger/$it")},{nav.navigate("quick")},{nav.navigate("search")})}
        composable(Root.Analytics.route){AnalyticsScreen(vm)}
        composable(Root.Settings.route){SettingsScreen(vm,context)}
        composable("person/add"){AddPersonScreen(vm,{nav.popBackStack()}){id->nav.navigate("ledger/$id"){popUpTo("person/add"){inclusive=true}}}}
        composable("quick"){QuickTransactionScreen(vm,{nav.popBackStack()}){id,type->nav.navigate("transaction/$id/${type.name}/0")}}
        composable("search"){SearchScreen(vm,{nav.popBackStack()}){nav.navigate("ledger/$it")}}
        composable("ledger/{personId}",arguments=listOf(navArgument("personId"){type=NavType.LongType})){back->val id=back.arguments!!.getLong("personId");LedgerScreen(vm,id,{nav.popBackStack()},{type->nav.navigate("transaction/$id/${type.name}/0")},{tx,type->nav.navigate("transaction/$id/${type.name}/$tx")},context)}
        composable("transaction/{personId}/{type}/{transactionId}",arguments=listOf(navArgument("personId"){type=NavType.LongType},navArgument("type"){type=NavType.StringType},navArgument("transactionId"){type=NavType.LongType})){back->val id=back.arguments!!.getLong("personId");val type=TransactionType.valueOf(back.arguments!!.getString("type")!!);val tx=back.arguments!!.getLong("transactionId");TransactionScreen(vm,id,type,tx,{nav.popBackStack()}){nav.popBackStack()}}
    }}
    AppLockGate(vm)
}

@Composable private fun AppLockGate(vm:OweTrackViewModel){
    val prefs by vm.preferences.collectAsState();val activity=androidx.compose.ui.platform.LocalContext.current as FragmentActivity;val owner=LocalLifecycleOwner.current;var unlocked by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    fun authenticate(){val executor=androidx.core.content.ContextCompat.getMainExecutor(activity);val prompt=BiometricPrompt(activity,executor,object:BiometricPrompt.AuthenticationCallback(){override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){unlocked=true};override fun onAuthenticationError(code:Int,msg:CharSequence){error=msg.toString()}});prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock OweTrack").setSubtitle("Your lending data stays on this device").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL).build())}
    LaunchedEffect(prefs.appLock){if(prefs.appLock&&!unlocked)authenticate()}
    DisposableEffect(owner,prefs.appLock){val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_STOP->if(prefs.appLock)unlocked=false;Lifecycle.Event.ON_RESUME->if(prefs.appLock&&!unlocked)authenticate();else->{}}};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer)}}
    if(prefs.appLock&&!unlocked)AlertDialog(onDismissRequest={},title={Text("OweTrack is locked")},text={Text(error?:"Authenticate to view your ledger.")},confirmButton={Button(::authenticate){Text("Unlock")}},dismissButton={if(error!=null)TextButton({activity.finishAndRemoveTask()}){Text("Close app")}})
}
