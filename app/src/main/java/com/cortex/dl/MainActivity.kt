package com.cortex.dl

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cortex.dl.ui.AppNavigation
import com.cortex.dl.ui.theme.CortexBlue
import com.cortex.dl.ui.theme.CortexCyan
import com.cortex.dl.ui.theme.CortexDarkBackground
import com.cortex.dl.ui.theme.CortexSurface
import com.cortex.dl.ui.theme.CortexTextPrimary
import com.cortex.dl.ui.theme.CortexTextSecondary
import com.cortex.dl.ui.theme.CortexTheme
import com.cortex.dl.util.ONBOARDING_COMPLETED
import com.cortex.dl.util.PreferenceUtil.getBoolean
import com.cortex.dl.util.PreferenceUtil.updateBoolean
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { CortexTheme { AppEntry() } }
    }
}

@Composable
private fun AppEntry() {
    var onboardingComplete by remember { mutableStateOf(ONBOARDING_COMPLETED.getBoolean()) }
    if (onboardingComplete) AppNavigation()
    else LanguageSetup(onContinue = {
        ONBOARDING_COMPLETED.updateBoolean(true)
        onboardingComplete = true
    })
}

private data class AppLanguage(val tag: String, val nativeName: String, val englishName: String)

private val onboardingLanguages = listOf(
    AppLanguage("ar", "العربية", "Arabic"), AppLanguage("en", "English", "English"),
    AppLanguage("es", "Español", "Spanish"), AppLanguage("fr", "Français", "French"),
    AppLanguage("de", "Deutsch", "German"), AppLanguage("tr", "Türkçe", "Turkish"),
    AppLanguage("ru", "Русский", "Russian"), AppLanguage("zh", "中文", "Chinese"),
)

@Composable
private fun LanguageSetup(onContinue: () -> Unit) {
    var selectedTag by remember { mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { Locale.getDefault().language }) }
    Column(
        modifier = Modifier.fillMaxSize().background(CortexDarkBackground).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Choose your app language", color = CortexCyan, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("You can change it later in Settings.", color = CortexTextSecondary, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(onboardingLanguages, key = { it.tag }) { language ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedTag = language.tag
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
                    },
                    colors = CardDefaults.cardColors(containerColor = if (selectedTag.startsWith(language.tag)) CortexBlue else CortexSurface),
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(language.nativeName, color = CortexTextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(language.englishName, color = CortexTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CortexCyan, contentColor = CortexDarkBackground),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue", fontWeight = FontWeight.Bold) }
    }
}
