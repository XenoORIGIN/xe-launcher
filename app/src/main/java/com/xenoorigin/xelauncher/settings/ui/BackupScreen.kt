package com.xenoorigin.xelauncher.settings.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.backup.BackupManager
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.di.AppGraph
import com.xenoorigin.xelauncher.wallpaper.WallpaperTarget
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(graph: AppGraph, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember {
        BackupManager(graph.db, graph.settings, { graph.wallpaper.currentFile() }, { f ->
            BitmapFactory.decodeFile(f.path)?.let { graph.wallpaper.applyBitmap(it, WallpaperTarget.HOME) }
        })
    }
    val saved = stringResource(R.string.backup_done); val restored = stringResource(R.string.restore_done); val failed = stringResource(R.string.restore_failed)
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch {
            runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { manager.export(it) } }
            Toast.makeText(ctx, saved, Toast.LENGTH_SHORT).show()
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val r = runCatching { ctx.contentResolver.openInputStream(uri)!!.use { manager.import(it) } }.getOrElse { Result.failure(it) }
            Toast.makeText(ctx, if (r.isSuccess) restored else failed, Toast.LENGTH_SHORT).show()
        }
    }
    SettingsPage(stringResource(R.string.backup), back) {
        Text(stringResource(R.string.backup_desc), style = XeType.Caption, color = LocalXeScheme.current.textSecondary)
        SettingsGroup {
            NavRow(stringResource(R.string.backup_export), R.drawable.ti_upload, RowColors.Cyan) {
                create.launch("xe-launcher-" + SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date()) + ".xebackup")
            }
            NavRow(stringResource(R.string.backup_import), R.drawable.ti_download, RowColors.Green) { open.launch(arrayOf("*/*")) }
        }
    }
}
