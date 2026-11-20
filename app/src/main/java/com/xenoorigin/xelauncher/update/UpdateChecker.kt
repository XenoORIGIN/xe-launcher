package com.xenoorigin.xelauncher.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(val tag: String, val apkUrl: String, val notes: String)

object UpdateChecker {
    const val LATEST_API = "https://api.github.com/repos/XenoORIGIN/xe-launcher/releases/latest"
    const val STABLE_APK = "https://github.com/XenoORIGIN/xe-launcher/releases/latest/download/xe-launcher.apk"
    const val WEBSITE = "https://xenoorigin.github.io/xe-launcher/"

    private fun parts(v: String): List<Int>? =
        Regex("\\d+(\\.\\d+)*").find(v)?.value?.split('.')?.map { it.toInt() }

    fun isNewer(remote: String, local: String): Boolean {
        val r = parts(remote) ?: return false; val l = parts(local) ?: return false
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }; val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    suspend fun latest(fetch: suspend (String) -> String? = ::httpGet): ReleaseInfo? = runCatching {
        val o = Json.parseToJsonElement(fetch(LATEST_API) ?: return null).jsonObject
        val tag = o["tag_name"]!!.jsonPrimitive.content
        val apk = o["assets"]?.jsonArray?.map { it.jsonObject }
            ?.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull == "xe-launcher.apk" }
            ?.get("browser_download_url")?.jsonPrimitive?.content ?: STABLE_APK
        ReleaseInfo(tag, apk, o["body"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }.getOrNull()

    private suspend fun httpGet(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            (URL(url).openConnection() as HttpURLConnection).run {
                connectTimeout = 8000; readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                if (responseCode != 200) null else inputStream.bufferedReader().use { it.readText() }
            }
        }.getOrNull()
    }
}
