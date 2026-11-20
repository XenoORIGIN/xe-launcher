package com.xenoorigin.xelauncher.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType

fun shouldOfferDefaultHome(isDefault: Boolean, firstRunDone: Boolean): Boolean = !isDefault && !firstRunDone

fun isDefaultHome(context: Context): Boolean =
    runCatching { context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME) }.getOrDefault(false)

fun requestDefaultHomeIntent(context: Context): Intent =
    context.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME)

@Composable
fun SetDefaultHomeCard(onRequest: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = LocalXeScheme.current
    GlassSurface(RoundedCornerShape(28.dp), modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.set_default_title), style = XeType.Title, color = scheme.text)
            Text(stringResource(R.string.set_default_body), style = XeType.Body, color = scheme.textSecondary)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPillButton(stringResource(R.string.not_now), onLater, Modifier.weight(1f))
                GlassPillButton(stringResource(R.string.set_default_action), onRequest, Modifier.weight(1.4f), icon = R.drawable.ti_home)
            }
        }
    }
}
