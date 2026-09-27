package com.aki.lnga

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aki.lnga.data.CredentialStore
import com.aki.lnga.data.BoardPreferenceStore
import com.aki.lnga.data.BrowseHistoryStore
import com.aki.lnga.data.NgaClient
import com.aki.lnga.data.NgaRepository
import com.aki.lnga.data.TopicFavoriteStore
import com.aki.lnga.data.AiClient
import com.aki.lnga.data.AiConfigStore
import com.aki.lnga.data.AiSecretStore
import com.aki.lnga.data.AiConversationStore
import com.aki.lnga.data.ReadingProgressStore
import com.aki.lnga.ui.LngaApp
import com.aki.lnga.ui.MainViewModel
import com.aki.lnga.ui.theme.LngaTheme
import com.aki.lnga.ui.theme.ThemeAppearanceOption
import com.aki.lnga.ui.theme.ThemePreferenceStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = NgaRepository(NgaClient(diagnosticContext = applicationContext))
        val credentialStore = CredentialStore(applicationContext)
        val boardPreferenceStore = BoardPreferenceStore(applicationContext)
        val browseHistoryStore = BrowseHistoryStore(applicationContext)
        val topicFavoriteStore = TopicFavoriteStore(applicationContext)
        val aiConfigStore = AiConfigStore(applicationContext)
        val aiSecretStore = AiSecretStore(applicationContext)
        val aiClient = AiClient()
        val aiConversationStore = AiConversationStore(applicationContext)
        val readingProgressStore = ReadingProgressStore(applicationContext)
        val themePreferenceStore = ThemePreferenceStore(applicationContext)
        val factory = MainViewModel.Factory(
            repository,
            credentialStore,
            boardPreferenceStore,
            browseHistoryStore,
            topicFavoriteStore,
            aiConfigStore,
            aiSecretStore,
            aiClient,
            aiConversationStore,
            readingProgressStore,
        )

        setContent {
            var themeColor by remember { mutableStateOf(themePreferenceStore.load()) }
            var themeAppearance by remember { mutableStateOf(themePreferenceStore.loadAppearance()) }
            var automaticNight by remember { mutableStateOf(themePreferenceStore.loadAutomaticNight()) }
            LngaTheme(themeColor, themeAppearance, automaticNight) {
                val viewModel: MainViewModel = viewModel(factory = factory)
                LngaApp(
                    viewModel = viewModel,
                    themeColor = themeColor,
                    themeAppearance = themeAppearance,
                    automaticNight = automaticNight,
                    onThemeColorChange = { option ->
                        themePreferenceStore.save(option)
                        themeColor = option
                    },
                    onThemeAppearanceChange = { option ->
                        themePreferenceStore.saveAppearance(option)
                        themeAppearance = option
                    },
                    onAutomaticNightChange = { enabled ->
                        themePreferenceStore.saveAutomaticNight(enabled)
                        automaticNight = enabled
                    },
                )
            }
        }
    }
}
