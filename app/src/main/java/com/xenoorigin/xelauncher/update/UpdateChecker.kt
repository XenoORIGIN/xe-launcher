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

data class HttpResult(val code: Int, val body: String)

sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data class Available(val release: ReleaseInfo) : UpdateResult
    /** No network, GitHub unreachable or rate-limited: try again later. */
    data object Failed : UpdateResult
}

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

    /** Compares the newest GitHub release with [current]. A repository with no release yet counts as up to date. */
    suspend fun check(current: String, fetch: suspend (String) -> HttpResult? = ::httpGet): UpdateResult {
        val res = fetch(LATEST_API) ?: return UpdateResult.Failed
        if (res.code == 404) return UpdateResult.UpToDate
        if (res.code != 200) return UpdateResult.Failed
        val r = parse(res.body) ?: return UpdateResult.Failed
        return if (isNewer(r.tag, current)) UpdateResult.Available(r) else UpdateResult.UpToDate
    }

    private fun parse(body: String): ReleaseInfo? = runCatching {
        val o = Json.parseToJsonElement(body).jsonObject
        val tag = o["tag_name"]!!.jsonPrimitive.content
        val apk = o["assets"]?.jsonArray?.map { it.jsonObject }
            ?.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull == "xe-launcher.apk" }
            ?.get("browser_download_url")?.jsonPrimitive?.content ?: STABLE_APK
        ReleaseInfo(tag, apk, o["body"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }.getOrNull()

    private suspend fun httpGet(url: String): HttpResult? = withContext(Dispatchers.IO) {
        runCatching {
            (URL(url).openConnection() as HttpURLConnection).run {
                connectTimeout = 10000; readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "XeLauncher")
                val code = responseCode
                val stream = if (code in 200..299) inputStream else errorStream
                HttpResult(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
            }
        }.getOrNull()
    }
}
