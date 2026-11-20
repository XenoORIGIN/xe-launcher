package com.xenoorigin.xelauncher.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class SettingsStore(context: Context, scope: CoroutineScope, fileName: String = "xe_settings") {
    private val key = stringPreferencesKey("settings_json")
    private val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
        context.preferencesDataStoreFile(fileName)
    }

    val settings: Flow<XeSettings> = store.data.map { decode(it[key]) }.distinctUntilChanged()

    suspend fun current(): XeSettings = settings.first()

    suspend fun update(f: (XeSettings) -> XeSettings) {
        store.edit { p -> p[key] = json.encodeToString(XeSettings.serializer(), f(decode(p[key]))) }
    }

    suspend fun exportJson(): String = json.encodeToString(XeSettings.serializer(), current())

    suspend fun importJson(text: String) {
        val parsed = decode(text)
        store.edit { it[key] = json.encodeToString(XeSettings.serializer(), parsed) }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }
        fun decode(text: String?): XeSettings =
            if (text.isNullOrBlank()) XeSettings() else runCatching { json.decodeFromString(XeSettings.serializer(), text) }.getOrElse { XeSettings() }
    }
}
