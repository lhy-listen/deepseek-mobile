package com.lhy.deepseekmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lhy.deepseekmobile.ui.AppViewModel
import com.lhy.deepseekmobile.ui.DsAppRoot

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DsAppRoot(vm)
        }
    }

    override fun onPause() {
        super.onPause()
        vm.onAppPaused()
    }
}
