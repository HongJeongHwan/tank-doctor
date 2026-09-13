package io.github.hongjeonghwan.tankdoctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hongjeonghwan.tankdoctor.ui.HomeScreen
import io.github.hongjeonghwan.tankdoctor.ui.ResultScreen
import io.github.hongjeonghwan.tankdoctor.ui.SettingsScreen
import io.github.hongjeonghwan.tankdoctor.ui.theme.TankDoctorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TankDoctorTheme {
                TankDoctorApp()
            }
        }
    }
}

@Composable
fun TankDoctorApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.screen != Screen.HOME) { vm.open(Screen.HOME) }

    when (state.screen) {
        Screen.HOME -> HomeScreen(state, vm)
        Screen.RESULT -> state.result?.let { ResultScreen(it, state.photos, vm) } ?: HomeScreen(state, vm)
        Screen.SETTINGS -> SettingsScreen(state, vm)
    }
}
