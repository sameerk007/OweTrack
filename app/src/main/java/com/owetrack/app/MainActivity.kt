package com.owetrack.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.owetrack.app.data.TransactionType
import com.owetrack.app.data.AppTheme
import com.owetrack.app.ui.screens.*
import com.owetrack.app.ui.theme.OweTrackTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: OweTrackViewModel = viewModel()
            val preferences by (application as OweTrackApplication).preferences.values.collectAsState(initial = null)
            OweTrackTheme(preferences?.theme ?: AppTheme.SYSTEM) {
                AppLockGate(preferences?.appLock, this) { OweTrackApp(vm) }
            }
        }
    }
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
}

@Composable private fun AppLockGate(appLock:Boolean?, activity:FragmentActivity, content:@Composable ()->Unit){
    val owner=LocalLifecycleOwner.current
    var unlocked by remember{mutableStateOf(false)}
    var promptOpen by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    val prompt=remember(activity){BiometricPrompt(activity,androidx.core.content.ContextCompat.getMainExecutor(activity),object:BiometricPrompt.AuthenticationCallback(){
        override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){promptOpen=false;error=null;unlocked=true}
        override fun onAuthenticationError(code:Int,msg:CharSequence){promptOpen=false;if(code!=BiometricPrompt.ERROR_CANCELED)error=msg.toString()}
    })}
    fun authenticate(){
        if(appLock!=true||unlocked||promptOpen||!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))return
        val authenticators=BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if(BiometricManager.from(activity).canAuthenticate(authenticators)!=BiometricManager.BIOMETRIC_SUCCESS){error="Device authentication is unavailable";return}
        promptOpen=true
        error=null
        try{prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock OweTrack").setSubtitle("Your lending data stays on this device").setAllowedAuthenticators(authenticators).build())}
        catch(e:RuntimeException){promptOpen=false;error="Could not start device authentication"}
    }
    LaunchedEffect(appLock,owner){if(appLock==true)authenticate()}
    DisposableEffect(owner,appLock){
        val observer=LifecycleEventObserver{_,event->when(event){
            Lifecycle.Event.ON_STOP->{unlocked=false;promptOpen=false}
            Lifecycle.Event.ON_RESUME->{if(appLock==true)authenticate()}
            else->{}
        }}
        owner.lifecycle.addObserver(observer)
        onDispose{owner.lifecycle.removeObserver(observer)}
    }
    when{
        appLock==null->Surface(Modifier.fillMaxSize()){Box(contentAlignment=Alignment.Center){CircularProgressIndicator()}}
        appLock==false||unlocked->content()
        else->Surface(Modifier.fillMaxSize()){Box(contentAlignment=Alignment.Center){Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)){
            Text("OweTrack is locked",style=MaterialTheme.typography.headlineSmall)
            Text(error?:"Authenticate to view your ledger.")
            Button(::authenticate,enabled=!promptOpen){Text("Unlock")}
        }}}
    }
}
