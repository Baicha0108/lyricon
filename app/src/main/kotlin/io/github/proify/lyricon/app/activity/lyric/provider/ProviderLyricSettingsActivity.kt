package io.github.proify.lyricon.app.activity.lyric.provider

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.proify.lyricon.app.R
import io.github.proify.lyricon.app.activity.BaseActivity
import io.github.proify.lyricon.app.bridge.AppBridgeConstants
import io.github.proify.lyricon.app.bridge.LyriconBridge
import io.github.proify.lyricon.app.bridge.ProviderLyricPrefs
import io.github.proify.lyricon.app.compose.AppToolBarListContainer
import io.github.proify.lyricon.app.compose.preference.IntInputPreference
import io.github.proify.lyricon.app.compose.preference.rememberBooleanPreference
import io.github.proify.lyricon.app.compose.preference.rememberStringPreference
import io.github.proify.lyricon.app.util.LyricPrefs
import io.github.proify.lyricon.common.PackageNames
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

class ProviderLyricSettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = intent.getStringExtra(EXTRA_PROVIDER)?.takeIf { it.isNotBlank() } ?: run { finish(); return }
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { provider }
        setContent { ProviderLyricsSettings(provider, label) }
    }
    companion object {
        const val EXTRA_PROVIDER = "provider_package"
        const val EXTRA_LABEL = "provider_label"
    }
}

@Composable fun ProviderLyricsSettingsAction(provider: String, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    IconButton(modifier = modifier, minWidth = 48.dp, minHeight = 48.dp, onClick = {
        context.startActivity(Intent(context, ProviderLyricSettingsActivity::class.java)
            .putExtra(ProviderLyricSettingsActivity.EXTRA_PROVIDER, provider)
            .putExtra(ProviderLyricSettingsActivity.EXTRA_LABEL, label))
    }) {
        Icon(MiuixIcons.Settings, contentDescription = stringResource(R.string.provider_lyrics_settings),
            tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
    }
}

@Composable private fun ProviderLyricsSettings(provider: String, label: String) {
    val context = LocalContext.current
    val prefs = remember { LyricPrefs.getSharedPreferences(ProviderLyricPrefs.PREF_NAME) }
    fun key(setting: String) = ProviderLyricPrefs.key(provider, setting)
    var disableAll by rememberBooleanPreference(prefs, key(ProviderLyricPrefs.DISABLE_ALL), false)
    var disableMain by rememberBooleanPreference(prefs, key(ProviderLyricPrefs.DISABLE_MAIN), false)
    var disableSecondary by rememberBooleanPreference(prefs, key(ProviderLyricPrefs.DISABLE_SECONDARY), false)
    var delayText by rememberStringPreference(prefs, key(ProviderLyricPrefs.DELAY_MS), "0")
    fun notifyChange() {
        LyriconBridge.with(context).to(PackageNames.SYSTEM_UI).key(AppBridgeConstants.REQUEST_UPDATE_PROVIDER_LYRICS).send()
    }
    fun resetProvider() {
        prefs.edit().apply {
            listOf(ProviderLyricPrefs.DISABLE_ALL, ProviderLyricPrefs.DISABLE_MAIN,
                ProviderLyricPrefs.DISABLE_SECONDARY, ProviderLyricPrefs.DELAY_MS).forEach { remove(key(it)) }
        }.commit()
        notifyChange()
    }
    AppToolBarListContainer(title = stringResource(R.string.provider_lyrics_settings), canBack = true,
        actions = {
            IconButton(modifier = Modifier.padding(end = 12.dp), minWidth = 48.dp, minHeight = 48.dp,
                onClick = { resetProvider() }) {
                Icon(MiuixIcons.Reset, contentDescription = stringResource(R.string.provider_lyrics_reset),
                    tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
        }) {
        item("provider") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp)) {
                Text(label, style = MiuixTheme.textStyles.title3)
                Text(provider, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text(stringResource(R.string.provider_lyrics_scope), Modifier.padding(top = 8.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        item("controls") {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SwitchPreference(title = stringResource(R.string.provider_lyrics_disable_all),
                    summary = stringResource(R.string.provider_lyrics_disable_all_summary), checked = disableAll,
                    onCheckedChange = { disableAll = it; notifyChange() })
                SwitchPreference(title = stringResource(R.string.provider_lyrics_disable_main),
                    summary = stringResource(R.string.provider_lyrics_disable_main_summary), checked = disableMain,
                    enabled = !disableAll, onCheckedChange = { disableMain = it; notifyChange() })
                SwitchPreference(title = stringResource(R.string.provider_lyrics_disable_secondary),
                    summary = stringResource(R.string.provider_lyrics_disable_secondary_summary), checked = disableSecondary,
                    enabled = !disableAll, onCheckedChange = { disableSecondary = it; notifyChange() })
                IntInputPreference(preferences = prefs, key = key(ProviderLyricPrefs.DELAY_MS),
                    title = stringResource(R.string.provider_lyrics_delay), defaultValue = 0,
                    range = -ProviderLyricPrefs.MAX_DELAY_MS..ProviderLyricPrefs.MAX_DELAY_MS,
                    enabled = !disableAll, label = stringResource(R.string.provider_lyrics_ms),
                    dialogSummary = stringResource(R.string.provider_lyrics_delay_summary),
                    summary = { stringResource(R.string.provider_lyrics_delay_value, it ?: 0) },
                    value = delayText?.toIntOrNull() ?: 0,
                    onValueChange = { delayText = (it ?: 0).toString(); notifyChange() })
            }
        }
    }
}
